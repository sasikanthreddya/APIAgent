Service Type: Brands (GET /brands, GET /brands/{id}, POST /brands,
PUT /brands/{id}, DELETE /brands/{id})

Grounded 2026-09-24 against the running toolshop-clone-api (localhost:8070), via
its live Swagger (`/v3/api-docs`) and direct curl calls - not from the OpenAPI
schema alone (the schema declares `BrandRequest` with **no required fields at
all** - not reliable; live validation requires `name`, same mismatch already
seen with Categories and Products). Every request/expected-response pair below
was captured from a real call this run. Same access-control matrix as
Products/Categories (verified independently here): writes are admin-only,
reads are public.

Request contract (BrandRequest, from the live schema - required-ness not
reliable, confirmed live instead):
  name  string, max 100  - required (blank -> 422); must also be unique -
                            a duplicate name -> 409, not a validation error

Response contract (BrandResponse): id, name, slug (auto-generated from name,
lowercase-hyphenated). No `parent_id` - brands are flat, unlike Categories.

`GET /brands` is **not paginated** - same as Categories, a plain JSON array
(confirmed live: 2 seeded brands, ForgeFlex Tools and MightyCraft Hardware).

## Background
Write scenarios need an admin token (`AuthClient.tokenFor(Role.ADMIN)`) or a
customer token (`Role.CUSTOMER`) for the negative role cases. Read scenarios
need nothing. The known-defect scenario additionally needs a real
`category_id` (via Products' already-generated lookup) to create a throwaway
product - Products is already a generated resource, so calling its client for
this one verification scenario is not "pre-building the next resource," it's
reusing what already exists, same as how Products itself reuses Categories/
Brands lookups.

## Scenario: List brands  [smoke]
1) GET /brands
2) Response status is 200
3) Response body is a plain JSON array (not a paginated wrapper) with at
   least the 2 seeded brands, each with id/name/slug

Request:
```json
{
  "method": "GET",
  "path": "/brands",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": [
    {"id": "31cda89a-6497-476a-9184-fc4d9fb940af", "name": "ForgeFlex Tools", "slug": "forgeflex-tools"},
    {"id": "73ef8721-bd85-400a-b338-7ce92c7174cf", "name": "MightyCraft Hardware", "slug": "mightycraft-hardware"}
  ],
  "dynamic_fields": ["*body-is-array-not-object*"],
  "notes": "Captured live 2026-09-24. Body is a raw array, not {data: [...]} - assert it's a non-empty JSON array (at least 2 elements) with every element having id/name/slug, not the exact contents (other scenarios in this flow create/delete their own temporary brands during the run)."
}
```

## Scenario: Get a brand by id  [smoke]
1) GET /brands/{an existing brand id}
2) Response status is 200
3) Response body's id matches the requested id

Request:
```json
{
  "method": "GET",
  "path": "/brands/{id}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {"id": "31cda89a-6497-476a-9184-fc4d9fb940af", "name": "ForgeFlex Tools", "slug": "forgeflex-tools"},
  "dynamic_fields": ["id", "name", "slug"],
  "notes": "Captured live 2026-09-24 against a seeded brand. Assert the response's id equals the requested id, not any specific brand's fields."
}
```

## Scenario: Getting a brand by a nonexistent id is rejected
1) GET /brands/{a well-formed but nonexistent id}
2) Response status is 404

