# Community API Contract

Base path: `/api/v1/community/posts`

## List Posts

`GET /api/v1/community/posts`

Query:

- `page`: zero-based page, default `0`
- `size`: page size, default `20`, max `100`
- `sort`: `createdAt,desc`, `viewCount,desc`, `commentCount,desc`
- `keyword`: title/content keyword
- `category`: `NOTICE`, `FREE`, `DEAL_REVIEW`, `QNA`

Public endpoint.
`NOTICE` posts are sorted before normal posts.

## Get Post

`GET /api/v1/community/posts/{postId}`

Public endpoint. Increments `viewCount` and returns comments as a parent/reply tree.

## Create Post

`POST /api/v1/community/posts`

Requires `USER` or `ADMIN`.
`NOTICE` category can be created or updated only by `ADMIN`.

```json
{
  "category": "FREE",
  "title": "게시글 제목",
  "content": "게시글 본문",
  "relatedPropertyId": 1001
}
```

## Create Comment

`POST /api/v1/community/posts/{postId}/comments`

Requires `USER` or `ADMIN`.

```json
{
  "parentCommentId": null,
  "content": "댓글 본문"
}
```

Replies support one level only. A reply cannot be used as another reply's parent.
