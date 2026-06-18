package com.jiber.backend.publicdata;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicDataImportService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<PublicDataApiType> API_TYPES = List.of(PublicDataApiType.SALE, PublicDataApiType.RENT);

    private final PublicDataImportProperties properties;
    private final LawdCodeRegistry lawdCodeRegistry;
    private final PublicDataApiClient publicDataApiClient;
    private final PublicDataTransactionMapper transactionMapper;
    private final AddressNormalizer addressNormalizer;
    private final KakaoGeocodingClient kakaoGeocodingClient;
    private final PublicDataImportMapper importMapper;
    private final CanonicalApartmentUpsertService canonicalUpsertService;

    public PublicDataImportService(
            PublicDataImportProperties properties,
            LawdCodeRegistry lawdCodeRegistry,
            PublicDataApiClient publicDataApiClient,
            PublicDataTransactionMapper transactionMapper,
            AddressNormalizer addressNormalizer,
            KakaoGeocodingClient kakaoGeocodingClient,
            PublicDataImportMapper importMapper,
            CanonicalApartmentUpsertService canonicalUpsertService
    ) {
        this.properties = properties;
        this.lawdCodeRegistry = lawdCodeRegistry;
        this.publicDataApiClient = publicDataApiClient;
        this.transactionMapper = transactionMapper;
        this.addressNormalizer = addressNormalizer;
        this.kakaoGeocodingClient = kakaoGeocodingClient;
        this.importMapper = importMapper;
        this.canonicalUpsertService = canonicalUpsertService;
    }

    public PublicDataImportSummary importRecentApartmentTransactions(PublicDataImportCommand command) {
        if (command.dryRun()) {
            return PublicDataImportSummary.empty(true);
        }
        properties.validateLiveImportSecrets();
        var run = PublicDataImportRunRecord.started(properties, command);
        importMapper.insertImportRun(run);
        var summary = PublicDataImportSummary.empty(false);
        try {
            var lawdCodes = lawdCodeRegistry.findByRegions(properties.targetRegions());
            for (var dealMonth : recentMonths(properties.importMonths())) {
                for (var lawdCode : lawdCodes) {
                    for (var apiType : API_TYPES) {
                        summary = importMonth(lawdCode, dealMonth, apiType, command, run.importRunId(), summary);
                        if (summary.reachedLimit(command.limit())) {
                            return finish(run, summary);
                        }
                    }
                }
            }
            return finish(run, summary);
        } catch (RuntimeException exception) {
            importMapper.markImportRunFailed(run.finish("FAILED", summary, exception.getMessage()));
            throw exception;
        }
    }

    private PublicDataImportSummary importMonth(
            LawdCode lawdCode,
            YearMonth dealMonth,
            PublicDataApiType apiType,
            PublicDataImportCommand command,
            Long importRunId,
            PublicDataImportSummary summary
    ) {
        var pageNo = 1;
        while (true) {
            PublicDataApartmentPage page;
            try {
                page = publicDataApiClient.fetchApartmentPage(apiType, lawdCode.lawdCd(), dealMonth, pageNo, properties.pageSize());
            } catch (RuntimeException exception) {
                importMapper.insertImportError(PublicDataImportErrorRecord.of(
                        importRunId,
                        lawdCode.lawdCd(),
                        dealMonth,
                        apiType,
                        exception.getClass().getSimpleName(),
                        SecretRedactor.redact(exception.getMessage(), properties.serviceKey())
                ));
                return summary.addFailure();
            }
            summary = summary.addFetched(page.items().size());
            for (var item : page.items()) {
                var transaction = transactionMapper.toImportedTransaction(item, apiType);
                var address = addressNormalizer.normalize(lawdCode.sido(), lawdCode.sigungu(), item.legalDong(), item.jibun());
                importMapper.upsertGeocodingCache(new GeocodingCacheRecord(
                        address.addressKey(),
                        address.fullAddress(),
                        GeocodingStatus.PENDING,
                        null,
                        null,
                        null
                ));
                importMapper.upsertRawTransaction(PublicDataRawTransactionRecord.from(importRunId, lawdCode, address, transaction));
                summary = summary.addStaged();
                var geocoding = geocode(address);
                importMapper.upsertGeocodingCache(GeocodingCacheRecord.from(address, geocoding));
                importMapper.updateRawGeocodingStatus(transaction.sourceKey(), geocoding.status());
                if (geocoding.status() == GeocodingStatus.SUCCESS) {
                    summary = summary.addGeocoded();
                }
                if (summary.reachedLimit(command.limit())) {
                    return summary;
                }
            }
            if (!page.hasNextPage()) {
                return summary;
            }
            pageNo++;
        }
    }

    private GeocodingResult geocode(NormalizedAddress address) {
        return importMapper.findGeocodingByAddressKey(address.addressKey())
                .filter(record -> record.status() == GeocodingStatus.SUCCESS)
                .map(record -> new GeocodingResult(
                        address.fullAddress(),
                        record.status(),
                        record.latitude(),
                        record.longitude(),
                        record.failureReason()
                ))
                .orElseGet(() -> kakaoGeocodingClient.geocode(address));
    }

    private PublicDataImportSummary finish(PublicDataImportRunRecord run, PublicDataImportSummary summary) {
        canonicalUpsertService.upsertEligibleRawRows(run.importRunId());
        importMapper.markImportRunSucceeded(run.finish("SUCCEEDED", summary, null));
        return summary;
    }

    private List<YearMonth> recentMonths(int count) {
        var currentMonth = YearMonth.now(SEOUL_ZONE);
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(currentMonth::minusMonths)
                .toList();
    }
}
