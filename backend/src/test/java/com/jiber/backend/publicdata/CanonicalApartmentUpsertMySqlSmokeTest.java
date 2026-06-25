package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.property.dto.MapSearchRequest;
import com.jiber.backend.property.mapper.PropertyMapper;
import com.jiber.backend.property.dto.PropertyType;
import com.jiber.backend.property.dto.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "JIBER_MYSQL_SMOKE", matches = "true")
class CanonicalApartmentUpsertMySqlSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CanonicalApartmentUpsertService upsertService;

    @Autowired
    private PropertyMapper propertyMapper;

    @Test
    void syntheticRawSuccessRowUpsertsCanonicalRowsAndIsVisibleToPropertyMapper() {
        var suffix = UUID.randomUUID().toString();
        var sourceKey = "MYSQL-SMOKE-" + suffix;
        var addressKey = "서울특별시|강남구|역삼동|99-" + suffix;
        var fullAddress = "서울특별시 강남구 역삼동 99-" + suffix;
        var apartmentName = "스모크 아파트 " + suffix.substring(0, 8);
        var importRunId = insertImportRun();
        insertGeocoding(addressKey, fullAddress);
        insertRawApartmentTransaction(importRunId, sourceKey, addressKey, fullAddress, apartmentName);

        var summary = upsertService.upsertEligibleRawRows(importRunId);

        assertThat(summary.processedCount()).isEqualTo(1);
        assertThat(summary.propertyCreatedCount()).isEqualTo(1);
        assertThat(summary.transactionCreatedCount()).isEqualTo(1);
        assertThat(countProperty(apartmentName, fullAddress)).isEqualTo(1);
        assertThat(countTransaction(sourceKey)).isEqualTo(1);
        assertThat(rawCanonicalStatus(sourceKey)).isEqualTo("APPLIED");

        var secondSummary = upsertService.upsertEligibleRawRows(importRunId);

        assertThat(secondSummary.processedCount()).isZero();
        assertThat(countProperty(apartmentName, fullAddress)).isEqualTo(1);
        assertThat(countTransaction(sourceKey)).isEqualTo(1);
        assertThat(mapContains(apartmentName)).isTrue();
    }

    @Test
    void officetelRawRowUsesExistingApartmentPropertyWhenSameIdentityExists() {
        var suffix = UUID.randomUUID().toString();
        var sourceKey = "MYSQL-SMOKE-OFFI-" + suffix;
        var addressKey = "SEOUL GANGNAM YEOKSAM 77-" + suffix;
        var fullAddress = "SEOUL GANGNAM YEOKSAM 77-" + suffix;
        var apartmentName = "Mixed Use Tower " + suffix.substring(0, 8);
        var importRunId = insertImportRun();
        insertGeocoding(addressKey, fullAddress);
        var apartmentPropertyId = insertApartmentProperty(apartmentName, fullAddress);
        insertRawTransaction(
                importRunId,
                sourceKey,
                addressKey,
                fullAddress,
                apartmentName,
                PropertyType.OFFICETEL,
                "SEOUL",
                "GANGNAM",
                "YEOKSAM",
                "77-1"
        );

        var summary = upsertService.upsertEligibleRawRows(importRunId);

        assertThat(summary.processedCount()).isEqualTo(1);
        assertThat(summary.propertyCreatedCount()).isZero();
        assertThat(summary.transactionCreatedCount()).isEqualTo(1);
        assertThat(transactionPropertyId(sourceKey)).isEqualTo(apartmentPropertyId);
        assertThat(countProperty(PropertyType.OFFICETEL, apartmentName, fullAddress)).isZero();
        assertThat(rawCanonicalStatus(sourceKey)).isEqualTo("APPLIED");
    }

    private Long insertImportRun() {
        jdbcTemplate.update("""
                INSERT INTO public_data_import_runs (
                    status,
                    target_months,
                    target_regions,
                    dry_run
                ) VALUES ('RUNNING', 1, 'SEOUL', false)
                """);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertGeocoding(String addressKey, String fullAddress) {
        jdbcTemplate.update("""
                INSERT INTO public_data_geocoding_cache (
                    address_key,
                    full_address,
                    status,
                    latitude,
                    longitude
                ) VALUES (?, ?, 'SUCCESS', 37.5010000, 127.0370000)
                """, addressKey, fullAddress);
    }

    private void insertRawApartmentTransaction(
            Long importRunId,
            String sourceKey,
            String addressKey,
            String fullAddress,
            String apartmentName
    ) {
        jdbcTemplate.update("""
                INSERT INTO public_data_raw_apartment_transactions (
                    import_run_id,
                    source_key,
                    transaction_type,
                    lawd_cd,
                    sido,
                    sigungu,
                    legal_dong,
                    jibun,
                    address_key,
                    full_address,
                    apartment_name,
                    exclusive_area_m2,
                    floor,
                    built_year,
                    deal_date,
                    deal_amount_krw,
                    deposit_amount_krw,
                    monthly_rent_krw,
                    geocoding_status,
                    canonical_status
                ) VALUES (
                    ?, ?, 'SALE', '11680', '서울특별시', '강남구', '역삼동', '99-1',
                    ?, ?, ?, 84.9500, 15, 2010, '2026-05-20',
                    1250000000, NULL, 0, 'SUCCESS', 'ELIGIBLE'
                )
                """, importRunId, sourceKey, addressKey, fullAddress, apartmentName);
    }

    private void insertRawTransaction(
            Long importRunId,
            String sourceKey,
            String addressKey,
            String fullAddress,
            String apartmentName,
            PropertyType propertyType,
            String sido,
            String sigungu,
            String legalDong,
            String jibun
    ) {
        jdbcTemplate.update("""
                INSERT INTO public_data_raw_apartment_transactions (
                    import_run_id,
                    source_key,
                    property_type,
                    transaction_type,
                    lawd_cd,
                    sido,
                    sigungu,
                    legal_dong,
                    jibun,
                    address_key,
                    full_address,
                    apartment_name,
                    exclusive_area_m2,
                    floor,
                    built_year,
                    deal_date,
                    deal_amount_krw,
                    deposit_amount_krw,
                    monthly_rent_krw,
                    geocoding_status,
                    canonical_status
                ) VALUES (
                    ?, ?, ?, 'SALE', '11680', ?, ?, ?, ?,
                    ?, ?, ?, 84.9500, 15, 2010, '2026-05-20',
                    1250000000, NULL, 0, 'SUCCESS', 'ELIGIBLE'
                )
                """,
                importRunId,
                sourceKey,
                propertyType.name(),
                sido,
                sigungu,
                legalDong,
                jibun,
                addressKey,
                fullAddress,
                apartmentName
        );
    }

    private Long insertApartmentProperty(String apartmentName, String fullAddress) {
        jdbcTemplate.update("""
                INSERT INTO properties (
                    property_type,
                    name,
                    sido,
                    sigungu,
                    legal_dong,
                    road_address,
                    jibun_address,
                    latitude,
                    longitude,
                    built_year,
                    household_count,
                    source_system
                ) VALUES (
                    'APARTMENT', ?, 'SEOUL', 'GANGNAM', 'YEOKSAM', NULL, ?,
                    37.5010000, 127.0370000, 2010, NULL, 'PUBLIC_DATA_PORTAL'
                )
                """, apartmentName, fullAddress);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Integer countProperty(String apartmentName, String fullAddress) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM properties
                WHERE property_type = 'APARTMENT'
                  AND name = ?
                  AND jibun_address = ?
                """, Integer.class, apartmentName, fullAddress);
    }

    private Integer countProperty(PropertyType propertyType, String apartmentName, String fullAddress) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM properties
                WHERE property_type = ?
                  AND name = ?
                  AND jibun_address = ?
                """, Integer.class, propertyType.name(), apartmentName, fullAddress);
    }

    private Integer countTransaction(String sourceKey) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM property_transactions
                WHERE source_system = 'PUBLIC_DATA_PORTAL'
                  AND source_transaction_id = ?
                """, Integer.class, sourceKey);
    }

    private Long transactionPropertyId(String sourceKey) {
        return jdbcTemplate.queryForObject("""
                SELECT property_id
                FROM property_transactions
                WHERE source_system = 'PUBLIC_DATA_PORTAL'
                  AND source_transaction_id = ?
                """, Long.class, sourceKey);
    }

    private String rawCanonicalStatus(String sourceKey) {
        return jdbcTemplate.queryForObject("""
                SELECT canonical_status
                FROM public_data_raw_apartment_transactions
                WHERE source_key = ?
                """, String.class, sourceKey);
    }

    private boolean mapContains(String apartmentName) {
        var recentSince = LocalDate.now().minusMonths(6);
        var rows = propertyMapper.findMapProperties(new MapSearchRequest(
                new BigDecimal("37.49"),
                new BigDecimal("127.02"),
                new BigDecimal("37.51"),
                new BigDecimal("127.05"),
                5,
                List.of(PropertyType.APARTMENT),
                List.of(TransactionType.SALE),
                null,
                null,
                null,
                null,
                null,
                null
        ), recentSince);
        return rows.stream().anyMatch(row -> apartmentName.equals(row.getName()));
    }
}
