Service Type: Auth (POST /users/register, POST /users/login)

Re-grounded 2026-09-24 (originally 2026-09-23) against the running
toolshop-clone-api (localhost:8070), via its live Swagger (`/v3/api-docs`)
and direct curl calls - not from the OpenAPI schema alone, since the
schema's response section only lists "200" for every operation (springdoc
default) while the live API actually answers 201 for a successful
register. Every request/expected-response pair below was captured from a
real call this run; behaviour is unchanged from the 2026-09-23 grounding
that this resource was originally generated from (`auth.feature`,
`AuthSteps.java`, `AuthClient.java` already exist and pass against this
same contract - this re-draft doesn't change anything about them).

Request contract (RegisterRequest, from the live schema):
  first_name  string, max 40   - required (blank -> 422)
  last_name   string, max 20   - required (blank -> 422)
  email       string, max 256  - required, must be a well-formed email address
  password    string, min 8    - required
  phone       string, max 24   - optional
  dob         date (yyyy-mm-dd) - optional
  address     object (street, house_number, city, state, country, postal_code) - optional, all sub-fields optional

Request contract (LoginRequest):
  email, password - both required (blank -> 422)

Response contract (UserResponse, returned by register):
  id, first_name, last_name, email, role, phone, dob, address, enabled, created_at
  role is always "CUSTOMER" for a self-registration (there is no way to
  register as admin - admin is a seeded account only).
  Any optional field not sent comes back null (phone, dob, and every address
  sub-field), not omitted.

Response contract (LoginResponse):
  access_token (JWT, 3 dot-separated segments, HS384), token_type ("bearer"),
  expires_in (seconds, 3600 observed)

## Background
(none - both endpoints are open, no token needed to call them)

## Scenario: Register with a full profile  [smoke]
1) POST /users/register with first_name, last_name, a unique email, password,
   phone, dob and a full address
2) Response status is 201
3) Response body's role is "CUSTOMER", email matches the request, and every
   submitted field (first_name, last_name, phone, dob, address.*) is echoed
   exactly
4) Track the created user id for cleanup

Request:
```json
{
  "method": "POST",
  "path": "/users/register",
  "auth": "none",
  "body": {
    "first_name": "Jane",
    "last_name": "Doe",
    "email": "jane.doe.<unique>@toolshop.test",
    "password": "Password123!",
    "phone": "0612345678",
    "dob": "1990-05-15",
    "address": {
      "street": "12 Main Street",
      "house_number": "12",
      "city": "Amsterdam",
      "state": "NH",
      "country": "Netherlands",
      "postal_code": "1011AB"
    }
  }
}
```

Expected response:
```json
{
  "status": 201,
  "body": {
    "id": "551ed197-daee-429e-a906-e6a0e30d523d",
    "first_name": "Jane",
    "last_name": "Doe",
    "email": "jane.doe.<unique>@toolshop.test",
    "role": "CUSTOMER",
    "phone": "0612345678",
    "dob": "1990-05-15",
    "address": {
      "street": "12 Main Street",
      "house_number": "12",
      "city": "Amsterdam",
      "state": "NH",
      "country": "Netherlands",
      "postal_code": "1011AB"
    },
    "enabled": true,
    "created_at": "2026-09-23T20:57:57.955217386"
  },
  "dynamic_fields": ["id", "email", "created_at"],
  "notes": "Re-captured live 2026-09-24 (throwaway user deleted after capture). id/created_at vary per run; email varies per run since the scenario itself generates a unique one. Every other submitted field is echoed verbatim."
}
```

## Scenario: Register with only the required fields  [smoke]
1) POST /users/register with only first_name, last_name, a unique email and
   password
2) Response status is 201
3) Response body's role is "CUSTOMER", email matches the request
4) phone, dob and every address sub-field are null
5) Track the created user id for cleanup

Request:
```json
{
  "method": "POST",
  "path": "/users/register",
  "auth": "none",
  "body": {
    "first_name": "Min",
    "last_name": "Imal",
    "email": "min.<unique>@toolshop.test",
    "password": "Password123!"
  }
}
```

Expected response:
```json
{
  "status": 201,
  "body": {
    "id": "b5cae1e5-b58e-497d-8ca0-752c4b9fd457",
    "first_name": "Min",
    "last_name": "Imal",
    "email": "min.<unique>@toolshop.test",
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
    "created_at": "2026-09-23T20:57:58.211388366"
  },
  "dynamic_fields": ["id", "email", "created_at"],
  "notes": "Re-captured live 2026-09-24. Confirms every optional field is nullable and comes back as null (not omitted) when not sent, including every address sub-field individually - address itself is never null."
}
```

## Scenario: Registering with an already-used email is rejected
1) Register a unique user (track for cleanup)
2) POST /users/register again with that same email (any other valid values)
3) Response status is 409
4) Response body's message names the conflicting email

Request:
```json
{
  "method": "POST",
  "path": "/users/register",
  "auth": "none",
  "body": {
    "first_name": "Jane",
    "last_name": "Doe",
    "email": "<same email as the scenario's own step 1 registration>",
    "password": "Password123!"
  }
}
```