Request:
```json
{
  "method": "GET",
  "path": "/brands/00000000-0000-0000-0000-000000000000",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T05:52:22.837636115",
    "status": 404,
    "error": "Not Found",
    "message": "Brand 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Create a brand as admin  [smoke]
1) POST /brands as admin with only a unique name
2) Response status is 201
3) Response body's name matches the request, slug is the lowercase-hyphenated name
4) Track the created brand id for cleanup

Request:
```json
{
  "method": "POST",
  "path": "/brands",
  "auth": "admin",
  "body": {"name": "Test Brand <unique>"}
}
```

Expected response:
```json
{
  "status": 201,
  "body": {
    "id": "a65e8d20-7ac2-4cf3-b837-d7c667f28067",
    "name": "Test Brand 1790229143312314300",
    "slug": "test-brand-1790229143312314300"
  },
  "dynamic_fields": ["id", "name", "slug"],
  "notes": "Captured live 2026-09-24 (brand deleted after capture). slug is auto-generated: lowercased, spaces to hyphens - assert it's derived from the submitted name, not this exact value."
}
```

## Scenario: Creating a brand without a token is rejected
1) POST /brands with no Authorization header (a unique name)
2) Response status is 401

Request:
```json
{
  "method": "POST",
  "path": "/brands",
  "auth": "none",
  "body": {"name": "Test Brand <unique>"}
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "error": "Unauthorized",
    "status": 401,
    "message": "A valid Bearer token is required for POST /brands"
  },
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24. Same access-control matrix as Products/Categories, verified independently here."
}
```

## Scenario: Creating a brand as a customer is rejected
1) POST /brands with a customer token (same body as above)
2) Response status is 403

Request:
```json
{
  "method": "POST",
  "path": "/brands",
  "auth": "customer",
  "body": {"name": "Test Brand <unique>"}
}
```

Expected response:
```json
{
  "status": 403,
  "body": {
    "error": "Forbidden",
    "status": 403,
    "message": "Your role does not permit POST /brands"
  },
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Creating a brand without a name is rejected
1) POST /brands as admin with an empty body
2) Response status is 422
3) Response body's message names the name rule

Request:
```json
{
  "method": "POST",
  "path": "/brands",
  "auth": "admin",
  "body": {}
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T05:52:23.624134200",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "name: must not be blank"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Despite the schema declaring no required fields, live validation requires name."
}
```

## Scenario: Creating a brand with a duplicate name is rejected
1) POST /brands as admin with the name of an existing seeded brand
   (e.g. "ForgeFlex Tools")
2) Response status is 409
3) Response body's message names the conflicting name

Request:
```json
{
  "method": "POST",
  "path": "/brands",
  "auth": "admin",
  "body": {"name": "ForgeFlex Tools"}
}
```

Expected response:
```json
{
  "status": 409,
  "body": {
    "timestamp": "2026-09-24T05:52:23.730721959",
    "status": 409,
    "error": "Conflict",
    "message": "A brand named 'ForgeFlex Tools' already exists"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Name uniqueness enforced globally - reuses an existing seeded name, so nothing needs tracking for this scenario."
}
```

## Scenario: Update a brand as admin  [smoke]
1) Create a brand as admin (track for cleanup)
2) PUT /brands/{id} as admin with a changed name
3) Response status is 200
4) Response body's name and slug reflect the change, id unchanged

Request:
```json
{
  "method": "PUT",
  "path": "/brands/{id}",
  "auth": "admin",
  "body": {"name": "Updated Test Brand"}
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "id": "a65e8d20-7ac2-4cf3-b837-d7c667f28067",
    "name": "Updated Test Brand",
    "slug": "updated-test-brand"
  },
  "dynamic_fields": ["id"],
  "notes": "Captured live 2026-09-24. name/slug reflect the update, same id as before."
}
```

## Scenario: Updating a brand without a token is rejected
1) Create a brand as admin (track for cleanup)
2) PUT /brands/{id} with no Authorization header
3) Response status is 401

Request:
```json
{
  "method": "PUT",
  "path": "/brands/{id}",
  "auth": "none",
  "body": {"name": "Updated"}
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "error": "Unauthorized",
    "status": 401,
    "message": "A valid Bearer token is required for PUT /brands/a65e8d20-7ac2-4cf3-b837-d7c667f28067"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24. message names the exact method+path including the id - assert it contains 'Bearer token is required'."
}
```

## Scenario: Updating a brand as a customer is rejected
1) Create a brand as admin (track for cleanup)
2) PUT /brands/{id} with a customer token
3) Response status is 403

Request:
```json
{
  "method": "PUT",
  "path": "/brands/{id}",
  "auth": "customer",
  "body": {"name": "Updated"}
}
```

