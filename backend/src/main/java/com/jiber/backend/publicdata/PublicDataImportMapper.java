package com.jiber.backend.publicdata;

import java.util.Optional;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PublicDataImportMapper {

    void insertImportRun(PublicDataImportRunRecord record);

    void markImportRunSucceeded(PublicDataImportRunRecord record);

    void markImportRunFailed(PublicDataImportRunRecord record);

    void insertImportError(PublicDataImportErrorRecord record);

    void upsertRawTransaction(PublicDataRawTransactionRecord record);

    void updateRawGeocodingStatus(
            @Param("sourceKey") String sourceKey,
            @Param("geocodingStatus") GeocodingStatus geocodingStatus
    );

    Optional<GeocodingCacheRecord> findGeocodingByAddressKey(String addressKey);

    void upsertGeocodingCache(GeocodingCacheRecord record);

    List<CanonicalApartmentRawRow> findCanonicalUpsertCandidates(@Param("importRunId") Long importRunId);

    Optional<Long> findCanonicalApartmentPropertyId(CanonicalApartmentRawRow row);

    void insertCanonicalApartmentProperty(CanonicalApartmentPropertyCommand command);

    Optional<Long> findCanonicalTransactionIdBySourceKey(@Param("sourceKey") String sourceKey);

    int insertCanonicalApartmentTransaction(CanonicalApartmentTransactionCommand command);

    void markRawCanonicalApplied(@Param("rawTransactionId") Long rawTransactionId);

    void markRawCanonicalSkipped(@Param("rawTransactionId") Long rawTransactionId);

    void markRawCanonicalFailed(@Param("rawTransactionId") Long rawTransactionId);
}
