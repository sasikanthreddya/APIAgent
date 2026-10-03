Service Type: Users - own profile (GET /users/me)

Grounded 2026-09-23, re-verified live 2026-09-24 (twice - all three
scenarios below re-curled unchanged both times) against the running
toolshop-clone-api (localhost:8070), via live Swagger and direct curl
calls - not from the schema alone. Every request/expected-response pair
below was captured from a real call this run.

Scoped to the single endpoint that needs nothing but a bearer token; the
rest of the Users resource (list, get by id, update, delete - all admin- or
ownership-gated) is a separate flow to be reviewed once this one is
generated, matching the clone's own docs/TESTING-ORDER.md phase split
(own-profile check in Phase 1, full user administration in Phase 6).

Response contract (UserResponse): same shape as flows/auth.md's register
response - id, first_name, last_name, email, role, phone, dob, address,
enabled, created_at.

## Background
(none)

## Scenario: A logged-in user can fetch their own profile  [smoke]
1) Register a unique user (track for cleanup) and log in as them
2) GET /users/me with that user's token
3) Response status is 200
4) Response body's id, email, first_name and last_name match the registration

Request:
```json
{
  "method": "GET",
  "path": "/users/me",
  "auth": "token:<the newly registered user's own token>",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "id": "8cf30f55-c7b2-4e36-9250-ac1e17e2c657",
    "first_name": "Users",
    "last_name": "Flow",
    "email": "usersflow.1790197982985695400@toolshop.test",
    "role": "CUSTOMER",
    "phone": null,
    "dob": null,
    "address": {
      "street": null,
      "house_number": null,
      "city": null,
      "state": null,
      "country": null,
      "postal_code": null
    },
    "enabled": true,
    "created_at": "2026-09-23T21:13:03.120160566"
  },
  "dynamic_fields": ["id", "email", "created_at"],
  "notes": "Re-captured live 2026-09-24 (second pass) using a freshly registered throwaway user (deleted after capture), matching what the generated scenario itself will do. id/email/created_at vary per run - assert id/email/first_name/last_name equal what the registration call returned, not this exact body."
}
```

## Scenario: Fetching the profile without a token is rejected
1) GET /users/me with no Authorization header
2) Response status is 401
3) Response body's message says a valid Bearer token is required

Request:
```json
{
  "method": "GET",
  "path": "/users/me",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "error": "Unauthorized",
    "status": 401,
    "message": "A valid Bearer token is required for GET /users/me"
  },
  "dynamic_fields": [],
  "notes": "Re-verified live 2026-09-24 (second pass), unchanged. Message names the exact method+path - assert it contains 'Bearer token is required', not the full sentence, in case that detail changes."
}
```

## Scenario: Fetching the profile with a malformed token is rejected
1) GET /users/me with an Authorization header that is not a real JWT
2) Response status is 401

Request:
```json
{
  "method": "GET",
  "path": "/users/me",
  "auth": "raw-header:Authorization=Bearer not.a.jwt",
  "body": null
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "error": "Unauthorized",
    "status": 401,
    "message": "A valid Bearer token is required for GET /users/me"
  },
  "dynamic_fields": [],
  "notes": "Re-verified live 2026-09-24 (second pass), unchanged. Identical response to the no-token case - a garbage bearer value is treated the same as no header at all, not a distinct 'invalid token' error. Worth calling out explicitly since it's easy to assume otherwise."
}
```