Expected response:
```json
{
  "status": 403,
  "body": {
    "error": "Forbidden",
    "status": 403,
    "message": "Your role does not permit PUT /brands/a65e8d20-7ac2-4cf3-b837-d7c667f28067"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Updating a brand without a name is rejected
1) Create a brand as admin (track for cleanup)
2) PUT /brands/{id} as admin with an empty body
3) Response status is 422

Request:
```json
{
  "method": "PUT",
  "path": "/brands/{id}",
  "auth": "admin",
  "body": {}
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T05:52:38.220111508",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "name: must not be blank"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Same name-required rule applies to update, not just create."
}
```

## Scenario: Updating a nonexistent brand is rejected
1) PUT /brands/{a well-formed but nonexistent id} as admin
2) Response status is 404

Request:
```json
{
  "method": "PUT",
  "path": "/brands/00000000-0000-0000-0000-000000000000",
  "auth": "admin",
  "body": {"name": "X"}
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T05:52:38.279945153",
    "status": 404,
    "error": "Not Found",
    "message": "Brand 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Delete a brand as admin  [smoke]
1) Create a brand as admin
2) DELETE /brands/{id} as admin
3) Response status is 204
4) GET /brands/{id} afterward returns 404 (deletion IS the cleanup)

Request:
```json
{
  "method": "DELETE",
  "path": "/brands/{id}",
  "auth": "admin",
  "body": null
}
```

Expected response:
```json
{
  "status": 204,
  "body": null,
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24. Empty body, same convention as DELETE /categories/{id}, DELETE /products/{id}, DELETE /users/{id}."
}
```

## Scenario: Deleting a brand without a token is rejected
1) Create a brand as admin (track for cleanup, since this scenario doesn't
   delete it)
2) DELETE /brands/{id} with no Authorization header
3) Response status is 401

Request:
```json
{
  "method": "DELETE",
  "path": "/brands/{id}",
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
    "message": "A valid Bearer token is required for DELETE /brands/a65e8d20-7ac2-4cf3-b837-d7c667f28067"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Deleting a brand as a customer is rejected
1) Create a brand as admin (track for cleanup, since this scenario doesn't
   delete it)
2) DELETE /brands/{id} with a customer token
3) Response status is 403

Request:
```json
{
  "method": "DELETE",
  "path": "/brands/{id}",
  "auth": "customer",
  "body": null
}
```

Expected response:
```json
{
  "status": 403,
  "body": {
    "error": "Forbidden",
    "status": 403,
    "message": "Your role does not permit DELETE /brands/a65e8d20-7ac2-4cf3-b837-d7c667f28067"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Deleting a nonexistent brand is rejected
1) DELETE /brands/{a well-formed but nonexistent id} as admin
2) Response status is 404

Request:
```json
{
  "method": "DELETE",
  "path": "/brands/00000000-0000-0000-0000-000000000000",
  "auth": "admin",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T05:52:38.461694286",
    "status": 404,
    "error": "Not Found",
    "message": "Brand 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Deleting a brand referenced by a product [known-defect]
1) Create a brand as admin
2) Create a product as admin using that brand id and any existing category id
   (via Products' already-generated client)
3) DELETE /brands/{brand id} as admin
4) Response status **should be** 409 Conflict (a referenced brand shouldn't
   be deletable) - the API **currently returns 500** Internal Server Error
   instead, with a bare `{timestamp, status, error, path}` body (no
   `message` - an uncaught exception, not a handled error response)
5) Verify the brand was NOT actually deleted (still GETtable) despite the 500
   - no data corruption, just the wrong status code
6) Clean up: delete the product first, then the brand (deleting the brand
   succeeds normally once nothing references it)

Request:
```json
{
  "method": "DELETE",
  "path": "/brands/{id}",
  "auth": "admin",
  "body": null
}
```

Expected response (documented defect, verified live 2026-09-24):
```json
{
  "status": 500,
  "body": {
    "timestamp": "2026-09-24T05:52:57.449+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "path": "/brands/1a570dda-ce67-41aa-ae06-004b550d7a59"
  },
  "dynamic_fields": ["timestamp", "path"],
  "notes": "Verified live 2026-09-24, reproduced twice. Correct behavior would be 409 Conflict (same pattern as Auth's duplicate-email or this project's duplicate-name checks) - instead an unhandled DB foreign-key constraint violation surfaces as a raw 500 with no message field, unlike every other error shape in this codebase. Confirmed non-destructive: the brand still exists (GET returns 200) after the 500, and deleting it succeeds normally (204) once the referencing product is removed first. Use CommonSteps.theResponseStatusShouldBeKnownDefect(409, 500) - flag this @known-defect and re-check if the clone is ever updated; remove the defect handling if it starts correctly returning 409."
}
```
