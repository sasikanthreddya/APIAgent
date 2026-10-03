Service Type: Categories (GET /categories, GET /categories/{id}, POST /categories,
PUT /categories/{id}, DELETE /categories/{id})

Grounded 2026-09-24 against the running toolshop-clone-api (localhost:8070), via
its live Swagger (`/v3/api-docs`) and direct curl calls - not from the OpenAPI
schema alone (the schema declares `CategoryRequest` with **no required fields at
all** - not reliable; live validation requires `name`). Every request/expected-
response pair below was captured from a real call this run. Same access-control
matrix as Products (verified independently here, not assumed from that flow):
writes are admin-only, reads are public.

Request contract (CategoryRequest, from the live schema - required-ness not
reliable, confirmed live instead):
  name       string, max 100  - required (blank -> 422)
  parent_id  string           - optional (root category if omitted); if
                                 provided must reference an existing category
                                 (unknown -> 404 "Parent category ... not found")
  name must also be unique - a duplicate name -> 409, not a validation error

Response contract (CategoryResponse): id, name, slug (auto-generated from name,
lowercase-hyphenated, e.g. "Test Root Cat" -> "test-root-cat"), parent_id
(null for a root category).

`GET /categories` is **not paginated** - unlike Products, it returns a plain
JSON array directly (confirmed live: 19 seeded categories, no `page`/`size`
params accepted, no wrapper object).

## Background
Write scenarios need an admin token (`AuthClient.tokenFor(Role.ADMIN)`) or a
customer token (`Role.CUSTOMER`) for the negative role cases. Read scenarios
need nothing. Category ids used across scenarios are looked up live via
`GET /categories` or from what the scenario itself created - never hardcoded
(rule 5).

## Scenario: List categories  [smoke]
1) GET /categories
2) Response status is 200
3) Response body is a plain JSON array (not a paginated wrapper) with at
   least the 19 seeded categories, each with id/name/slug

Request:
```json
{
  "method": "GET",
  "path": "/categories",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": [
    {"id": "1b958f6e-5fe6-4eb9-acb9-62558232ac5c", "name": "Chisels", "slug": "chisels", "parent_id": "d0abb6f9-0271-4be8-962e-a5374df18006"},
    {"id": "87e7b51a-e25a-41c9-8c92-bf0cbe6fc187", "name": "Power Tools", "slug": "power-tools", "parent_id": null}
  ],
  "dynamic_fields": ["*body-is-array-not-object*"],
  "notes": "Captured live 2026-09-24. Body is a raw array, not {data: [...]} - assert it's a non-empty JSON array (at least 19 elements) with every element having id/name/slug, not the exact contents (shared seed data, other scenarios in this same flow create/delete their own temporary categories during the run)."
}
```

## Scenario: Get a category by id  [smoke]
1) GET /categories/{an existing category id}
2) Response status is 200
3) Response body's id matches the requested id

Request:
```json
{
  "method": "GET",
  "path": "/categories/{id}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {"id": "3c015787-d3f4-4f5f-b59a-4e2df669cf2a", "name": "Hammer", "slug": "hammer", "parent_id": "d0abb6f9-0271-4be8-962e-a5374df18006"},
  "dynamic_fields": ["id", "name", "slug", "parent_id"],
  "notes": "Captured live 2026-09-24 against a seeded category. Assert the response's id equals the requested id, not any specific category's fields."
}
```

## Scenario: Getting a category by a nonexistent id is rejected
1) GET /categories/{a well-formed but nonexistent id}
2) Response status is 404

