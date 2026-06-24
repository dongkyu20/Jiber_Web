package com.jiber.backend.property.mapper;

import com.jiber.backend.property.dto.MapSearchRequest;
import com.jiber.backend.property.dto.PropertySearchRequest;
import com.jiber.backend.property.dto.PropertyType;
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

    List<PropertyTransactionRow> findTransactionsByPropertyId(@Param("propertyId") Long propertyId);

    List<String> findApartmentNameHintsByPropertyId(@Param("propertyId") Long propertyId);

    Optional<PropertyType> findPropertyTypeById(@Param("propertyId") Long propertyId);
}
