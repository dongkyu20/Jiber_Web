# Property API Contract Draft

## Scope

Property APIs support map search, filter search, property detail, public valuation, and public SHAP explanation surfaces for the MVP.

Base path: `/api/v1`

The frontend calls only Spring Boot public APIs. Spring Boot owns persistence, authorization checks, property eligibility checks, and internal model-server calls. The FastAPI model server remains internal and is documented in `docs/contracts/model-server.md`.

## Shared Enums

`propertyTypes` values:

- `APARTMENT`
- `OFFICETEL`
- `VILLA`
- `HOUSE`

`transactionTypes` values:

- `SALE`
- `JEONSE`
- `MONTHLY_RENT`

List query parameters use comma-separated values, for example `propertyTypes=APARTMENT,OFFICETEL`.

## Map Search

`GET /api/v1/properties/map`

Map search is optimized for marker and cluster rendering within the visible Kakao Maps bounds. It returns lightweight property summaries, per-property recent transaction counts for frontend MarkerClusterer badges, and administrative cluster summaries for wider zoom levels. It does not return full detail records.

Query parameters:

| Name | Required | Description |
| --- | --- | --- |
| `swLat` | yes | South-west latitude of the visible map bounds. |
| `swLng` | yes | South-west longitude of the visible map bounds. |
| `neLat` | yes | North-east latitude of the visible map bounds. |
| `neLng` | yes | North-east longitude of the visible map bounds. |
| `zoomLevel` | yes | Kakao Maps zoom level from the frontend. |
| `propertyTypes` | no | Comma-separated property type filters. Defaults to all supported types. |
| `transactionTypes` | no | Comma-separated transaction type filters. Defaults to all supported types. |
| `minDealAmount` | no | Minimum transaction amount in KRW. |
| `maxDealAmount` | no | Maximum transaction amount in KRW. |
| `minAreaM2` | no | Minimum exclusive area in square meters. |
| `maxAreaM2` | no | Maximum exclusive area in square meters. |
| `dealYearFrom` | no | Earliest transaction year. |
| `dealYearTo` | no | Latest transaction year. |

Validation rules:

- `swLat`, `neLat` must be between `-90` and `90`.
- `swLng`, `neLng` must be between `-180` and `180`.
- `swLat` must be lower than `neLat`.
- `swLng` must be lower than `neLng`.
- `zoomLevel` must be present and must match the supported frontend Kakao Maps zoom range.
- Unknown enum values return `VALIDATION_FAILED`.

Draft response:

