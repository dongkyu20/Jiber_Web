package com.jiber.backend.publicdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.property.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CanonicalApartmentUpsertServiceTest {

    @Test
    void geocodingSuccessRawRowCreatesPropertyAndTransaction() {
        var mapper = new FakeCanonicalMapper(List.of(raw(1L, "source-sale-1", TransactionType.SALE)));
        var service = new CanonicalApartmentUpsertService(mapper);

        var summary = service.upsertEligibleRawRows(10L);

        assertThat(summary.processedCount()).isEqualTo(1);
        assertThat(summary.propertyCreatedCount()).isEqualTo(1);
        assertThat(summary.transactionCreatedCount()).isEqualTo(1);
        assertThat(summary.skippedCount()).isZero();
        assertThat(mapper.insertedProperties).singleElement().satisfies(property -> {
            assertThat(property.getPropertyType()).isEqualTo("APARTMENT");
            assertThat(property.getName()).isEqualTo("예시아파트");
            assertThat(property.getSido()).isEqualTo("서울특별시");
            assertThat(property.getSigungu()).isEqualTo("강남구");
            assertThat(property.getLegalDong()).isEqualTo("역삼동");
            assertThat(property.getJibunAddress()).isEqualTo("서울특별시 강남구 역삼동 12-3");
            assertThat(property.getLatitude()).isEqualByComparingTo("37.5001000");
            assertThat(property.getLongitude()).isEqualByComparingTo("127.0364000");
            assertThat(property.getSourceSystem()).isEqualTo("PUBLIC_DATA_PORTAL");
        });
        assertThat(mapper.insertedTransactions).singleElement().satisfies(transaction -> {
            assertThat(transaction.propertyId()).isEqualTo(2001L);
            assertThat(transaction.sourceTransactionId()).isEqualTo("source-sale-1");
            assertThat(transaction.transactionType()).isEqualTo(TransactionType.SALE);
            assertThat(transaction.dealAmountKrw()).isEqualTo(1_250_000_000L);
            assertThat(transaction.depositAmountKrw()).isNull();
            assertThat(transaction.monthlyRentKrw()).isEqualTo(0L);
        });
        assertThat(mapper.appliedRawIds).containsExactly(1L);
    }

    @Test
    void sameSourceKeyIsIdempotentAndDoesNotInsertDuplicateTransaction() {
        var mapper = new FakeCanonicalMapper(List.of(raw(1L, "source-sale-1", TransactionType.SALE)));
        mapper.existingTransactionSourceKeys.add("source-sale-1");
        var service = new CanonicalApartmentUpsertService(mapper);

        var summary = service.upsertEligibleRawRows(10L);

        assertThat(summary.propertyCreatedCount()).isEqualTo(1);
        assertThat(summary.transactionCreatedCount()).isZero();
        assertThat(summary.transactionSkippedCount()).isEqualTo(1);
        assertThat(mapper.insertedTransactions).isEmpty();
        assertThat(mapper.appliedRawIds).containsExactly(1L);
    }

    @Test
    void longRawSourceKeyIsStoredAsCanonicalSourceTransactionIdWithoutTruncation() {
        String longSourceKey = "PUBLIC-DATA-" + "서울특별시-강남구-역삼동-예시아파트-".repeat(9);
        assertThat(longSourceKey.length()).isGreaterThan(150).isLessThanOrEqualTo(500);
        var mapper = new FakeCanonicalMapper(List.of(raw(1L, longSourceKey, TransactionType.SALE)));
        var service = new CanonicalApartmentUpsertService(mapper);

        service.upsertEligibleRawRows(10L);

        assertThat(mapper.insertedTransactions)
                .singleElement()
                .extracting(CanonicalApartmentTransactionCommand::sourceTransactionId)
                .isEqualTo(longSourceKey);
    }

    @Test
    void sameApartmentAddressUsesOnePropertyForMultipleTransactions() {
        var mapper = new FakeCanonicalMapper(List.of(
                raw(1L, "source-sale-1", TransactionType.SALE),
                raw(2L, "source-jeonse-1", TransactionType.JEONSE)
        ));
        var service = new CanonicalApartmentUpsertService(mapper);

        var summary = service.upsertEligibleRawRows(10L);

        assertThat(summary.propertyCreatedCount()).isEqualTo(1);
        assertThat(summary.transactionCreatedCount()).isEqualTo(2);
        assertThat(mapper.insertedProperties).hasSize(1);
        assertThat(mapper.insertedTransactions)
                .extracting(CanonicalApartmentTransactionCommand::sourceTransactionId)
                .containsExactly("source-sale-1", "source-jeonse-1");
    }

    @Test
    void geocodingFailureRawRowIsSkippedBeforeCanonicalWrite() {
        var mapper = new FakeCanonicalMapper(List.of(raw(
                1L,
                "source-zero-result",
                TransactionType.SALE,
                GeocodingStatus.ZERO_RESULT,
                null,
                null
        )));
        var service = new CanonicalApartmentUpsertService(mapper);

        var summary = service.upsertEligibleRawRows(10L);

        assertThat(summary.processedCount()).isEqualTo(1);
        assertThat(summary.skippedCount()).isEqualTo(1);
        assertThat(mapper.insertedProperties).isEmpty();
        assertThat(mapper.insertedTransactions).isEmpty();
        assertThat(mapper.skippedRawIds).containsExactly(1L);
    }

    @Test
    void preservesSaleJeonseAndMonthlyRentAmountsInKrw() {
        var mapper = new FakeCanonicalMapper(List.of(
                raw(1L, "source-sale-1", TransactionType.SALE),
                raw(2L, "source-jeonse-1", TransactionType.JEONSE),
                raw(3L, "source-rent-1", TransactionType.MONTHLY_RENT)
        ));
        var service = new CanonicalApartmentUpsertService(mapper);

        service.upsertEligibleRawRows(10L);

        assertThat(mapper.insertedTransactions).satisfiesExactly(
                sale -> {
                    assertThat(sale.transactionType()).isEqualTo(TransactionType.SALE);
                    assertThat(sale.dealAmountKrw()).isEqualTo(1_250_000_000L);
                    assertThat(sale.depositAmountKrw()).isNull();
                    assertThat(sale.monthlyRentKrw()).isEqualTo(0L);
                },
                jeonse -> {
                    assertThat(jeonse.transactionType()).isEqualTo(TransactionType.JEONSE);
                    assertThat(jeonse.dealAmountKrw()).isNull();
                    assertThat(jeonse.depositAmountKrw()).isEqualTo(780_000_000L);
                    assertThat(jeonse.monthlyRentKrw()).isEqualTo(0L);
                },
                rent -> {
                    assertThat(rent.transactionType()).isEqualTo(TransactionType.MONTHLY_RENT);
                    assertThat(rent.dealAmountKrw()).isNull();
                    assertThat(rent.depositAmountKrw()).isEqualTo(120_000_000L);
                    assertThat(rent.monthlyRentKrw()).isEqualTo(1_500_000L);
                }
        );
    }

    private CanonicalApartmentRawRow raw(Long rawTransactionId, String sourceKey, TransactionType transactionType) {
        return raw(rawTransactionId, sourceKey, transactionType, GeocodingStatus.SUCCESS, new BigDecimal("37.5001000"), new BigDecimal("127.0364000"));
    }

    private CanonicalApartmentRawRow raw(
            Long rawTransactionId,
            String sourceKey,
            TransactionType transactionType,
            GeocodingStatus geocodingStatus,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        Long dealAmount = transactionType == TransactionType.SALE ? 1_250_000_000L : null;
        Long deposit = switch (transactionType) {
            case SALE -> null;
            case JEONSE -> 780_000_000L;
            case MONTHLY_RENT -> 120_000_000L;
        };
        Long monthlyRent = transactionType == TransactionType.MONTHLY_RENT ? 1_500_000L : 0L;
        return new CanonicalApartmentRawRow(
                rawTransactionId,
                sourceKey,
                transactionType,
                "서울특별시",
                "강남구",
                "역삼동",
                "12-3",
                "서울특별시 강남구 역삼동 12-3",
                "예시아파트",
                new BigDecimal("84.9500"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                dealAmount,
                deposit,
                monthlyRent,
                geocodingStatus,
                geocodingStatus,
                latitude,
                longitude
        );
    }

    private static class FakeCanonicalMapper implements PublicDataImportMapper {

        private long nextPropertyId = 2001L;
        private final List<CanonicalApartmentRawRow> rows;
        private final Map<String, Long> propertyIdsByIdentity = new HashMap<>();
        private final List<CanonicalApartmentPropertyCommand> insertedProperties = new ArrayList<>();
        private final List<CanonicalApartmentTransactionCommand> insertedTransactions = new ArrayList<>();
        private final List<Long> appliedRawIds = new ArrayList<>();
        private final List<Long> skippedRawIds = new ArrayList<>();
        private final List<Long> failedRawIds = new ArrayList<>();
        private final List<String> existingTransactionSourceKeys = new ArrayList<>();

        FakeCanonicalMapper(List<CanonicalApartmentRawRow> rows) {
            this.rows = rows;
        }

        @Override
        public List<CanonicalApartmentRawRow> findCanonicalUpsertCandidates(Long importRunId) {
            return rows;
        }

        @Override
        public Optional<Long> findCanonicalApartmentPropertyId(CanonicalApartmentRawRow row) {
            return Optional.ofNullable(propertyIdsByIdentity.get(identity(row)));
        }

        @Override
        public void insertCanonicalApartmentProperty(CanonicalApartmentPropertyCommand command) {
            command.setPropertyId(nextPropertyId++);
            propertyIdsByIdentity.put(identity(command), command.getPropertyId());
            insertedProperties.add(command);
        }

        @Override
        public Optional<Long> findCanonicalTransactionIdBySourceKey(String sourceKey) {
            return existingTransactionSourceKeys.contains(sourceKey) ? Optional.of(9001L) : Optional.empty();
        }

        @Override
        public int insertCanonicalApartmentTransaction(CanonicalApartmentTransactionCommand command) {
            insertedTransactions.add(command);
            existingTransactionSourceKeys.add(command.sourceTransactionId());
            return 1;
        }

        @Override
        public void markRawCanonicalApplied(Long rawTransactionId) {
            appliedRawIds.add(rawTransactionId);
        }

        @Override
        public void markRawCanonicalSkipped(Long rawTransactionId) {
            skippedRawIds.add(rawTransactionId);
        }

        @Override
        public void markRawCanonicalFailed(Long rawTransactionId) {
            failedRawIds.add(rawTransactionId);
        }

        private String identity(CanonicalApartmentRawRow row) {
            return row.sido() + "|" + row.sigungu() + "|" + row.legalDong() + "|" + row.jibun() + "|" + row.apartmentName();
        }

        private String identity(CanonicalApartmentPropertyCommand command) {
            return command.getSido() + "|" + command.getSigungu() + "|" + command.getLegalDong() + "|" + command.getJibun() + "|" + command.getName();
        }

        @Override
        public void insertImportRun(PublicDataImportRunRecord record) {
        }

        @Override
        public void markImportRunSucceeded(PublicDataImportRunRecord record) {
        }

        @Override
        public void markImportRunFailed(PublicDataImportRunRecord record) {
        }

        @Override
        public void insertImportError(PublicDataImportErrorRecord record) {
        }

        @Override
        public void upsertRawTransaction(PublicDataRawTransactionRecord record) {
        }

        @Override
        public void updateRawGeocodingStatus(String sourceKey, GeocodingStatus geocodingStatus) {
        }

        @Override
        public Optional<GeocodingCacheRecord> findGeocodingByAddressKey(String addressKey) {
            return Optional.empty();
        }

        @Override
        public void upsertGeocodingCache(GeocodingCacheRecord record) {
        }
    }
}
