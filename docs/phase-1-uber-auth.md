# Phase 1 — Uber Direct Authentication

## Overview
Implements OAuth 2.0 client credentials flow against Uber's auth endpoint.
Tokens are cached in memory and automatically refreshed before expiry.

## Flow
App startup / first API call
↓
UberAuthService.getAccessToken()
↓
Token cached and valid? → return cached token
↓ (if not)
POST https://auth.uber.com/oauth/v2/token
↓
Cache token + expiry timestamp
↓
Return access token
## Key Classes

| Class | Location | Responsibility |
|---|---|---|
| `UberProperties` | `config/` | Binds `uber.*` properties from config |
| `UberTokenResponse` | `dto/` | Maps Uber's token JSON response |
| `UberAuthService` | `service/` | Fetches, caches, refreshes access token |
| `UberAuthTestController` | `controller/` | Temporary test endpoint (remove before prod) |

## Token Caching Strategy
- Token is stored in memory on the service instance
- Refreshed automatically 60 seconds before expiry
- No persistence — app fetches a fresh token on restart
- Thread safety note: for production scale, consider `synchronized` or an atomic reference

## Environment Variables Required
| Variable | Description |
|---|---|
| `UBER_CLIENT_ID` | From Uber Developer Dashboard |
| `UBER_CLIENT_SECRET` | From Uber Developer Dashboard |

## Testing
```bash
GET http://localhost:8080/api/test/uber-token
```
Expected response:
```json
{ "status": "OK", "tokenLength": "...", "tokenPrefix": "..." }
```

## Security Notes
- The test endpoint exposes partial token info — remove `UberAuthTestController` before deploying to production
- Never log the full access token
- Secrets must live in `application-local.properties` or hosting platform env vars only