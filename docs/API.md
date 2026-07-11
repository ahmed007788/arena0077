# Arena.ai API Reference

Complete API surface extracted from arena.ai's production JavaScript bundles.

## Authentication

Arena.ai uses **Supabase Auth** (project: `lmarena.supabase.co`).

### Sign Up (Anonymous)

```
POST /nextjs-api/sign-up
Content-Type: application/json

{
  "recaptchaToken": "<token or empty>",
  "provisionalUserId": "<uuid>"
}
```

Response: `ArenaUser` object.

### Sign In with Email

```
POST /nextjs-api/sign-in/email
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "yourpassword",
  "recaptchaToken": null
}
```

Response: `ArenaUser` object. Sets `arena-auth-prod-v1` cookie.

### Sign Out

```
POST /nextjs-api/sign-out
Authorization: Bearer <access_token>
```

### Get Current User

```
GET /api/me
Authorization: Bearer <access_token>
```

Response: `ArenaUser` object.

## Chat Streaming

All chat endpoints are **reCAPTCHA-protected**. They require either:
- `recaptchaV3Token` (invisible reCAPTCHA)
- `recaptchaV2Token` (challenge reCAPTCHA, used as fallback)

### Create New Conversation

```
POST /nextjs-api/stream/create-evaluation
Content-Type: application/json

{
  "modality": "chat" | "image" | "video" | "webdev",
  "mode": "battle" | "side" | "direct" | "agent",
  "prompt": "Hello, what is 2+2?",
  "title": "optional title",
  "modelAId": "optional uuid",
  "modelBId": "optional uuid",
  "files": ["file_id_1", "file_id_2"],
  "recaptchaV2Token": "...",
  "recaptchaV3Token": "...",
  "webhook": { "url": "...", "data": "..." },
  "secrets": { "a": [...], "b": [...] }
}
```

Response: Server-Sent Events stream.

#### SSE Event Types

| Type | Description |
|------|-------------|
| `conversation_created` | New conversation created |
| `message_start` | Stream started for a model |
| `text_delta` | Text chunk (incremental) |
| `message_complete` | Stream finished |
| `image_generated` | Image URL ready |
| `video_generated` | Video URL ready |
| `webdev_preview` | Web dev preview URL |
| `agent_step` | Agent mode step |
| `error` | Error event |

### Send Followup Message

```
POST /nextjs-api/stream/post-to-evaluation/{evaluationSessionId}
Content-Type: application/json

{
  "prompt": "Followup question",
  "files": [],
  "recaptchaV2Token": "...",
  "recaptchaV3Token": "...",
  "mode": null  // omitted for followups
}
```

### Stop Streaming

```
POST /nextjs-api/stream/stop/{evaluationSessionId}/messages/{messageId}
Content-Type: application/json

{ "stoppedAt": "2026-07-11T00:00:00Z" }
```

### Rerun Message

```
POST /nextjs-api/stream/rerun/{messageId}
```

### Resample

```
POST /nextjs-api/stream/resample/{evaluationSessionId}
```

### Skip Direct Battle

```
POST /nextjs-api/stream/skip-direct-battle/{evaluationSessionId}
```

### Retry Message

```
POST /nextjs-api/stream/retry-evaluation-session-message/{evaluationSessionId}/messages/{messageId}
```

### Resume WebDev

```
POST /nextjs-api/stream/resume-webdev/{evaluationSessionId}
```

### Resume Video Workflow

```
POST /nextjs-api/stream/resume-video-workflow/{evaluationSessionId}
```

## History

```
GET /api/history/unified?limit=20&includeArchived=false&cursor={cursor}
Authorization: Bearer <access_token>
```

Response:
```json
{
  "items": [
    {
      "id": "uuid",
      "title": "Conversation title",
      "modality": "chat",
      "mode": "battle",
      "createdAt": "2026-07-10T22:00:00Z",
      "updatedAt": "2026-07-10T22:30:00Z",
      "isArchived": false
    }
  ],
  "hasMore": false,
  "nextCursor": null
}
```

## Modality Auto-Detection

```
POST /nextjs-api/auto-modality
Content-Type: application/json

{ "prompt": "Generate an image of a cat" }
```

Response:
```json
{ "modality": "image", "confidence": 0.92 }
```

## Voting