Request:
```json
{
  "method": "GET",
  "path": "/categories/00000000-0000-0000-0000-000000000000",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T05:24:09.089035915",
    "status": 404,
    "error": "Not Found",
    "message": "Category 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Create a root category as admin  [smoke]
1) POST /categories as admin with only a unique name (no parent_id)
2) Response status is 201
3) Response body's name matches the request, slug is the lowercase-hyphenated
   name, parent_id is null
4) Track the created category id for cleanup

Request:
```json
{
  "method": "POST",
  "path": "/categories",
  "auth": "admin",
  "body": {
    "name": "Test Root Cat <unique>"
  }
}
```

Expected response:
```json
{
  "status": 201,
  "body": {
    "id": "a11b793e-9777-4725-988c-e078c87586b9",
    "name": "Test Root Cat 1790227449279139100",
    "slug": "test-root-cat-1790227449279139100",
    "parent_id": null
  },
  "dynamic_fields": ["id", "name", "slug"],
  "notes": "Captured live 2026-09-24 (category deleted after capture). slug is auto-generated: lowercased, spaces to hyphens - assert it's derived from the submitted name, not this exact value."
}
```

## Scenario: Create a category with a parent as admin
1) Create a root category as admin (track for cleanup)
2) POST /categories as admin with a unique name and parent_id set to that
   root category's id
3) Response status is 201
4) Response body's parent_id matches the parent's id
5) Track the created category id for cleanup

Request:
```json
{
  "method": "POST",
  "path": "/categories",
  "auth": "admin",
  "body": {
    "name": "Temp Child <unique>",
    "parent_id": "<the parent category's id>"
  }
}
```

Expected response:
```json
{
  "status": 201,
  "body": {
    "id": "176e010d-86a8-483d-90c3-27131c5dbfac",
    "name": "Temp Child 1790227486485462200",
    "slug": "temp-child-1790227486485462200",
    "parent_id": "eb62db69-5fa9-4940-82aa-1abe9172ac16"
  },
  "dynamic_fields": ["id", "name", "slug", "parent_id"],
  "notes": "Captured live 2026-09-24 (both categories deleted after capture). parent_id in the response equals whatever parent_id was submitted."
}
```

## Scenario: Creating a category without a token is rejected
1) POST /categories with no Authorization header (a unique name, no parent_id)
2) Response status is 401

Request:
```json
{
  "method": "POST",
  "path": "/categories",
  "auth": "none",
  "body": {"name": "Test Cat <unique>"}
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "error": "Unauthorized",
    "status": 401,
    "message": "A valid Bearer token is required for POST /categories"
  },
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24. Same access-control matrix as Products, verified independently here."
}
```

## Scenario: Creating a category as a customer is rejected
1) POST /categories with a customer token (same body as above)
2) Response status is 403

Request:
```json
{
  "method": "POST",
  "path": "/categories",
  "auth": "customer",
  "body": {"name": "Test Cat <unique>"}
}
```

Expected response:
```json
{
  "status": 403,
  "body": {
    "error": "Forbidden",
    "status": 403,
    "message": "Your role does not permit POST /categories"
  },
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Creating a category without a name is rejected
1) POST /categories as admin with an empty body
2) Response status is 422
3) Response body's message names the name rule

Request:
```json
{
  "method": "POST",
  "path": "/categories",
  "auth": "admin",
  "body": {}
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T05:24:25.908776452",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "name: must not be blank"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Despite the schema declaring no required fields at all, live validation requires name - the exact kind of schema/reality mismatch already seen with Products (see flows/products.md)."
}
```

## Scenario: Creating a category with a nonexistent parent is rejected
1) POST /categories as admin with a unique name and a well-formed but
   nonexistent parent_id
2) Response status is 404
3) Response body's message names the missing parent id

Request:
```json
{
  "method": "POST",
  "path": "/categories",
  "auth": "admin",
  "body": {
    "name": "Bad Parent <unique>",
    "parent_id": "00000000-0000-0000-0000-000000000000"
  }
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T05:24:26.032804856",
    "status": 404,
    "error": "Not Found",
    "message": "Parent category 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Distinct message from the plain 'Category ... not found' used for direct id lookups - this one says 'Parent category'."
}
```

## Scenario: Creating a category with a duplicate name is rejected
1) POST /categories as admin with the name of an existing seeded category
   (e.g. "Hammer")
2) Response status is 409
3) Response body's message names the conflicting name

Request:
```json
{
  "method": "POST",
  "path": "/categories",
  "auth": "admin",
  "body": {"name": "Hammer"}
}
```

Expected response:
```json
{
  "status": 409,
  "body": {
    "timestamp": "2026-09-24T05:24:26.077397002",
    "status": 409,
    "error": "Conflict",
    "message": "A category named 'Hammer' already exists"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Name uniqueness is enforced globally (not just within a parent) - reuses an existing seeded name rather than creating one, so nothing needs tracking for this scenario."
}
```

## Scenario: Update a category as admin  [smoke]
1) Create a root category as admin (track for cleanup)
2) PUT /categories/{id} as admin with a changed name
3) Response status is 200
4) Response body's name and slug reflect the change, id unchanged

Request:
```json
{
  "method": "PUT",
  "path": "/categories/{id}",
  "auth": "admin",
  "body": {"name": "Updated Root Cat"}
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "id": "a11b793e-9777-4725-988c-e078c87586b9",
    "name": "Updated Root Cat",
    "slug": "updated-root-cat",
    "parent_id": null
  },
  "dynamic_fields": ["id"],
  "notes": "Captured live 2026-09-24. name/slug reflect the update, same id as before."
}
```

