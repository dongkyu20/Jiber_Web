# Map Administrative Clustering Design

Date: 2026-06-23

## Summary

The map search screen will show fewer individual property markers as the Kakao map level gets wider. From map level 4 and above, it will always show Kakao `MarkerClusterer` clusters for recent transaction volume. From map level 5 and above, it will also show administrative-area clusters with recent 6-month average price information.

The feature is for visual exploration only. It must not provide investment advice or buy/sell recommendations.

## Current Context

- Frontend map rendering is owned by `frontend/src/components/KakaoMapPanel.vue` and `frontend/src/map/kakaoMap.ts`.
- The Kakao Maps loader already requests `libraries=services,clusterer`.
- `GET /api/v1/properties/map` currently returns individual `items`, `bounds`, and `filters`.
- Backend property rows already contain `sigungu` and `legal_dong`, but map item responses do not expose enough aggregate data for area clusters.

## Goals

- Reduce visual clutter when the map view is wide.
- Preserve the current result list and property detail navigation.
- Show filter-aware, visible-area-aware recent 6-month statistics.
- Show two cluster layers together from map level 5 and above:
  - Kakao `MarkerClusterer`: arbitrary screen-area clusters with recent 6-month transaction count.
  - Administrative clusters: dong or sigungu clusters with recent 6-month average price and transaction count.

## Non-Goals

- Do not change authentication or authorization behavior.
- Do not change the property detail contract.
- Do not introduce investment recommendations.
- Do not require a new map provider.

## Zoom-Level Behavior

Kakao map level numbers are treated as the source of truth.

| Map level | Individual property markers | Kakao MarkerClusterer | Administrative clusters |
| --- | --- | --- | --- |
| 1-3 | Show | Hide | Hide |
| 4 | Hide individual property markers; use clustered markers | Show recent 6-month transaction count | Hide |
| 5-6 | Hide individual property markers; use clustered markers | Show recent 6-month transaction count | Show one cluster per visible dong |
| 7+ | Hide individual property markers; use clustered markers | Show recent 6-month transaction count | Show one cluster per visible sigungu |

For levels 5 and above, the Kakao MarkerClusterer and administrative clusters must both be visible. Administrative clusters are the larger information labels. MarkerClusterer clusters are smaller transaction-count badges so the two layers can be distinguished visually.

## Data Rules

- All cluster data follows the current map bounds and current filters:
  - property type filters
  - transaction type filters
  - deal amount filters
  - exclusive area filters
  - deal year filters
- Recent 6-month cluster statistics use transactions where `deal_date >= CURRENT_DATE - INTERVAL 6 MONTH`.
- Average price uses `COALESCE(deal_amount_krw, deposit_amount_krw)` so sale and jeonse filters have comparable price-like values.
- Monthly rent rows without either deal amount or deposit amount are excluded from average-price calculation but still count as transactions if they match the selected filters.
- Administrative clusters use only properties whose coordinates are inside the visible map bounds.
- Cluster center is the average latitude and longitude of the properties represented by the cluster.
- A cluster with no recent 6-month price values returns `averageDealAmount: null` and still shows its transaction count.

## API Contract

Extend `GET /api/v1/properties/map` response with cluster metadata while keeping existing fields compatible. Existing map item fields stay intact, and each item gains `recentTransactionCount` so Kakao MarkerClusterer can show recent 6-month transaction volume rather than marker count.

```json
{
  "items": [
    {
      "propertyId": 1001,
      "propertyType": "APARTMENT",
      "name": "경희궁롯데캐슬",
      "address": "서울특별시 종로구 무악동",
      "lat": 37.5738636,
      "lng": 126.9594466,
      "latestTransaction": {
        "transactionType": "JEONSE",
        "dealAmount": 1080000000,
        "dealDate": "2026-06-08"
      },
      "dealCount": 18,
      "recentTransactionCount": 6,
      "aiAvailable": true
    }
  ],
  "administrativeClusters": [
    {
      "clusterId": "LEGAL_DONG:서울특별시:종로구:무악동",
      "level": "LEGAL_DONG",
      "sido": "서울특별시",
      "sigungu": "종로구",
      "legalDong": "무악동",
      "label": "무악동",
      "centerLat": 37.5738636,
      "centerLng": 126.9594466,
      "propertyCount": 12,
      "transactionCount": 31,
      "averageDealAmount": 1080000000
    }
  ],
  "bounds": {
    "swLat": 37.48,
    "swLng": 127.01,
    "neLat": 37.52,
    "neLng": 127.06
  },
  "filters": {
    "propertyTypes": ["APARTMENT"],
    "transactionTypes": ["SALE"],
    "zoomLevel": 5
  }
}
```

