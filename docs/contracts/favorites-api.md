# Favorites API Contract Draft

## Scope

Favorites APIs support logged-in users saving apartment complexes and areas for later review. Favorites are a personal convenience feature and must not be framed as investment advice or a buy/sell recommendation.

Base path: `/api/v1`

All favorites APIs require authentication with role `USER` or `ADMIN`. Unauthenticated requests return `AUTH_REQUIRED`.

## Favorite Apartment APIs

### List Favorite Apartments

`GET /api/v1/favorites/apartments`

Draft response:

```json
{
  "items": [
    {
      "favoriteId": 501,
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
      "createdAt": "2026-06-12T10:30:00+09:00"
    }
  ]
}
```

### Add Favorite Apartment

`POST /api/v1/favorites/apartments`

Draft request:

```json
{
  "propertyId": 1001
}
```

Draft response:

```json
{
  "favoriteId": 501,
  "propertyId": 1001,
  "createdAt": "2026-06-12T10:30:00+09:00",
  "message": "관심 아파트에 추가했습니다."
}
```

Rules:

- Only apartment properties can be added to the favorite apartment list during MVP.
- Adding the same apartment twice returns `FAVORITE_ALREADY_EXISTS`.
- A missing property returns `PROPERTY_NOT_FOUND`.

### Remove Favorite Apartment

`DELETE /api/v1/favorites/apartments/{propertyId}`

Draft response:

```json
{
  "propertyId": 1001,
  "message": "관심 아파트에서 삭제했습니다."
}
```

Rules:

- Deleting a non-existing favorite returns `FAVORITE_NOT_FOUND`.
- Users can delete only their own favorites.

## Favorite Area APIs

Favorite areas allow users to save administrative areas or map-centered areas.

### List Favorite Areas

`GET /api/v1/favorites/areas`

Draft response:

```json
{
  "items": [
    {
      "favoriteAreaId": 801,
      "label": "강남구 역삼동",
      "sido": "서울특별시",
      "sigungu": "강남구",
      "legalDong": "역삼동",
      "centerLat": 37.5001,
      "centerLng": 127.0364,
      "zoomLevel": 5,
      "createdAt": "2026-06-12T10:30:00+09:00"
    }
  ]
}
```

### Add Favorite Area

`POST /api/v1/favorites/areas`

Draft request:

```json
{
  "label": "강남구 역삼동",
  "sido": "서울특별시",
  "sigungu": "강남구",
  "legalDong": "역삼동",
  "centerLat": 37.5001,
  "centerLng": 127.0364,
  "zoomLevel": 5
}
```

Draft response:

```json
{
  "favoriteAreaId": 801,
  "label": "강남구 역삼동",
  "createdAt": "2026-06-12T10:30:00+09:00",
  "message": "관심 지역에 추가했습니다."
}
```

Rules:

- `label` is user-facing Korean text and should be natural.
- At least one of administrative area fields or center coordinates must be present.
- If coordinates are provided, both `centerLat` and `centerLng` are required.
- Adding the same normalized area twice returns `FAVORITE_AREA_ALREADY_EXISTS`.

### Remove Favorite Area

`DELETE /api/v1/favorites/areas/{favoriteAreaId}`

Draft response:

```json
{
  "favoriteAreaId": 801,
  "message": "관심 지역에서 삭제했습니다."
}
```

Rules:

- Deleting a non-existing favorite area returns `FAVORITE_AREA_NOT_FOUND`.
- Users can delete only their own favorite areas.

## Error Responses

All errors use the shared shape in `docs/contracts/error-response.md`.

| HTTP status | Code | Usage |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | Invalid property ID, area payload, coordinate pair, or enum value. |
| 401 | `AUTH_REQUIRED` | Login is required. |
| 403 | `ACCESS_DENIED` | User is trying to access another user's favorite. |
| 404 | `PROPERTY_NOT_FOUND` | Target apartment does not exist. |
| 404 | `FAVORITE_NOT_FOUND` | Favorite apartment does not exist for the current user. |
| 404 | `FAVORITE_AREA_NOT_FOUND` | Favorite area does not exist for the current user. |
| 409 | `FAVORITE_ALREADY_EXISTS` | Favorite apartment already exists. |
| 409 | `FAVORITE_AREA_ALREADY_EXISTS` | Favorite area already exists. |

## Handoff Impact

- Backend API Agent owns controllers, services, mappers, and DB schema for these endpoints.
- Auth / Security Agent must protect all endpoints with `USER` or `ADMIN`.
- Frontend / Map Agent should map API messages to polished Korean UI copy and should not describe favorites as recommendations.
- QA / Review Agent should test ownership isolation between users.
