# Notices API Contract Draft

## Scope

Notice APIs support public announcement reading and `ADMIN`-only notice mutation for the MVP. Notices are operational content, not investment advice.

Base path: `/api/v1`

## Public Notice APIs

### List Notices

`GET /api/v1/notices`

Query parameters:

| Name | Required | Description |
| --- | --- | --- |
| `page` | no | Zero-based page number. Defaults to `0`. |
| `size` | no | Page size. Defaults to backend policy. |
| `sort` | no | `publishedAt,desc` or `createdAt,desc`. |
| `keyword` | no | Title/content search keyword. |
| `pinnedOnly` | no | If `true`, return only pinned notices. |

Draft response:

```json
{
  "items": [
    {
      "noticeId": 301,
      "title": "서비스 점검 안내",
      "summary": "서비스 점검 일정을 안내드립니다.",
      "pinned": true,
      "publishedAt": "2026-06-12T10:30:00+09:00",
      "createdAt": "2026-06-12T09:00:00+09:00"
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

### Get Notice Detail

`GET /api/v1/notices/{noticeId}`

Draft response:

```json
{
  "noticeId": 301,
  "title": "서비스 점검 안내",
  "content": "서비스 점검 일정을 안내드립니다.",
  "pinned": true,
  "publishedAt": "2026-06-12T10:30:00+09:00",
  "createdAt": "2026-06-12T09:00:00+09:00",
  "updatedAt": "2026-06-12T09:30:00+09:00"
}
```

Rules:

- Public notice reads do not require login.
- The frontend must render empty states and errors in natural Korean.
- Notice content must not include investment advice, buy/sell recommendations, or guaranteed return language.

## Admin Notice APIs

All admin notice APIs require authentication and role `ADMIN`.

### List Notices For Admin

`GET /api/v1/admin/notices`

Query parameters are the same as `GET /api/v1/notices`. Unlike the public list, this endpoint returns scheduled notices whose `publishedAt` is in the future. Soft-deleted notices are excluded.

Response shape is the same as the public notice list.

### Get Notice Detail For Admin

`GET /api/v1/admin/notices/{noticeId}`

Response shape is the same as the public notice detail. Unlike the public detail endpoint, this endpoint can return scheduled notices whose `publishedAt` is in the future.

### Create Notice

`POST /api/v1/admin/notices`

Draft request:

```json
{
  "title": "서비스 점검 안내",
  "content": "서비스 점검 일정을 안내드립니다.",
  "pinned": true,
  "publishedAt": "2026-06-12T10:30:00+09:00"
}
```

Draft response:

```json
{
  "noticeId": 301,
  "message": "공지사항을 등록했습니다."
}
```

### Update Notice

`PUT /api/v1/admin/notices/{noticeId}`

Draft request:

```json
{
  "title": "서비스 점검 일정 변경 안내",
  "content": "서비스 점검 일정이 변경되었습니다.",
  "pinned": true,
  "publishedAt": "2026-06-12T11:00:00+09:00"
}
```

Draft response:

```json
{
  "noticeId": 301,
  "message": "공지사항을 수정했습니다."
}
```

### Delete Notice

`DELETE /api/v1/admin/notices/{noticeId}`

Draft response:

```json
{
  "noticeId": 301,
  "message": "공지사항을 삭제했습니다."
}
```

## Permission Failure Responses

All errors use the shared shape in `docs/contracts/error-response.md`.

Unauthenticated admin mutation:

```json
{
  "code": "AUTH_REQUIRED",
  "message": "로그인이 필요합니다.",
  "details": [],
  "path": "/api/v1/admin/notices",
  "timestamp": "2026-06-12T10:30:00+09:00"
}
```

Authenticated non-admin mutation:

```json
{
  "code": "ACCESS_DENIED",
  "message": "관리자 권한이 필요합니다.",
  "details": [],
  "path": "/api/v1/admin/notices",
  "timestamp": "2026-06-12T10:30:00+09:00"
}
```

Error table:

| HTTP status | Code | Usage |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | Title, content, date, or pinned payload is invalid. |
| 401 | `AUTH_REQUIRED` | Login is required for admin mutation. |
| 403 | `ACCESS_DENIED` | `ADMIN` role is required. |
| 404 | `NOTICE_NOT_FOUND` | Notice does not exist. |

## Handoff Impact

- Backend API Agent owns notice read and admin mutation endpoints.
- Auth / Security Agent must protect `/api/v1/admin/notices/**` with `ADMIN`.
- Frontend / Map Agent owns public notice views and admin notice forms with Korean UI copy.
- QA / Review Agent should verify anonymous read, authenticated non-admin denial, and admin mutation success.
