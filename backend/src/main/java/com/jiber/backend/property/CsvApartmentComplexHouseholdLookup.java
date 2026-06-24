package com.jiber.backend.property;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class CsvApartmentComplexHouseholdLookup implements ApartmentComplexHouseholdLookup {

    private static final Logger log = LoggerFactory.getLogger(CsvApartmentComplexHouseholdLookup.class);
    private static final Integer AMBIGUOUS = Integer.MIN_VALUE;
    private static final int MIN_SCORED_MATCH = 7;
    private static final String APARTMENT_SUFFIX = "\uC544\uD30C\uD2B8";
    private static final List<Path> DEFAULT_PATHS = List.of(
            Path.of("data", "apartment-complex-households-seoul-busan.csv"),
            Path.of("..", "data", "apartment-complex-households-seoul-busan.csv")
    );

    private final List<Record> records;
    private final Map<String, Integer> byRegionLegalDongName = new HashMap<>();
    private final Map<String, Integer> byAddressName = new HashMap<>();

    CsvApartmentComplexHouseholdLookup() {
        this(loadDefaultRecords());
    }

    CsvApartmentComplexHouseholdLookup(List<Record> records) {
        this.records = List.copyOf(records);
        for (var record : records) {
            index(record);
        }
    }

    @Override
    public Optional<Integer> findHouseholdCount(PropertyDetailRow property, List<String> apartmentNameHints) {
        if (property == null || property.getPropertyType() != PropertyType.APARTMENT) {
            return Optional.empty();
        }
        var nameKeys = candidateNameKeys(property, apartmentNameHints);
        for (var nameKey : nameKeys) {
            var byJibunAddress = find(byAddressName, join(normalize(property.getJibunAddress()), nameKey));
            if (byJibunAddress.isPresent()) {
                return byJibunAddress;
            }
            var byRoadAddress = find(byAddressName, join(normalize(property.getRoadAddress()), nameKey));
            if (byRoadAddress.isPresent()) {
                return byRoadAddress;
            }
            var byRegion = find(byRegionLegalDongName, join(
                    normalize(property.getSido()),
                    normalize(property.getSigungu()),
                    normalize(property.getLegalDong()),
                    nameKey
            ));
            if (byRegion.isPresent()) {
                return byRegion;
            }
        }
        return findBestScoredRecord(property, nameKeys);
    }

    private Optional<Integer> findBestScoredRecord(PropertyDetailRow property, Set<String> wantedNameKeys) {
        var bestScore = 0;
        var bestNameSpecificity = 0;
        Integer bestHouseholdCount = null;
        var ambiguous = false;
        for (var record : records) {
            if (record.householdCount() == null || record.householdCount() <= 0) {
                continue;
            }
            if (!sameTextIfBothPresent(record.sido(), property.getSido())) {
                continue;
            }
            var nameMatch = nameMatch(record.complexName(), wantedNameKeys);
            if (nameMatch.score() == 0) {
                continue;
            }
            var score = nameMatch.score();
            if (sameText(record.sigungu(), property.getSigungu())) {
                score += 2;
            }
            if (sameText(record.legalDong(), property.getLegalDong())) {
                score += 2;
            }
            var address = normalize((record.legalAddress() == null ? "" : record.legalAddress()) + " "
                    + (record.roadAddress() == null ? "" : record.roadAddress()));
            if (containsNormalized(address, property.getSigungu())) {
                score += 1;
            }
            if (containsNormalized(address, property.getLegalDong())) {
                score += 1;
            }
            if (score < MIN_SCORED_MATCH) {
                continue;
            }
            if (score > bestScore) {
                bestScore = score;
                bestNameSpecificity = nameMatch.specificity();
                bestHouseholdCount = record.householdCount();
                ambiguous = false;
                continue;
            }
            if (score == bestScore && nameMatch.specificity() > bestNameSpecificity) {
                bestNameSpecificity = nameMatch.specificity();
                bestHouseholdCount = record.householdCount();
                ambiguous = false;
                continue;
            }
            if (score == bestScore
                    && nameMatch.specificity() == bestNameSpecificity
                    && !record.householdCount().equals(bestHouseholdCount)) {
                ambiguous = true;
            }
        }
        if (ambiguous) {
            return Optional.empty();
        }
        return Optional.ofNullable(bestHouseholdCount);
    }

    private void index(Record record) {
        if (record.householdCount() == null || record.householdCount() <= 0) {
            return;
        }
        for (var nameKey : nameKeys(record.complexName())) {
            putUnique(byRegionLegalDongName, join(
                    normalize(record.sido()),
                    normalize(record.sigungu()),
                    normalize(record.legalDong()),
                    nameKey
            ), record.householdCount());
            putAddressKey(record.legalAddress(), nameKey, record.householdCount());
            putAddressKey(record.roadAddress(), nameKey, record.householdCount());
        }
    }

    private void putAddressKey(String address, String nameKey, Integer householdCount) {
        if (!StringUtils.hasText(address)) {
            return;
        }
        putUnique(byAddressName, join(normalize(address), nameKey), householdCount);
    }

    private void putUnique(Map<String, Integer> map, String key, Integer householdCount) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        var previous = map.putIfAbsent(key, householdCount);
        if (previous != null && !previous.equals(householdCount)) {
            map.put(key, AMBIGUOUS);
        }
    }

    private Optional<Integer> find(Map<String, Integer> map, String key) {
        var value = map.get(key);
        if (value == null || value.equals(AMBIGUOUS)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private static List<Record> loadDefaultRecords() {
        var source = DEFAULT_PATHS.stream()
                .filter(Files::isRegularFile)
                .findFirst();
        if (source.isEmpty()) {
            log.warn("Apartment complex household CSV was not found. Tried: {}", DEFAULT_PATHS);
            return List.of();
        }
        try {
            return readRecords(source.get());
        } catch (IOException | RuntimeException exception) {
            log.warn("Apartment complex household CSV could not be read: {}", source.get(), exception);
            return List.of();
        }
    }

    private static List<Record> readRecords(Path source) throws IOException {
        try (var reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            var headerLine = reader.readLine();
            if (headerLine == null) {
                return List.of();
            }
            var headers = parseCsvLine(headerLine);
            var index = new HashMap<String, Integer>();
            for (var i = 0; i < headers.size(); i++) {
                index.put(headers.get(i), i);
            }
            var records = new ArrayList<Record>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                var values = parseCsvLine(line);
                records.add(new Record(
                        value(values, index, "sido"),
                        value(values, index, "sigungu"),
                        value(values, index, "legal_dong"),
                        value(values, index, "complex_name"),
                        value(values, index, "legal_address"),
                        value(values, index, "road_address"),
                        parsePositiveInt(value(values, index, "household_count"))
                ));
            }
            return records;
        }
    }

    private static String value(List<String> values, Map<String, Integer> index, String column) {
        var position = index.get(column);
        if (position == null || position >= values.size()) {
            return null;
        }
        return values.get(position);
    }

    private static Integer parsePositiveInt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            var parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static List<String> parseCsvLine(String line) {
        var values = new ArrayList<String>();
        var value = new StringBuilder();
        var quoted = false;
        for (var i = 0; i < line.length(); i++) {
            var character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                    continue;
                }
                quoted = !quoted;
                continue;
            }
            if (character == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
                continue;
            }
            value.append(character);
        }
        values.add(value.toString());
        return values;
    }

    private static Set<String> nameKeys(String name) {
        var normalized = normalize(name);
        if (!StringUtils.hasText(normalized)) {
            return Set.of();
        }
        var keys = new HashSet<String>();
        keys.add(normalized);
        if (normalized.endsWith(APARTMENT_SUFFIX)) {
            keys.add(normalized.substring(0, normalized.length() - APARTMENT_SUFFIX.length()));
        }
        return keys;
    }

    private static Set<String> candidateNameKeys(PropertyDetailRow property, List<String> apartmentNameHints) {
        var names = new LinkedHashSet<String>();
        names.add(property.getName());
        if (apartmentNameHints != null) {
            names.addAll(apartmentNameHints);
        }
        var keys = new LinkedHashSet<String>();
        for (var name : names) {
            keys.addAll(nameKeys(name));
        }
        return keys;
    }

    private static NameMatch nameMatch(String recordName, Set<String> wantedNameKeys) {
        var recordNameKeys = nameKeys(recordName);
        var bestScore = 0;
        var bestSpecificity = 0;
        for (var wantedNameKey : wantedNameKeys) {
            for (var recordNameKey : recordNameKeys) {
                var score = 0;
                var specificity = 0;
                if (wantedNameKey.equals(recordNameKey)) {
                    score = 5;
                    specificity = recordNameKey.length();
                } else if (containsEither(wantedNameKey, recordNameKey)) {
                    score = 3;
                    specificity = Math.min(wantedNameKey.length(), recordNameKey.length());
                }
                if (score > bestScore || (score == bestScore && specificity > bestSpecificity)) {
                    bestScore = score;
                    bestSpecificity = specificity;
                }
            }
        }
        return new NameMatch(bestScore, bestSpecificity);
    }

    private static boolean containsEither(String left, String right) {
        return StringUtils.hasText(left)
                && StringUtils.hasText(right)
                && left.length() >= 3
                && right.length() >= 3
                && (left.contains(right) || right.contains(left));
    }

    private static boolean sameText(String left, String right) {
        var normalizedLeft = normalize(left);
        var normalizedRight = normalize(right);
        return StringUtils.hasText(normalizedLeft) && normalizedLeft.equals(normalizedRight);
    }

    private static boolean sameTextIfBothPresent(String left, String right) {
        var normalizedLeft = normalize(left);
        var normalizedRight = normalize(right);
        return !StringUtils.hasText(normalizedLeft)
                || !StringUtils.hasText(normalizedRight)
                || normalizedLeft.equals(normalizedRight);
    }

    private static boolean containsNormalized(String text, String value) {
        var normalizedValue = normalize(value);
        return StringUtils.hasText(text)
                && StringUtils.hasText(normalizedValue)
                && text.contains(normalizedValue);
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        var normalized = new StringBuilder();
        var lower = value.toLowerCase(Locale.ROOT);
        for (var offset = 0; offset < lower.length();) {
            var codePoint = lower.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                normalized.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return normalized.toString();
    }

    private static String join(String... values) {
        return String.join("|", values);
    }

    record Record(
            String sido,
            String sigungu,
            String legalDong,
            String complexName,
            String legalAddress,
            String roadAddress,
            Integer householdCount
    ) {
    }

    private record NameMatch(int score, int specificity) {
    }
}
