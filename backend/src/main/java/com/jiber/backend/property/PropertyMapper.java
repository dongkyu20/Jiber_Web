package com.jiber.backend.property;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PropertyMapper {

    List<PropertyListRow> findMapProperties(
            @Param("request") MapSearchRequest request,
            @Param("recentSince") LocalDate recentSince
    );

    List<AdministrativeClusterRow> findLegalDongClusters(
            @Param("request") MapSearchRequest request,
            @Param("recentSince") LocalDate recentSince
    );

    List<AdministrativeClusterRow> findSigunguClusters(
            @Param("request") MapSearchRequest request,
            @Param("recentSince") LocalDate recentSince
    );

    List<PropertyListRow> searchProperties(
            @Param("request") PropertySearchRequest request,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countSearchProperties(@Param("request") PropertySearchRequest request);

    Optional<PropertyDetailRow> findDetailById(@Param("propertyId") Long propertyId);

    List<PropertyTransactionRow> findRecentTransactions(
            @Param("propertyId") Long propertyId,
            @Param("limit") int limit
    );

    Optional<PropertyType> findPropertyTypeById(@Param("propertyId") Long propertyId);
}