Expected response:
```json
{
  "status": 409,
  "body": {
    "timestamp": "2026-09-23T20:57:58.377525426",
    "status": 409,
    "error": "Conflict",
    "message": "An account with email jane.doe.1790197077722865400@toolshop.test already exists"
  },
  "dynamic_fields": ["timestamp", "message"],
  "notes": "Re-captured live 2026-09-24. message's exact text includes the email, so the assertion should check it CONTAINS 'already exists' plus the submitted email, not an exact string match."
}
```

## Scenario: Registering without the required fields is rejected
1) POST /users/register with only an email (no first_name, last_name, password)
2) Response status is 422
3) Response body's message lists each missing field (first_name, last_name, password)

Request:
```json
{
  "method": "POST",
  "path": "/users/register",
  "auth": "none",
  "body": {
    "email": "x.<unique>@toolshop.test"
  }
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-23T20:57:58.514904044",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "first_name: must not be blank; last_name: must not be blank; password: must not be blank"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Re-captured live 2026-09-24, unchanged. message is a semicolon-joined list of field violations - order matched this run but field validation order is not a documented contract, so the assertion should check each expected fragment is PRESENT, not the whole string or its order."
}
```

## Scenario: Registering with a password shorter than 8 characters is rejected
1) POST /users/register with a valid first_name/last_name/email and a
   7-character password
2) Response status is 422
3) Response body's message names the password size rule

Request:
```json
{
  "method": "POST",
  "path": "/users/register",
  "auth": "none",
  "body": {
    "first_name": "A",
    "last_name": "B",
    "email": "short.<unique>@toolshop.test",
    "password": "short"
  }
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-23T20:57:58.637357024",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "password: size must be between 8 and 2147483647"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Re-captured live 2026-09-24, unchanged. The upper bound (2147483647 = Integer.MAX_VALUE) is an artifact of @Size(min = 8) with no max set - assert the message contains 'password' and 'must be between 8', not the full number."
}
```

## Scenario: Registering with a malformed email is rejected
1) POST /users/register with first_name/last_name/password valid and email "not-an-email"
2) Response status is 422
3) Response body's message says the email must be well-formed

Request:
```json
{
  "method": "POST",
  "path": "/users/register",
  "auth": "none",
  "body": {
    "first_name": "A",
    "last_name": "B",
    "email": "not-an-email",
    "password": "Password123!"
  }
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-23T20:57:58.717445688",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "email: must be a well-formed email address"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Re-captured live 2026-09-24, unchanged."
}
```

## Scenario: A newly registered user can log in  [smoke]
1) Register a unique user (track for cleanup)
2) POST /users/login with that user's email and password
3) Response status is 200
4) Response body has an access_token with 3 dot-separated segments, token_type "bearer", expires_in above 0

Request:
```json
{
  "method": "POST",
  "path": "/users/login",
  "auth": "none",
  "body": {
    "email": "<the email registered in this scenario's own step 1>",
    "password": "Password123!"
  }
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "access_token": "<JWT, header {\"alg\":\"HS384\"}, payload {sub, email, role, iat, exp}>",
    "token_type": "bearer",
    "expires_in": 3600
  },
  "dynamic_fields": ["access_token"],
  "notes": "Re-captured live 2026-09-24, unchanged. access_token decodes (base64url, no signature check needed) to header {alg: HS384} and a payload carrying sub (user id), email, role, iat, exp. Assert 3 dot-separated segments, token_type == 'bearer', expires_in > 0 - not the token's exact value."
}
```

## Scenario: Login with the wrong password is rejected
1) Register a unique user (track for cleanup)
2) POST /users/login with that user's email and an incorrect password
3) Response status is 401
4) Response body's message is a generic "Invalid email or password" (does not
   reveal whether the email exists)

Request:
```json
{
  "method": "POST",
  "path": "/users/login",
  "auth": "none",
  "body": {
    "email": "<a registered email>",
    "password": "WrongPass1!"
  }
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "timestamp": "2026-09-23T20:57:59.112673790",
    "error": "Unauthorized",
    "status": 401,
    "message": "Invalid email or password"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Re-captured live 2026-09-24 - corrects the original 2026-09-23 capture, which claimed no timestamp field on this shape. It's present and consistent (re-verified 3x that same session). Assert error/status/message for exact match, timestamp for shape only."
}
```

## Scenario: Login with an email that was never registered is rejected
1) POST /users/login with an email that has never been registered
2) Response status is 401
3) Response body's message is the same generic "Invalid email or password"

Request:
```json
{
  "method": "POST",
  "path": "/users/login",
  "auth": "none",
  "body": {
    "email": "nobody.<unique>@toolshop.test",
    "password": "Password123!"
  }
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "timestamp": "2026-09-23T20:57:59.250781289",
    "error": "Unauthorized",
    "status": 401,
    "message": "Invalid email or password"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Re-captured live 2026-09-24. Identical message to the wrong-password case - the API deliberately does not reveal whether the email exists. Worth asserting that both cases produce the exact same message, as a security-relevant behaviour, not just the same status code."
}
```

## Scenario: Login without a password is rejected
1) POST /users/login with only an email
2) Response status is 422

Request:
```json
{
  "method": "POST",
  "path": "/users/login",
  "auth": "none",
  "body": {
    "email": "<any registered email>"
  }
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-23T20:57:59.323335843",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "password: must not be blank"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Re-captured live 2026-09-24, unchanged."
}
```
