package com.jiber.backend.publicdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class LawdCodeRegistry {

    private static final String RESOURCE_PATH = "publicdata/lawd-codes-seoul-busan.csv";

    private final List<LawdCode> codes;

    public LawdCodeRegistry() {
        this.codes = loadCodes();
    }

    public List<LawdCode> findByRegions(List<PublicDataTargetRegion> regions) {
        return codes.stream()
                .filter(code -> regions.contains(code.region()))
                .toList();
    }

    public Optional<LawdCode> findByLawdCd(String lawdCd) {
        return codes.stream()
                .filter(code -> code.lawdCd().equals(lawdCd))
                .findFirst();
    }

    private List<LawdCode> loadCodes() {
        var resource = new ClassPathResource(RESOURCE_PATH);
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(this::parseLine)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("LAWD code resource could not be loaded: " + RESOURCE_PATH, exception);
        }
    }

    private LawdCode parseLine(String line) {
        var parts = line.split(",", -1);
        return new LawdCode(
                PublicDataTargetRegion.valueOf(parts[0]),
                parts[1],
                parts[2],
                parts[3]
        );
    }
}
