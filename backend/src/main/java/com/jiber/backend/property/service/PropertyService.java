package com.jiber.backend.property.service;

import com.jiber.backend.auth.dto.AuthUserPrincipal;
import com.jiber.backend.common.PageMetadata;
import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.favorite.mapper.FavoriteMapper;
import com.jiber.backend.property.client.PropertyValuationClient;
import com.jiber.backend.property.dto.AdministrativeClusterLevel;
import com.jiber.backend.property.dto.AdministrativeClusterResponse;
import com.jiber.backend.property.dto.BoundsResponse;
import com.jiber.backend.property.dto.LatestTransactionResponse;
import com.jiber.backend.property.dto.MapFilterResponse;
import com.jiber.backend.property.dto.MapSearchRequest;
import com.jiber.backend.property.dto.NewApartmentAnalysisRequest;
import com.jiber.backend.property.dto.NewApartmentAnalysisResponse;
import com.jiber.backend.property.dto.PropertyDetailResponse;
import com.jiber.backend.property.dto.PropertyMapItemResponse;
import com.jiber.backend.property.dto.PropertyMapResponse;
import com.jiber.backend.property.dto.PropertySearchItemResponse;
import com.jiber.backend.property.dto.PropertySearchRequest;
import com.jiber.backend.property.dto.PropertySearchResponse;
import com.jiber.backend.property.dto.PropertyTransactionResponse;
import com.jiber.backend.property.dto.PropertyType;
import com.jiber.backend.property.dto.ShapRequest;
import com.jiber.backend.property.dto.ShapResponse;
import com.jiber.backend.property.dto.ValuationRequest;
import com.jiber.backend.property.dto.ValuationResponse;
import com.jiber.backend.property.mapper.AdministrativeClusterRow;
import com.jiber.backend.property.mapper.PropertyDetailRow;
import com.jiber.backend.property.mapper.PropertyListRow;
import com.jiber.backend.property.mapper.PropertyMapper;
import com.jiber.backend.property.mapper.PropertyTransactionRow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

    private final PropertyMapper propertyMapper;
    private final FavoriteMapper favoriteMapper;
    private final PropertyAiEligibilityService eligibilityService;
    private final PropertyValuationClient valuationClient;
    private final ApartmentComplexHouseholdLookup householdLookup;
    private final Clock clock;

    @Autowired
    public PropertyService(
            PropertyMapper propertyMapper,
            FavoriteMapper favoriteMapper,
            PropertyAiEligibilityService eligibilityService,
            PropertyValuationClient valuationClient,
            ApartmentComplexHouseholdLookup householdLookup
    ) {
        this(propertyMapper, favoriteMapper, eligibilityService, valuationClient, householdLookup, Clock.systemDefaultZone());
    }

    PropertyService(
            PropertyMapper propertyMapper,
            FavoriteMapper favoriteMapper,
            PropertyAiEligibilityService eligibilityService,
            PropertyValuationClient valuationClient,
            Clock clock
    ) {
        this(propertyMapper, favoriteMapper, eligibilityService, valuationClient, (property, ignored) -> java.util.Optional.empty(), clock);
    }

    PropertyService(
            PropertyMapper propertyMapper,
            FavoriteMapper favoriteMapper,
            PropertyAiEligibilityService eligibilityService,
            PropertyValuationClient valuationClient,
            ApartmentComplexHouseholdLookup householdLookup,
            Clock clock
    ) {
        this.propertyMapper = propertyMapper;
        this.favoriteMapper = favoriteMapper;
        this.eligibilityService = eligibilityService;
        this.valuationClient = valuationClient;
        this.householdLookup = householdLookup;
        this.clock = clock;
    }

    public PropertyMapResponse findMapProperties(MapSearchRequest request) {
        var recentSince = LocalDate.now(clock).minusYears(1);
        return new PropertyMapResponse(
                propertyMapper.findMapProperties(request, recentSince).stream()
                        .map(this::toMapItem)
                        .toList(),
                administrativeClusters(request, recentSince),
                new BoundsResponse(request.swLat(), request.swLng(), request.neLat(), request.neLng()),
                new MapFilterResponse(request.propertyTypes(), request.transactionTypes(), request.zoomLevel())
        );
    }

    public PropertySearchResponse searchProperties(PropertySearchRequest request) {
        var page = request.effectivePage();
        var size = request.effectiveSize();
        var offset = page * size;
        var items = propertyMapper.searchProperties(request, size, offset).stream()
                .map(this::toSearchItem)
                .toList();
        var totalElements = propertyMapper.countSearchProperties(request);
        return new PropertySearchResponse(items, pageMetadata(page, size, totalElements));
    }

    public PropertyDetailResponse getPropertyDetail(Long propertyId) {
        return getPropertyDetail(propertyId, null);
    }

    public PropertyDetailResponse getPropertyDetail(Long propertyId, AuthUserPrincipal principal) {
        var row = propertyMapper.findDetailById(propertyId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND));
        var transactions = propertyMapper.findTransactionsByPropertyId(propertyId).stream()
                .map(this::toTransaction)
                .toList();
        var aiAvailable = row.getPropertyType() == PropertyType.APARTMENT;
        var apartmentFavorited = principal != null
                && principal.userId() != null
                && favoriteMapper.existsFavoriteApartment(principal.userId(), propertyId);
        return new PropertyDetailResponse(
                row.getPropertyId(),
                row.getPropertyType(),
                row.getName(),
                new PropertyDetailResponse.Address(row.getSido(), row.getSigungu(), row.getLegalDong(), row.getRoadAddress()),
                new PropertyDetailResponse.Location(toDouble(row.getLatitude()), toDouble(row.getLongitude())),
                new PropertyDetailResponse.Summary(row.getBuiltYear(), householdCount(row, propertyId), row.getLatestDealAmount(), row.getLatestDealDate()),
                transactions,
                new PropertyDetailResponse.FavoriteSummary(apartmentFavorited, false),
                new PropertyDetailResponse.AiMetadata(
                        aiAvailable,
                        aiAvailable,
                        aiAvailable ? null : ErrorCode.VALUATION_UNSUPPORTED_PROPERTY_TYPE.name()
                )
        );
    }

    private Integer householdCount(PropertyDetailRow row, Long propertyId) {
        if (row.getHouseholdCount() != null || row.getPropertyType() != PropertyType.APARTMENT) {
            return row.getHouseholdCount();
        }
        return householdLookup.findHouseholdCount(row, propertyMapper.findApartmentNameHintsByPropertyId(propertyId)).orElse(null);
    }

    public ValuationResponse valuateApartment(Long propertyId, ValuationRequest request) {
        var row = resolvePropertyForAi(propertyId);
        eligibilityService.ensureApartmentSupported(row.getPropertyType());
        return valuationClient.valuateApartment(row, request);
    }

    public ShapResponse explainApartment(Long propertyId, ShapRequest request) {
        var row = resolvePropertyForAi(propertyId);
        eligibilityService.ensureApartmentSupported(row.getPropertyType());
        return valuationClient.explainApartment(row, request);
    }

    public NewApartmentAnalysisResponse analyzeNewApartment(NewApartmentAnalysisRequest request) {
        var enrichedRequest = enrichNewApartmentAnalysisRequest(request);
        var valuation = valuationClient.valuateNewApartment(enrichedRequest);
        var shap = valuationClient.explainNewApartment(enrichedRequest);
        return new NewApartmentAnalysisResponse(
                request.propertyName().trim(),
                valuation,
                shap,
                "입력한 신규 아파트 조건으로 적정가와 주요 요인을 계산했습니다."
        );
    }

    private NewApartmentAnalysisRequest enrichNewApartmentAnalysisRequest(NewApartmentAnalysisRequest request) {
        if (request.latitude() != null && request.longitude() != null && request.householdCount() != null) {
            return request;
        }

        var matchedProperty = propertyMapper.findBestApartmentForNewAnalysis(request).orElse(null);
        var areaCentroid = needsAreaFallback(request, matchedProperty)
                ? propertyMapper.findAreaCentroidForNewAnalysis(request).orElse(null)
                : null;
        if (matchedProperty == null && areaCentroid == null) {
            return request;
        }

        return new NewApartmentAnalysisRequest(
                request.propertyName(),
                request.sido(),
                request.sigungu(),
                request.legalDong(),
                firstNonNull(request.latitude(), valueOrNull(matchedProperty, PropertyDetailRow::getLatitude), valueOrNull(areaCentroid, PropertyDetailRow::getLatitude)),
                firstNonNull(request.longitude(), valueOrNull(matchedProperty, PropertyDetailRow::getLongitude), valueOrNull(areaCentroid, PropertyDetailRow::getLongitude)),
                firstNonNull(request.householdCount(), valueOrNull(matchedProperty, PropertyDetailRow::getHouseholdCount), valueOrNull(areaCentroid, PropertyDetailRow::getHouseholdCount)),
                request.exclusiveAreaM2(),
                request.floor(),
                request.builtYear(),
                request.asOfDate(),
                request.distanceToStationM()
        );
    }

    private boolean needsAreaFallback(NewApartmentAnalysisRequest request, PropertyDetailRow matchedProperty) {
        return (request.latitude() == null && valueOrNull(matchedProperty, PropertyDetailRow::getLatitude) == null)
                || (request.longitude() == null && valueOrNull(matchedProperty, PropertyDetailRow::getLongitude) == null)
                || (request.householdCount() == null && valueOrNull(matchedProperty, PropertyDetailRow::getHouseholdCount) == null);
    }

    private <T> T valueOrNull(PropertyDetailRow row, java.util.function.Function<PropertyDetailRow, T> getter) {
        return row == null ? null : getter.apply(row);
    }

    private <T> T firstNonNull(T first, T second, T third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private PropertyDetailRow resolvePropertyForAi(Long propertyId) {
        return propertyMapper.findDetailById(propertyId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND));
    }

    private PropertyMapItemResponse toMapItem(PropertyListRow row) {
        return new PropertyMapItemResponse(
                row.getPropertyId(),
                row.getPropertyType(),
                row.getName(),
                row.getAddress(),
                toDouble(row.getLatitude()),
                toDouble(row.getLongitude()),
                toLatestTransaction(row),
                row.getDealCount() == null ? 0 : row.getDealCount(),
                row.getRecentTransactionCount(),
                row.getRecentYearAverageDealAmount(),
                row.getRecentYearAverageJeonseDepositAmount(),
                row.getPropertyType() == PropertyType.APARTMENT
        );
    }

    private List<AdministrativeClusterResponse> administrativeClusters(MapSearchRequest request, LocalDate recentSince) {
        if (request.zoomLevel() < 5) {
            return List.of();
        }
        if (request.zoomLevel() < 7) {
            return propertyMapper.findLegalDongClusters(request, recentSince).stream()
                    .map(row -> toAdministrativeCluster(row, AdministrativeClusterLevel.LEGAL_DONG))
                    .toList();
        }
        return propertyMapper.findSigunguClusters(request, recentSince).stream()
                .map(row -> toAdministrativeCluster(row, AdministrativeClusterLevel.SIGUNGU))
                .toList();
    }

    private AdministrativeClusterResponse toAdministrativeCluster(
            AdministrativeClusterRow row,
            AdministrativeClusterLevel level
    ) {
        return new AdministrativeClusterResponse(
                clusterId(row, level),
                level,
                row.getSido(),
                row.getSigungu(),
                row.getLegalDong(),
                row.getLabel(),
                toDouble(row.getCenterLat()),
                toDouble(row.getCenterLng()),
                row.getPropertyCount() == null ? 0 : row.getPropertyCount(),
                row.getTransactionCount() == null ? 0 : row.getTransactionCount(),
                row.getAverageDealAmount()
        );
    }

    private String clusterId(AdministrativeClusterRow row, AdministrativeClusterLevel level) {
        if (level == AdministrativeClusterLevel.SIGUNGU) {
            return "SIGUNGU:%s:%s".formatted(row.getSido(), row.getSigungu());
        }
        return "LEGAL_DONG:%s:%s:%s".formatted(row.getSido(), row.getSigungu(), row.getLegalDong());
    }

    private PropertySearchItemResponse toSearchItem(PropertyListRow row) {
        return new PropertySearchItemResponse(
                row.getPropertyId(),
                row.getPropertyType(),
                row.getName(),
                row.getAddress(),
                row.getLegalDong(),
                toDouble(row.getLatitude()),
                toDouble(row.getLongitude()),
                row.getDistanceM(),
                toLatestTransaction(row),
                row.getPropertyType() == PropertyType.APARTMENT
        );
    }

    private LatestTransactionResponse toLatestTransaction(PropertyListRow row) {
        if (row.getLatestTransactionType() == null) {
            return null;
        }
        return new LatestTransactionResponse(
                row.getLatestTransactionType(),
                row.getLatestDealAmount(),
                row.getLatestDepositAmount(),
                row.getLatestMonthlyRent(),
                row.getLatestDealDate()
        );
    }

    private PropertyTransactionResponse toTransaction(PropertyTransactionRow row) {
        return new PropertyTransactionResponse(
                row.getTransactionId(),
                row.getTransactionType(),
                row.getExclusiveAreaM2(),
                row.getFloor(),
                row.getDealAmount(),
                row.getDepositAmount(),
                row.getMonthlyRent(),
                row.getDealDate()
        );
    }

    private PageMetadata pageMetadata(int page, int size, long totalElements) {
        if (totalElements == 0) {
            return PageMetadata.empty(page, size);
        }
        return new PageMetadata(page, size, totalElements, (int) Math.ceil((double) totalElements / size));
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