```json
{
  "items": [
    {
      "propertyId": 1001,
      "propertyType": "APARTMENT",
      "name": "예시아파트",
      "address": "서울특별시 강남구 예시동 1",
      "lat": 37.5001,
      "lng": 127.0364,
      "latestTransaction": {
        "transactionType": "SALE",
        "dealAmount": 1250000000,
        "dealDate": "2026-05-20"
      },
      "dealCount": 18,
      "recentTransactionCount": 6,
      "aiAvailable": true
    }
  ],
  "administrativeClusters": [
    {
      "clusterId": "LEGAL_DONG:서울특별시:강남구:예시동",
      "level": "LEGAL_DONG",
      "sido": "서울특별시",
      "sigungu": "강남구",
      "legalDong": "예시동",
      "label": "예시동",
      "centerLat": 37.5008,
      "centerLng": 127.0366,
      "propertyCount": 12,
      "transactionCount": 34,
      "averageDealAmount": 1230000000
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

Cluster fields:

- `recentTransactionCount` counts transactions for the property in the most recent six months from the backend clock after applying the current transaction filters. The frontend uses this value in Kakao MarkerClusterer clusters from map level `4` upward.
- `administrativeClusters` contains only administrative regions visible in the requested map bounds and matching the same property and transaction filters.
- For `zoomLevel` below `5`, `administrativeClusters` is empty.
- For `zoomLevel` `5` and `6`, each administrative cluster represents one legal dong and uses `level: "LEGAL_DONG"`.
- For `zoomLevel` `7` and above, each administrative cluster represents one sigungu and uses `level: "SIGUNGU"`.
- `transactionCount` counts recent six-month transactions inside the administrative cluster after filters.
- `averageDealAmount` is the rounded average of recent priced transactions in KRW and may be `null` when no priced transaction exists.

## Filter Search

`GET /api/v1/properties/search`

Filter search supports list pages, sidebar result lists, and keyword-driven searches. It may be used with or without map bounds.

Query parameters:

| Name | Required | Description |
| --- | --- | --- |
| `sido` | no | Administrative province/city name, for example `서울특별시`. |
| `sigungu` | no | Administrative district name, for example `강남구`. |
| `legalDong` | no | Legal dong name, for example `역삼동`. |
| `complexName` | no | Apartment complex or property name keyword. |
| `keyword` | no | General search keyword across complex name, road address, and legal dong. |
| `centerLat` | no | Current map center latitude used for distance priority. |
| `centerLng` | no | Current map center longitude used for distance priority. |
| `swLat` | no | Optional map bounds south-west latitude. |
| `swLng` | no | Optional map bounds south-west longitude. |
| `neLat` | no | Optional map bounds north-east latitude. |
| `neLng` | no | Optional map bounds north-east longitude. |
| `propertyTypes` | no | Comma-separated property type filters. |
| `transactionTypes` | no | Comma-separated transaction type filters. |
| `minDealAmount` | no | Minimum transaction amount in KRW. |
| `maxDealAmount` | no | Maximum transaction amount in KRW. |
| `minAreaM2` | no | Minimum exclusive area in square meters. |
| `maxAreaM2` | no | Maximum exclusive area in square meters. |
| `dealYearFrom` | no | Earliest transaction year. |
| `dealYearTo` | no | Latest transaction year. |
| `page` | no | Zero-based page number. Defaults to `0`. |
| `size` | no | Page size. Defaults to backend policy. |
| `sort` | no | `relevance,desc`, `distance,asc`, `latestDealDate,desc`, `latestDealAmount,desc`, or `dealCount,desc`. |

Priority rules:

- If `complexName` is present, exact or prefix complex-name matches rank before address matches.
- If `sido`, `sigungu`, or `legalDong` are present, results outside the selected administrative area are excluded.
- If `centerLat` and `centerLng` are present, the backend may calculate `distanceM` and use it for `distance,asc` or relevance tie-breaking.
- If both bounds and center coordinates are present, bounds limit the result set and center coordinates affect ordering only.
- If no sort is provided, default ordering is `relevance,desc` when keyword-like fields are present, otherwise `latestDealDate,desc`.

Draft response:

```json
{
  "items": [
    {
      "propertyId": 1001,
      "propertyType": "APARTMENT",
      "name": "예시아파트",
      "address": "서울특별시 강남구 예시동 1",
      "legalDong": "예시동",
      "lat": 37.5001,
      "lng": 127.0364,
      "distanceM": 350,
      "latestTransaction": {
        "transactionType": "SALE",
        "dealAmount": 1250000000,
        "dealDate": "2026-05-20"
      },
      "aiAvailable": true
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## Property Detail

`GET /api/v1/properties/{propertyId}`

Property detail returns canonical property data, recent transactions, favorite summary, and AI availability metadata. It does not execute valuation or SHAP inference directly.

Draft response:

```json
{
  "propertyId": 1001,
  "propertyType": "APARTMENT",
  "name": "예시아파트",
  "address": {
    "sido": "서울특별시",
    "sigungu": "강남구",
    "legalDong": "예시동",
    "roadAddress": "서울특별시 강남구 예시로 1"
  },
  "location": {
    "lat": 37.5001,
    "lng": 127.0364
  },
  "summary": {
    "builtYear": 2010,
    "householdCount": 500,
    "latestDealAmount": 1250000000,
    "latestDealDate": "2026-05-20"
  },
  "transactions": [],
  "favorite": {
    "apartmentFavorited": false,
    "areaFavorited": false
  },
  "ai": {
    "valuationAvailable": true,
    "shapAvailable": true,
    "unsupportedReason": null
  }
}
```

## Public Valuation API

`POST /api/v1/properties/{propertyId}/valuation`

This externally exposed Spring backend API requires `USER` or `ADMIN` in the MVP. It performs eligibility checks and calls the internal model server only for supported apartment properties.

Draft request:

```json
{
  "exclusiveAreaM2": 84.95,
  "floor": 15,
  "asOfDate": "2026-06-12"
}
```

Draft response:

```json
{
  "propertyId": 1001,
  "supported": true,
  "estimatedPrice": 1230000000,
  "currency": "KRW",
  "predictionInterval": {
    "lower": 1150000000,
    "upper": 1310000000
  },
  "modelVersion": "hedonic-v1",
  "baselineDate": "2026-06-12",
  "featureSetVersion": "apartment-basic-v1",
  "message": "아파트 실거래 데이터를 바탕으로 계산한 추정가입니다."
}
```

Unsupported non-apartment properties return `VALUATION_UNSUPPORTED_PROPERTY_TYPE`.

## Public SHAP API

`POST /api/v1/properties/{propertyId}/shap`

This externally exposed Spring backend API requires `USER` or `ADMIN` in the MVP. It returns chart-ready explanation data for the frontend and must not expose internal file paths, raw model objects, or debugging traces.

Draft request:

```json
{
  "exclusiveAreaM2": 84.95,
  "floor": 15,
  "asOfDate": "2026-06-12"
}
```

Draft response:

```json
{
  "propertyId": 1001,
  "supported": true,
  "baseValue": 980000000,
  "prediction": 1230000000,
  "currency": "KRW",
  "values": [
    {
      "feature": "exclusiveAreaM2",
      "labelKo": "전용면적",
      "value": 84.95,
      "shapValue": 120000000,
      "direction": "UP"
    }
  ],
  "modelVersion": "hedonic-v1",
  "baselineDate": "2026-06-12",
  "featureSetVersion": "apartment-basic-v1",
  "message": "추정가에 영향을 준 주요 요인입니다."
}
```

Unsupported non-apartment properties return `VALUATION_UNSUPPORTED_PROPERTY_TYPE`.

## New Apartment Analysis API

`POST /api/v1/properties/new-analysis`

This externally exposed Spring backend API requires `USER` or `ADMIN`. It is for user-entered apartment candidates that do not yet exist as a persisted property row. The backend maps the submitted features to the existing internal model-server apartment valuation and SHAP contracts, and returns both outputs in one response. It does not persist the submitted candidate.

Draft request:

```json
{
  "propertyName": "래미안 삼성",
  "sido": "서울특별시",
  "sigungu": "강남구",
  "legalDong": "삼성동",
  "latitude": 37.5123,
  "longitude": 127.0567,
  "householdCount": 1200,
  "exclusiveAreaM2": 84.95,
  "floor": 15,
  "builtYear": 2010,
  "asOfDate": "2026-06-25",
  "distanceToStationM": null
}
```

Draft response:

```json
{
  "propertyName": "래미안 삼성",
  "valuation": {
    "propertyId": 0,
    "supported": true,
    "estimatedPrice": 1230000000,
    "currency": "KRW",
    "predictionInterval": {
      "lower": 1150000000,
      "upper": 1310000000
    },
    "modelVersion": "hedonic-v1",
    "baselineDate": "2026-06-12",
    "featureSetVersion": "apartment-basic-v1",
    "message": "아파트 실거래 데이터를 바탕으로 계산한 추정가입니다."
  },
  "shap": {
    "propertyId": 0,
    "supported": true,
    "baseValue": 980000000,
    "prediction": 1230000000,
    "currency": "KRW",
    "values": [],
    "modelVersion": "hedonic-v1",
    "baselineDate": "2026-06-12",
    "featureSetVersion": "apartment-basic-v1",
    "message": "추정가에 영향을 준 주요 요인입니다."
  },
  "message": "입력한 신규 아파트 조건으로 적정가와 주요 요인을 계산했습니다."
}
```

## Related Contracts

- Favorites are documented in `docs/contracts/favorites-api.md`.
- Notices and admin notice mutations are documented in `docs/contracts/notices-api.md`.
- Community posts and comments are documented in `docs/contracts/community-api.md`.
- Internal valuation and SHAP model-server contracts are documented in `docs/contracts/model-server.md`.
- Shared error response shape is documented in `docs/contracts/error-response.md`.

## Product Guardrails

- Detail and AI responses must not say whether the user should buy, sell, or invest.
- The frontend should describe valuation outputs as estimates and explanations, not decisions.
- User-facing fallback messages must be natural Korean.
