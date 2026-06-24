package com.jiber.backend.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jiber.backend.auth.AuthUserPrincipal;
import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.favorite.FavoriteAreaInsertCommand;
import com.jiber.backend.favorite.FavoriteAreaRow;
import com.jiber.backend.favorite.FavoriteApartmentRow;
import com.jiber.backend.favorite.FavoriteMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PropertyServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate EXPECTED_RECENT_SINCE = LocalDate.of(2025, 6, 23);

    @Test
    void searchPropertiesReturnsEmptyPageFromMapper() {
        var mapper = new FakePropertyMapper();
        var service = service(mapper, new RecordingValuationClient());
        var request = new PropertySearchRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(PropertyType.APARTMENT),
                List.of(TransactionType.SALE),
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                10,
                "latestDealDate,desc"
        );

        var response = service.searchProperties(request);

        assertThat(response.items()).isEmpty();
        assertThat(response.page().number()).isEqualTo(1);
        assertThat(response.page().size()).isEqualTo(10);
        assertThat(response.page().totalElements()).isZero();
        assertThat(response.page().totalPages()).isZero();
        assertThat(mapper.searchLimit).isEqualTo(10);
        assertThat(mapper.searchOffset).isEqualTo(10);
    }

    @Test
    void propertyDetailThrowsNotFoundWhenMapperHasNoRow() {
        var mapper = new FakePropertyMapper();
        var service = service(mapper, new RecordingValuationClient());

        assertThatThrownBy(() -> service.getPropertyDetail(404L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND));
    }

    @Test
    void aiEndpointsRejectNonApartmentFromDbBeforeCallingModelServer() {
        var mapper = new FakePropertyMapper();
        mapper.detailRow = sampleDetailRow(PropertyType.OFFICETEL);
        var valuationClient = new RecordingValuationClient();
        var service = service(mapper, valuationClient);

        assertThatThrownBy(() -> service.valuateApartment(1901L, valuationRequest()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALUATION_UNSUPPORTED_PROPERTY_TYPE));
        assertThatThrownBy(() -> service.explainApartment(1901L, shapRequest()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALUATION_UNSUPPORTED_PROPERTY_TYPE));
        assertThat(valuationClient.valuationCalled).isFalse();
        assertThat(valuationClient.shapCalled).isFalse();
    }

    @Test
    void aiEndpointsPassPropertyDetailToModelServerClient() {
        var mapper = new FakePropertyMapper();
        mapper.detailRow = sampleDetailRow(PropertyType.APARTMENT);
        var valuationClient = new RecordingValuationClient();
        var service = service(mapper, valuationClient);

        service.valuateApartment(1001L, valuationRequest());
        service.explainApartment(1001L, shapRequest());

        assertThat(valuationClient.valuationProperty).isSameAs(mapper.detailRow);
        assertThat(valuationClient.shapProperty).isSameAs(mapper.detailRow);
    }

    @Test
    void mapPropertiesConvertsMapperRowsToContractResponse() {
        var mapper = new FakePropertyMapper();
        mapper.mapRows.add(sampleListRow());
        var service = service(mapper, new RecordingValuationClient());
        var request = mapRequest(5);

        var response = service.findMapProperties(request);

        assertThat(response.bounds().swLat()).isEqualByComparingTo("37.40");
        assertThat(response.filters().zoomLevel()).isEqualTo(5);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.propertyId()).isEqualTo(1001L);
            assertThat(item.propertyType()).isEqualTo(PropertyType.APARTMENT);
            assertThat(item.address()).isEqualTo("서울특별시 강남구 역삼동 12-3");
            assertThat(item.latestTransaction().transactionType()).isEqualTo(TransactionType.SALE);
            assertThat(item.latestTransaction().dealAmount()).isEqualTo(1_250_000_000L);
            assertThat(item.dealCount()).isEqualTo(2);
            assertThat(item.recentYearAverageDealAmount()).isEqualTo(1_180_000_000L);
            assertThat(item.recentYearAverageJeonseDepositAmount()).isEqualTo(780_000_000L);
            assertThat(item.aiAvailable()).isTrue();
        });
    }

    @Test
    void mapPropertiesAtLevelFourIncludesRecentTransactionCountAndNoAdministrativeClusters() {
        var mapper = new FakePropertyMapper();
        mapper.mapRows.add(sampleListRow());
        var service = service(mapper, new RecordingValuationClient());

        var response = service.findMapProperties(mapRequest(4));

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.recentTransactionCount()).isEqualTo(3);
            assertThat(item.aiAvailable()).isTrue();
        });
        assertThat(response.administrativeClusters()).isEmpty();
        assertThat(mapper.mapRecentSince).isEqualTo(EXPECTED_RECENT_SINCE);
        assertThat(mapper.legalDongClustersCalled).isFalse();
        assertThat(mapper.sigunguClustersCalled).isFalse();
    }

    @Test
    void mapPropertiesAtLevelFiveReturnsLegalDongAdministrativeClusters() {
        var mapper = new FakePropertyMapper();
        mapper.legalDongClusters.add(sampleClusterRow(
                "Sido",
                "Sigungu",
                "LegalDong",
                "LegalDong",
                new BigDecimal("37.5001000"),
                new BigDecimal("127.0364000"),
                7,
                11,
                1_180_000_000L
        ));
        var service = service(mapper, new RecordingValuationClient());

        var response = service.findMapProperties(mapRequest(5));

        assertThat(response.administrativeClusters()).singleElement().satisfies(cluster -> {
            assertThat(cluster.clusterId()).isEqualTo("LEGAL_DONG:Sido:Sigungu:LegalDong");
            assertThat(cluster.level()).isEqualTo(AdministrativeClusterLevel.LEGAL_DONG);
            assertThat(cluster.sido()).isEqualTo("Sido");
            assertThat(cluster.sigungu()).isEqualTo("Sigungu");
            assertThat(cluster.legalDong()).isEqualTo("LegalDong");
            assertThat(cluster.label()).isEqualTo("LegalDong");
            assertThat(cluster.centerLat()).isEqualTo(37.5001000d);
            assertThat(cluster.centerLng()).isEqualTo(127.0364000d);
            assertThat(cluster.propertyCount()).isEqualTo(7);
            assertThat(cluster.transactionCount()).isEqualTo(11);
            assertThat(cluster.averageDealAmount()).isEqualTo(1_180_000_000L);
        });
        assertThat(mapper.legalDongRequest.zoomLevel()).isEqualTo(5);
        assertThat(mapper.legalDongRecentSince).isEqualTo(EXPECTED_RECENT_SINCE);
        assertThat(mapper.sigunguClustersCalled).isFalse();
    }

    @Test
    void mapPropertiesAtLevelSevenReturnsSigunguAdministrativeClusters() {
        var mapper = new FakePropertyMapper();
        mapper.sigunguClusters.add(sampleClusterRow(
                "Sido",
                "Sigungu",
                null,
                "Sigungu",
                new BigDecimal("37.5172000"),
                new BigDecimal("127.0473000"),
                null,
                null,
                null
        ));
        var service = service(mapper, new RecordingValuationClient());

        var response = service.findMapProperties(mapRequest(7));

        assertThat(response.administrativeClusters()).singleElement().satisfies(cluster -> {
            assertThat(cluster.clusterId()).isEqualTo("SIGUNGU:Sido:Sigungu");
            assertThat(cluster.level()).isEqualTo(AdministrativeClusterLevel.SIGUNGU);
            assertThat(cluster.sido()).isEqualTo("Sido");
            assertThat(cluster.sigungu()).isEqualTo("Sigungu");
            assertThat(cluster.legalDong()).isNull();
            assertThat(cluster.label()).isEqualTo("Sigungu");
            assertThat(cluster.centerLat()).isEqualTo(37.5172000d);
            assertThat(cluster.centerLng()).isEqualTo(127.0473000d);
            assertThat(cluster.propertyCount()).isZero();
            assertThat(cluster.transactionCount()).isZero();
            assertThat(cluster.averageDealAmount()).isNull();
        });
        assertThat(mapper.sigunguRequest.zoomLevel()).isEqualTo(7);
        assertThat(mapper.sigunguRecentSince).isEqualTo(EXPECTED_RECENT_SINCE);
        assertThat(mapper.legalDongClustersCalled).isFalse();
    }

    @Test
    void propertyDetailReturnsCanonicalLatestSummaryAndRecentTransactions() {
        var mapper = new FakePropertyMapper();
        mapper.detailRow = sampleDetailRow();
        mapper.transactionRows.add(sampleTransactionRow(5001L, TransactionType.SALE, 1_250_000_000L, null, 0L, LocalDate.of(2026, 5, 20)));
        mapper.transactionRows.add(sampleTransactionRow(5002L, TransactionType.JEONSE, null, 780_000_000L, 0L, LocalDate.of(2026, 3, 15)));
        var service = service(mapper, new RecordingValuationClient());

        var response = service.getPropertyDetail(1001L);

        assertThat(response.propertyId()).isEqualTo(1001L);
        assertThat(response.summary().latestDealAmount()).isEqualTo(1_250_000_000L);
        assertThat(response.summary().latestDealDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(response.transactions()).hasSize(2);
        assertThat(response.transactions().get(0).transactionType()).isEqualTo(TransactionType.SALE);
        assertThat(response.transactions().get(0).dealAmount()).isEqualTo(1_250_000_000L);
        assertThat(response.transactions().get(1).transactionType()).isEqualTo(TransactionType.JEONSE);
        assertThat(response.transactions().get(1).depositAmount()).isEqualTo(780_000_000L);
        assertThat(response.favorite().apartmentFavorited()).isFalse();
        assertThat(response.ai().valuationAvailable()).isTrue();
    }

    @Test
    void propertyDetailKeepsHouseholdCountFromDbWhenPresent() {
        var mapper = new FakePropertyMapper();
        mapper.detailRow = sampleDetailRow(PropertyType.APARTMENT);
        mapper.detailRow.setHouseholdCount(500);
        var householdLookup = new FakeApartmentComplexHouseholdLookup();
        householdLookup.householdCount = 806;
        var service = service(mapper, new FakeFavoriteMapper(), new RecordingValuationClient(), householdLookup);

        var response = service.getPropertyDetail(1001L);

        assertThat(response.summary().householdCount()).isEqualTo(500);
        assertThat(householdLookup.requests).isEmpty();
    }

    @Test
    void propertyDetailFillsMissingApartmentHouseholdCountFromComplexData() {
        var mapper = new FakePropertyMapper();
        mapper.detailRow = sampleDetailRow(PropertyType.APARTMENT);
        mapper.detailRow.setHouseholdCount(null);
        mapper.apartmentNameHints = List.of("샘플역삼아파트");
        var householdLookup = new FakeApartmentComplexHouseholdLookup();
        householdLookup.householdCount = 806;
        var service = service(mapper, new FakeFavoriteMapper(), new RecordingValuationClient(), householdLookup);

        var response = service.getPropertyDetail(1001L);

        assertThat(response.summary().householdCount()).isEqualTo(806);
        assertThat(householdLookup.requests).containsExactly(mapper.detailRow);
        assertThat(householdLookup.apartmentNameHints).containsExactly(List.of("샘플역삼아파트"));
    }

    @Test
    void propertyDetailKeepsMissingHouseholdCountForNonApartment() {
        var mapper = new FakePropertyMapper();
        mapper.detailRow = sampleDetailRow(PropertyType.OFFICETEL);
        mapper.detailRow.setHouseholdCount(null);
        var householdLookup = new FakeApartmentComplexHouseholdLookup();
        householdLookup.householdCount = 806;
        var service = service(mapper, new FakeFavoriteMapper(), new RecordingValuationClient(), householdLookup);

        var response = service.getPropertyDetail(1001L);

        assertThat(response.summary().householdCount()).isNull();
        assertThat(householdLookup.requests).isEmpty();
    }

    @Test
    void propertyDetailReturnsFavoriteFlagForLoggedInUser() {
        var mapper = new FakePropertyMapper();
        mapper.detailRow = sampleDetailRow();
        var favoriteMapper = new FakeFavoriteMapper();
        favoriteMapper.favorite(7L, 1001L);
        var service = service(mapper, favoriteMapper, new RecordingValuationClient());
        var principal = new AuthUserPrincipal(7L, "user@example.com", "사용자", Set.of("USER"));

        var response = service.getPropertyDetail(1001L, principal);

        assertThat(response.favorite().apartmentFavorited()).isTrue();
        assertThat(response.favorite().areaFavorited()).isFalse();
    }

    private PropertyService service(PropertyMapper mapper, PropertyValuationClient valuationClient) {
        return service(mapper, new FakeFavoriteMapper(), valuationClient);
    }

    private PropertyService service(PropertyMapper mapper, FavoriteMapper favoriteMapper, PropertyValuationClient valuationClient) {
        return new PropertyService(mapper, favoriteMapper, new PropertyAiEligibilityService(), valuationClient, FIXED_CLOCK);
    }

    private PropertyService service(
            PropertyMapper mapper,
            FavoriteMapper favoriteMapper,
            PropertyValuationClient valuationClient,
            ApartmentComplexHouseholdLookup householdLookup
    ) {
        return new PropertyService(
                mapper,
                favoriteMapper,
                new PropertyAiEligibilityService(),
                valuationClient,
                householdLookup,
                FIXED_CLOCK
        );
    }

    private ValuationRequest valuationRequest() {
        return new ValuationRequest(new BigDecimal("84.95"), 15, LocalDate.of(2026, 6, 12));
    }

    private ShapRequest shapRequest() {
        return new ShapRequest(new BigDecimal("84.95"), 15, LocalDate.of(2026, 6, 12));
    }

    private MapSearchRequest mapRequest(int zoomLevel) {
        return new MapSearchRequest(
                new BigDecimal("37.40"),
                new BigDecimal("126.90"),
                new BigDecimal("37.60"),
                new BigDecimal("127.20"),
                zoomLevel,
                List.of(PropertyType.APARTMENT),
                List.of(TransactionType.SALE),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private PropertyListRow sampleListRow() {
        var row = new PropertyListRow();
        row.setPropertyId(1001L);
        row.setPropertyType(PropertyType.APARTMENT);
        row.setName("샘플 역삼아파트");
        row.setAddress("서울특별시 강남구 역삼동 12-3");
        row.setLegalDong("역삼동");
        row.setLatitude(new BigDecimal("37.5001000"));
        row.setLongitude(new BigDecimal("127.0364000"));
        row.setLatestTransactionType(TransactionType.SALE);
        row.setLatestDealAmount(1_250_000_000L);
        row.setLatestDealDate(LocalDate.of(2026, 5, 20));
        row.setDealCount(2);
        row.setRecentTransactionCount(3);
        row.setRecentYearAverageDealAmount(1_180_000_000L);
        row.setRecentYearAverageJeonseDepositAmount(780_000_000L);
        return row;
    }

    private AdministrativeClusterRow sampleClusterRow(
            String sido,
            String sigungu,
            String legalDong,
            String label,
            BigDecimal centerLat,
            BigDecimal centerLng,
            Integer propertyCount,
            Integer transactionCount,
            Long averageDealAmount
    ) {
        var row = new AdministrativeClusterRow();
        row.setSido(sido);
        row.setSigungu(sigungu);
        row.setLegalDong(legalDong);
        row.setLabel(label);
        row.setCenterLat(centerLat);
        row.setCenterLng(centerLng);
        row.setPropertyCount(propertyCount);
        row.setTransactionCount(transactionCount);
        row.setAverageDealAmount(averageDealAmount);
        return row;
    }

    private PropertyDetailRow sampleDetailRow() {
        return sampleDetailRow(PropertyType.APARTMENT);
    }

    private PropertyDetailRow sampleDetailRow(PropertyType propertyType) {
        var row = new PropertyDetailRow();
        row.setPropertyId(1001L);
        row.setPropertyType(propertyType);
        row.setName("샘플 역삼아파트");
        row.setSido("서울특별시");
        row.setSigungu("강남구");
        row.setLegalDong("역삼동");
        row.setRoadAddress("서울특별시 강남구 테헤란로 123");
        row.setJibunAddress("서울특별시 강남구 역삼동 12-3");
        row.setLatitude(new BigDecimal("37.5001000"));
        row.setLongitude(new BigDecimal("127.0364000"));
        row.setBuiltYear(2010);
        row.setHouseholdCount(500);
        row.setLatestDealAmount(1_250_000_000L);
        row.setLatestDealDate(LocalDate.of(2026, 5, 20));
        return row;
    }

    private PropertyTransactionRow sampleTransactionRow(
            Long transactionId,
            TransactionType transactionType,
            Long dealAmount,
            Long depositAmount,
            Long monthlyRent,
            LocalDate dealDate
    ) {
        var row = new PropertyTransactionRow();
        row.setTransactionId(transactionId);
        row.setTransactionType(transactionType);
        row.setExclusiveAreaM2(new BigDecimal("84.9500"));
        row.setFloor(15);
        row.setDealAmount(dealAmount);
        row.setDepositAmount(depositAmount);
        row.setMonthlyRent(monthlyRent);
        row.setDealDate(dealDate);
        return row;
    }

    private static class FakePropertyMapper implements PropertyMapper {

        private final List<PropertyListRow> mapRows = new ArrayList<>();
        private final List<AdministrativeClusterRow> legalDongClusters = new ArrayList<>();
        private final List<AdministrativeClusterRow> sigunguClusters = new ArrayList<>();
        private final List<PropertyListRow> searchRows = new ArrayList<>();
        private final List<PropertyTransactionRow> transactionRows = new ArrayList<>();
        private List<String> apartmentNameHints = List.of();
        private PropertyType propertyType;
        private PropertyDetailRow detailRow;
        private int searchLimit;
        private int searchOffset;
        private long totalElements;
        private LocalDate mapRecentSince;
        private MapSearchRequest legalDongRequest;
        private LocalDate legalDongRecentSince;
        private boolean legalDongClustersCalled;
        private MapSearchRequest sigunguRequest;
        private LocalDate sigunguRecentSince;
        private boolean sigunguClustersCalled;

        @Override
        public List<PropertyListRow> findMapProperties(MapSearchRequest request, LocalDate recentSince) {
            this.mapRecentSince = recentSince;
            return mapRows;
        }

        @Override
        public List<AdministrativeClusterRow> findLegalDongClusters(MapSearchRequest request, LocalDate recentSince) {
            this.legalDongRequest = request;
            this.legalDongRecentSince = recentSince;
            this.legalDongClustersCalled = true;
            return legalDongClusters;
        }

        @Override
        public List<AdministrativeClusterRow> findSigunguClusters(MapSearchRequest request, LocalDate recentSince) {
            this.sigunguRequest = request;
            this.sigunguRecentSince = recentSince;
            this.sigunguClustersCalled = true;
            return sigunguClusters;
        }

        @Override
        public List<PropertyListRow> searchProperties(PropertySearchRequest request, int limit, int offset) {
            this.searchLimit = limit;
            this.searchOffset = offset;
            return searchRows;
        }

        @Override
        public long countSearchProperties(PropertySearchRequest request) {
            return totalElements;
        }

        @Override
        public Optional<PropertyDetailRow> findDetailById(Long propertyId) {
            return Optional.ofNullable(detailRow);
        }

        @Override
        public List<PropertyTransactionRow> findTransactionsByPropertyId(Long propertyId) {
            return transactionRows;
        }

        @Override
        public List<String> findApartmentNameHintsByPropertyId(Long propertyId) {
            return apartmentNameHints;
        }

        @Override
        public Optional<PropertyType> findPropertyTypeById(Long propertyId) {
            return Optional.ofNullable(propertyType);
        }
    }

    private static class FakeFavoriteMapper implements FavoriteMapper {

        private final Set<String> favorites = new HashSet<>();

        void favorite(Long userId, Long propertyId) {
            favorites.add(key(userId, propertyId));
        }

        @Override
        public Optional<PropertyType> findPropertyTypeById(Long propertyId) {
            return Optional.of(PropertyType.APARTMENT);
        }

        @Override
        public List<FavoriteApartmentRow> findFavoriteApartments(Long userId) {
            return List.of();
        }

        @Override
        public Optional<FavoriteApartmentRow> findFavoriteApartment(Long userId, Long propertyId) {
            return Optional.empty();
        }

        @Override
        public int insertFavoriteApartment(Long userId, Long propertyId) {
            return 0;
        }

        @Override
        public int deleteFavoriteApartment(Long userId, Long propertyId) {
            return 0;
        }

        @Override
        public boolean existsFavoriteApartment(Long userId, Long propertyId) {
            return favorites.contains(key(userId, propertyId));
        }

        @Override
        public List<FavoriteAreaRow> findFavoriteAreas(Long userId) {
            return List.of();
        }

        @Override
        public Optional<FavoriteAreaRow> findFavoriteAreaByNormalizedKey(Long userId, String normalizedKey) {
            return Optional.empty();
        }

        @Override
        public int insertFavoriteArea(FavoriteAreaInsertCommand command) {
            return 0;
        }

        @Override
        public int deleteFavoriteArea(Long userId, Long favoriteAreaId) {
            return 0;
        }

        @Override
        public boolean existsFavoriteAreaByNormalizedKey(Long userId, String normalizedKey) {
            return false;
        }

        private String key(Long userId, Long propertyId) {
            return userId + ":" + propertyId;
        }
    }

    private static class RecordingValuationClient implements PropertyValuationClient {

        private boolean valuationCalled;
        private boolean shapCalled;
        private PropertyDetailRow valuationProperty;
        private PropertyDetailRow shapProperty;

        @Override
        public ValuationResponse valuateApartment(PropertyDetailRow property, ValuationRequest request) {
            valuationCalled = true;
            valuationProperty = property;
            return null;
        }

        @Override
        public ShapResponse explainApartment(PropertyDetailRow property, ShapRequest request) {
            shapCalled = true;
            shapProperty = property;
            return null;
        }
    }

    private static class FakeApartmentComplexHouseholdLookup implements ApartmentComplexHouseholdLookup {

        private final List<PropertyDetailRow> requests = new ArrayList<>();
        private final List<List<String>> apartmentNameHints = new ArrayList<>();
        private Integer householdCount;

        @Override
        public Optional<Integer> findHouseholdCount(PropertyDetailRow property, List<String> apartmentNameHints) {
            requests.add(property);
            this.apartmentNameHints.add(apartmentNameHints);
            return Optional.ofNullable(householdCount);
        }
    }
}