```
POST /api/vote
Content-Type: application/json

{
  "value": "model_a" | "model_b" | "tie" | "bothbad",
  "messageAId": "uuid",
  "messageBId": "uuid",
  "evaluationSessionId": "uuid",
  "recaptchaV3Token": "...",
  "modelsRevealed": false,
  "didInspectBothMobileHorizontalResponses": false
}
```

## WebDev

### Get Stream Credentials

```
GET /api/evaluation/webdev/{id}/stream-credentials
Authorization: Bearer <access_token>
```

## Media Proxy

```
GET /nextjs-api/proxy/media?url={encoded_url}
```

## Factuality Verification

```
POST /nextjs-api/factuality/verify
```

## Authentication Cookie Format

The `arena-auth-prod-v1` cookie contains a base64-encoded JSON:

```json
{
  "access_token": "eyJhbGciOi...",
  "token_type": "bearer",
  "expires_in": 3600,
  "expires_at": 1783732154,
  "refresh_token": "opnnlpmbt2pv",
  "user": {
    "id": "cb3f7163-622e-4f8a-b9c3-84f0d6acc976",
    "aud": "authenticated",
    "role": "authenticated",
    "email": "user@example.com",
    "email_confirmed_at": "2026-01-19T09:24:11.978647Z",
    "confirmed_at": "2026-01-19T09:24:11.978647Z",
    "last_sign_in_at": "2026-07-11T00:09:14.78794Z",
    "app_metadata": {
      "provider": "email",
      "providers": ["email"]
    },
    "user_metadata": {
      "domain_url": "https://lmarena.ai",
      "email": "user@example.com",
      "email_verified": true,
      "full_name": "A",
      "id": "019bd58f-849d-75c2-a614-10b608b7d4b5",
      "phone_verified": false,
      "should_link_history": true,
      "signup_intent_id": "669cbb0b-31e3-41c9-a81c-1f30c4938c80",
      "sub": "cb3f7163-622e-4f8a-b9c3-84f0d6acc976"
    },
    "identities": [
      {
        "id": "fd3edd44-48ee-4ca5-b839-356771ba8866",
        "user_id": "cb3f7163-622e-4f8a-b9c3-84f0d6acc976",
        "identity_data": {
          "domain_url": "https://lmarena.ai",
          "email": "user@example.com",
          "email_verified": true,
          "full_name": "A",
          "id": "019bd58f-849d-75c2-a614-10b608b7d4b5"
        },
        "provider": "email",
        "last_sign_in_at": "2026-01-19T09:22:49.670529Z",
        "created_at": "2026-01-19T09:22:49.670573Z",
        "updated_at": "2026-01-19T09:22:49.670573Z"
      }
    ],
    "created_at": "2026-01-19T09:22:49.666901Z",
    "updated_at": "2026-07-11T00:09:14.82331Z",
    "is_anonymous": false
  },
  "weak_password": null
}
```

## reCAPTCHA Enterprise

| Type | Site Key | Usage |
|------|----------|-------|
| V3 (invisible) | `6LeTGMcsAAAAALuIlkVwIxaAuZA8VledA6d3Nnb0` | Primary, executes on every chat request |
| V2 (challenge) | `6Le3_cYsAAAAAGwWOK2RLDgNI15Bh8C0yLBOL1yL` | Fallback when V3 fails |

## URL Patterns

| Page | URL |
|------|-----|
| Home | `https://arena.ai/` |
| Conversation | `https://arena.ai/c/{evaluationSessionId}` |
| Leaderboard | `https://arena.ai/leaderboard` |
| Leaderboard (agent) | `https://arena.ai/leaderboard/agent` |
| Privacy Policy | `https://help.arena.ai/articles/3765052346-privacy-policy` |
| Terms of Use | `https://help.arena.ai/articles/5629909088-terms-of-use` |

## Error Codes

| Status | Error | Description |
|--------|-------|-------------|
| 401 | UnauthorizedError | Missing or invalid auth |
| 403 | ForbiddenError | General forbidden |
| 403 | RecaptchaValidationFailedError | reCAPTCHA validation failed |
| 404 | NotFoundError | Resource not found |
| 409 | ConflictError | Resource conflict |
| 429 | PromptFailedError | Rate limit exceeded |
| 500 | BasicError | Internal server error |