## Scenario: Updating a category without a token is rejected
1) Create a root category as admin (track for cleanup)
2) PUT /categories/{id} with no Authorization header
3) Response status is 401

Request:
```json
{
  "method": "PUT",
  "path": "/categories/{id}",
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
    "message": "A valid Bearer token is required for PUT /categories/a11b793e-9777-4725-988c-e078c87586b9"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24. message names the exact method+path including the id - assert it contains 'Bearer token is required'."
}
```

## Scenario: Updating a category as a customer is rejected
1) Create a root category as admin (track for cleanup)
2) PUT /categories/{id} with a customer token
3) Response status is 403

Request:
```json
{
  "method": "PUT",
  "path": "/categories/{id}",
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
    "message": "Your role does not permit PUT /categories/a11b793e-9777-4725-988c-e078c87586b9"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Updating a category without a name is rejected
1) Create a root category as admin (track for cleanup)
2) PUT /categories/{id} as admin with an empty body
3) Response status is 422

Request:
```json
{
  "method": "PUT",
  "path": "/categories/{id}",
  "auth": "admin",
  "body": {}
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T05:25:35.209163375",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "name: must not be blank"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Same name-required rule applies to update, not just create."
}
```

## Scenario: Updating a nonexistent category is rejected
1) PUT /categories/{a well-formed but nonexistent id} as admin
2) Response status is 404

Request:
```json
{
  "method": "PUT",
  "path": "/categories/00000000-0000-0000-0000-000000000000",
  "auth": "admin",
  "body": {"name": "X"}
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T05:24:26.266767218",
    "status": 404,
    "error": "Not Found",
    "message": "Category 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Delete a category as admin  [smoke]
1) Create a root category as admin
2) DELETE /categories/{id} as admin
3) Response status is 204
4) GET /categories/{id} afterward returns 404 (deletion IS the cleanup)

Request:
```json
{
  "method": "DELETE",
  "path": "/categories/{id}",
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
  "notes": "Captured live 2026-09-24. Empty body, same convention as DELETE /products/{id} and DELETE /users/{id}."
}
```

## Scenario: Deleting a category without a token is rejected
1) Create a root category as admin (track for cleanup, since this scenario
   doesn't delete it)
2) DELETE /categories/{id} with no Authorization header
3) Response status is 401

Request:
```json
{
  "method": "DELETE",
  "path": "/categories/{id}",
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
    "message": "A valid Bearer token is required for DELETE /categories/a11b793e-9777-4725-988c-e078c87586b9"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Deleting a category as a customer is rejected
1) Create a root category as admin (track for cleanup, since this scenario
   doesn't delete it)
2) DELETE /categories/{id} with a customer token
3) Response status is 403

Request:
```json
{
  "method": "DELETE",
  "path": "/categories/{id}",
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
    "message": "Your role does not permit DELETE /categories/a11b793e-9777-4725-988c-e078c87586b9"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Deleting a nonexistent category is rejected
1) DELETE /categories/{a well-formed but nonexistent id} as admin
2) Response status is 404

Request:
```json
{
  "method": "DELETE",
  "path": "/categories/00000000-0000-0000-0000-000000000000",
  "auth": "admin",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T05:24:26.446104725",
    "status": 404,
    "error": "Not Found",
    "message": "Category 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Deleting a category with a child leaves the child orphaned
1) Create a root category as admin (the parent)
2) Create a second category as admin with parent_id set to the first (the child, track for cleanup)
3) DELETE /categories/{parent id} as admin
4) Response status is 204 (delete succeeds even though a child references it -
   no foreign-key block, no cascade)
5) GET /categories/{child id} afterward still returns 200, with parent_id
   still pointing at the now-deleted parent id (dangling reference, not
   nulled out)

Request:
```json
{
  "method": "DELETE",
  "path": "/categories/{parentId}",
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
  "notes": "Captured live 2026-09-24 with a throwaway parent+child pair (child deleted after capture). This is real, verified behavior, not a defect flag - deleting a category is never blocked by existing children, and no cascade or null-out happens. Worth asserting explicitly since it's the kind of business rule easy to assume incorrectly (e.g. assuming a 409 conflict, as Auth's duplicate-email or this same flow's duplicate-name cases have)."
}
```