`administrativeClusters` is always present. It is empty for levels 1-4. For levels 5-6 it contains `LEGAL_DONG` clusters. For level 7 and above it contains `SIGUNGU` clusters.

The existing `items` field remains present for result lists and marker clustering. For this implementation, backend returns map-visible items for all levels. If performance later requires server-side item thinning, that must be a separate contract change.

## Backend Design

- Add `AdministrativeClusterResponse`.
- Add `administrativeClusters` to `PropertyMapResponse`.
- Add `recentTransactionCount` to `PropertyMapItemResponse`.
- Add `AdministrativeClusterRow`.
- Add mapper methods for:
  - legal-dong cluster aggregation for levels 5-6
  - sigungu cluster aggregation for levels 7+
- Reuse existing map bounds and filter SQL fragments where possible.
- Add recent 6-month transaction aggregation to cluster SQL and map item SQL.
- Keep monthly rent price averaging explicit: count matching recent transactions, but average only non-null `COALESCE(deal_amount_krw, deposit_amount_krw)` values.
- Service logic chooses administrative cluster level from `MapSearchRequest.zoomLevel()`.

## Frontend Design

- Extend frontend API types with `AdministrativeCluster` and `recentTransactionCount`.
- `MapView` continues to pass `items` to `KakaoMapPanel` and also passes `administrativeClusters`.
- `KakaoMapPanel` keeps one rendering path for individual property markers, one for Kakao MarkerClusterer, and one for administrative cluster overlays.
- `kakaoMap.ts` owns low-level marker and cluster synchronization helpers.
- At levels 1-3, sync individual property markers only.
- At level 4 and above, sync Kakao MarkerClusterer transaction-count clusters.
- At levels 5 and above, also sync administrative cluster markers.
- Kakao MarkerClusterer displays the sum of `recentTransactionCount` for the markers in each cluster. Kakao `setTexts` only receives marker count, so implementation uses the `clustered` event, `Cluster.getMarkers()`, and `Cluster.getClusterMarker()` to update the cluster overlay content after clustering.
- Administrative cluster labels use Korean display text:
  - legal dong: `{동명}`
  - sigungu: `{구명}`
  - average price: `평균 {formatKrw(value)}`
  - count: `거래 {n}건`
- If average price is null, show `평균 정보 없음` and still show the transaction count.

## Visual Treatment

- Administrative clusters use larger pill-style custom markers with area name, average price, and count.
- Kakao MarkerClusterer clusters use smaller circular or compact badge markers showing only recent 6-month transaction count.
- Selected individual property styling stays unchanged for levels 1-3.
- Marker layers should avoid covering the bottom map status label.

## Error Handling

- If cluster data is missing or empty, the map still renders the existing item list and available marker layer.
- If Kakao Maps fails to load, the existing missing-key and load-error behavior remains unchanged.
- If backend aggregation fails, `GET /properties/map` should fail consistently with the existing map search error handling rather than returning partial inconsistent data.

## Testing

Backend:

- Service test for level 4 returning no administrative clusters.
- Service test for level 5 returning legal-dong clusters.
- Service test for level 7 returning sigungu clusters.
- Mapper or SQL-oriented test covering recent 6-month transaction count and average price rules.
- Contract test that existing `items`, `bounds`, and `filters` fields still serialize.

Frontend:

- Unit test for map-level render mode selection.
- Unit test for administrative cluster marker text, including null average price.
- Unit test that MarkerClusterer remains active at levels 5-6 and 7+.
- Unit test that MarkerClusterer cluster text sums `recentTransactionCount` instead of raw marker count.
- Component test that `KakaoMapPanel` clears old individual, clusterer, and administrative markers when props change or unmount.

Manual verification:

- Run frontend tests and backend property tests.
- Start the app when local environment permits.
- Verify `/map` at levels 3, 4, 5, 6, and 7 with filters changed.

## Contract Changes

- `GET /api/v1/properties/map` item responses add `recentTransactionCount`.
- `GET /api/v1/properties/map` response adds `administrativeClusters`.
- Frontend consumes the new field but remains compatible with existing `items`.
- No database schema change is required.

## References

- Kakao Maps Web API MarkerClusterer documentation: https://apis.map.kakao.com/web/documentation/#MarkerClusterer

## Blockers

None.
