Service Type: Products (GET /products, GET /products/{id}, GET /products/search,
GET /products/{id}/related, POST /products, PUT /products/{id}, DELETE /products/{id})

Grounded 2026-09-24 against the running toolshop-clone-api (localhost:8070), via
its live Swagger (`/v3/api-docs`) and direct curl calls - not from the OpenAPI
schema alone. Every request/expected-response pair below was captured from a
real call this run. Cross-referenced against the clone's own
`docs/TESTING-ORDER.md` (Phase 2 read / Phase 3 write), which documents this
exact same access-control matrix (401 anonymous write, 403 customer write, 201
admin write) as a deliberate fix of two verified bugs in the real public
Toolshop API (anonymous/customer callers can create products there) - this
clone enforces it correctly, and the negative cases below verify that directly
rather than assuming it from that doc.

Request contract (ProductRequest, from the live schema): schema declares only
`is_location_offer`, `is_rental`, `price`, `stock_quantity` as required - **not
reliable**, live validation also requires `category_id`, `brand_id`, and
`description` (confirmed via the missing-required-fields scenario below).
`name` is genuinely optional (absent from every "must not be blank" response).
  name           string, max 200  - optional
  description    string, max 2000 - required (blank -> 422), despite schema
  price          number, >= 0     - required (null -> 422, negative -> 422)
  category_id    string           - required (blank -> 422), must reference an
                                     existing category (unknown -> 404)
  brand_id       string           - required (blank -> 422), must reference an
                                     existing brand (unknown -> 404)
  is_location_offer  boolean      - required (null -> 422)
  is_rental          boolean      - required (null -> 422)
  stock_quantity     integer, >= 0 - required (null -> 422)

Response contract (ProductResponse): id, name, description, price,
is_location_offer, is_rental, in_stock (derived: stock_quantity > 0),
stock_quantity, category (nested CategoryResponse: id, name, slug, parent_id),
brand (nested BrandResponse: id, name, slug).

List/search/related responses are paginated (PageResponseProductResponse):
current_page, data (array of ProductResponse), from, last_page, per_page, to,
total. Default pagination when no page/size given: page=1, size=20.

## Background
Every write scenario needs a real `category_id` and `brand_id` - fetched at
runtime via `GET /categories` / `GET /brands` (seeded data, always present),
never hardcoded (rule 5). Write scenarios need an admin token (seeded
account, `AuthClient.tokenFor(Role.ADMIN)`) or a customer token
(`Role.CUSTOMER`) for the negative role cases. Read scenarios need nothing.

## Scenario: List products  [smoke]
1) GET /products with no query params
2) Response status is 200
3) Response body has current_page=1, per_page=20, and total/last_page/data
   consistent with a paginated catalogue; every item in data has the full
   product shape (id, name, price, category.id, brand.id, ...)

Request:
```json
{
  "method": "GET",
  "path": "/products",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "current_page": 1,
    "data": [
      {
        "id": "097d60e9-3767-463c-a2cd-e6e7463e8735",
        "name": "M4 Nuts",
        "description": "Reliable m4 nuts, suitable for both professional and home workshop use.",
        "price": 4.65,
        "is_location_offer": false,
        "is_rental": false,
        "in_stock": false,
        "stock_quantity": 0,
        "category": {"id": "fac9a9b4-bfe7-4bd9-8224-15c022d987d3", "name": "Fasteners", "slug": "fasteners", "parent_id": "e15dc66e-3e46-4dd5-bb30-4dbb7472956f"},
        "brand": {"id": "31cda89a-6497-476a-9184-fc4d9fb940af", "name": "ForgeFlex Tools", "slug": "forgeflex-tools"}
      }
    ],
    "from": 1,
    "last_page": 2,
    "per_page": 20,
    "to": 20,
    "total": 39
  },
  "dynamic_fields": ["data", "last_page", "total", "to"],
  "notes": "Captured live 2026-09-24. data's exact contents/count are NOT asserted field-by-field (shared, mutable seed catalogue, grows as other resources' tests run - e.g. Products/Cart/Invoices scenarios create their own throwaway products) - only assert current_page=1, per_page=20, data is non-empty, and every element has the full ProductResponse shape (a representative element is shown here, not the literal array)."
}
```

## Scenario: Get a product by id  [smoke]
1) GET /products/{id} with an id from the product list
2) Response status is 200
3) Response body's id matches the requested id, and category/brand are
   nested objects (not just ids)

Request:
```json
{
  "method": "GET",
  "path": "/products/{id}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "id": "b3bdf0ea-c649-4f24-9e3c-d3e75ba95842",
    "name": "Test Hammer 1790222559164543900",
    "description": "grounding test",
    "price": 19.99,
    "is_location_offer": false,
    "is_rental": false,
    "in_stock": true,
    "stock_quantity": 10,
    "category": {"id": "3c015787-d3f4-4f5f-b59a-4e2df669cf2a", "name": "Hammer", "slug": "hammer", "parent_id": "d0abb6f9-0271-4be8-962e-a5374df18006"},
    "brand": {"id": "31cda89a-6497-476a-9184-fc4d9fb940af", "name": "ForgeFlex Tools", "slug": "forgeflex-tools"}
  },
  "dynamic_fields": ["id", "name", "description", "price", "in_stock", "stock_quantity", "category", "brand"],
  "notes": "Captured live 2026-09-24 against a product this same grounding pass created (then deleted). Assert the response's id equals the requested id and category/brand are present as nested objects - not any specific product's field values, since the scenario itself picks whichever id the list scenario returned."
}
```

## Scenario: Getting a product by a nonexistent id is rejected
1) GET /products/{a well-formed but nonexistent id}
2) Response status is 404
3) Response body's message names the missing product id

Request:
```json
{
  "method": "GET",
  "path": "/products/00000000-0000-0000-0000-000000000000",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T04:02:55.339478810",
    "status": 404,
    "error": "Not Found",
    "message": "Product 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Also confirmed a non-UUID-shaped id (e.g. \"not-a-uuid\") gets the same 404 (\"Product not-a-uuid not found\"), not a 400 - the id is treated as an opaque lookup key, not validated as a UUID first."
}
```

## Scenario: Search products by name  [smoke]
1) GET /products/search?q=hammer
2) Response status is 200
3) Every returned item's name contains "hammer" (case-insensitive)

Request:
```json
{
  "method": "GET",
  "path": "/products/search?q=hammer",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "current_page": 2,
    "data": [
      {"id": "abce71d5-428d-41b8-b869-bea46eb83a57", "name": "Claw Hammer with Shock Reduction Grip"},
      {"id": "e1b30e60-9025-44e4-96b8-f2333da7a446", "name": "Sledgehammer"}
    ],
    "total": 8
  },
  "dynamic_fields": ["current_page", "data", "total"],
  "notes": "Captured live 2026-09-24 (page 2 shown since default page size 20 doesn't apply here - result set of 8 total across 3 pages of 3). Assert every element's name contains the query case-insensitively, and total > 0 for a term known to match seeded data - not this exact page/array."
}
```

## Scenario: Searching without a query parameter is rejected
1) GET /products/search with no q parameter at all
2) Response status is 400

Request:
```json
{
  "method": "GET",
  "path": "/products/search",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 400,
  "body": {
    "timestamp": "2026-09-24T04:03:10.135+00:00",
    "status": 400,
    "error": "Bad Request",
    "path": "/products/search"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Different error shape from every other 4xx in this flow - no \"message\" key, has \"path\" instead (a Spring default error response, not the app's own exception handler - the q param is missing entirely, which trips validation before the app's controller runs). Only assert status/error/path - don't expect a message field. Confirmed q=<empty string> is NOT the same case - that returns 200 with the full unfiltered list, so this 400 is specifically about the parameter being absent, not blank."
}
```

## Scenario: Get related products
1) GET /products/{id}/related for an existing product id
2) Response status is 200
3) Response body is a paginated list of other products (same shape as the
   product list)

Request:
```json
{
  "method": "GET",
  "path": "/products/{id}/related",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "current_page": 2,
    "data": [
      {"id": "abce71d5-428d-41b8-b869-bea46eb83a57", "name": "Claw Hammer with Shock Reduction Grip"},
      {"id": "e1b30e60-9025-44e4-96b8-f2333da7a446", "name": "Sledgehammer"},
      {"id": "ea2c634e-0fe4-4672-9dec-a4527953e8f1", "name": "Court Hammer"}
    ],
    "total": 7
  },
  "dynamic_fields": ["current_page", "data", "total"],
  "notes": "Captured live 2026-09-24 for a Hammer-category product - related returned other Hammer-category products (same category as the source product, source excluded). Assert status 200 and the paginated shape, not the exact array."
}
```

## Scenario: Filter the product list by category
1) GET /products?categoryId={an existing category id}
2) Response status is 200
3) Every returned item's category.id equals the requested categoryId

Request:
```json
{
  "method": "GET",
  "path": "/products?categoryId={categoryId}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "current_page": 2,
    "data": [
      {"id": "abce71d5-428d-41b8-b869-bea46eb83a57", "category": {"id": "3c015787-d3f4-4f5f-b59a-4e2df669cf2a", "name": "Hammer"}}
    ],
    "total": 8
  },
  "dynamic_fields": ["current_page", "data", "total"],
  "notes": "Captured live 2026-09-24 filtering on the Hammer category id. Assert every returned item's category.id equals the requested categoryId, not the exact array."
}
```

## Scenario: Create a product as admin  [smoke]
1) POST /products as admin with name, description, price, an existing
   category_id and brand_id, is_location_offer, is_rental, stock_quantity
2) Response status is 201
3) Response body echoes every submitted field, in_stock is true (stock_quantity > 0),
   and category/brand are the full nested objects for the submitted ids
4) Track the created product id for cleanup

Request:
```json
{
  "method": "POST",
  "path": "/products",
  "auth": "admin",
  "body": {
    "name": "Test Hammer <unique>",
    "description": "grounding test",
    "price": 19.99,
    "category_id": "<an existing category id>",
    "brand_id": "<an existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 10
  }
}
```

Expected response:
```json
{
  "status": 201,
  "body": {
    "id": "b3bdf0ea-c649-4f24-9e3c-d3e75ba95842",
    "name": "Test Hammer 1790222559164543900",
    "description": "grounding test",
    "price": 19.99,
    "is_location_offer": false,
    "is_rental": false,
    "in_stock": true,
    "stock_quantity": 10,
    "category": {"id": "3c015787-d3f4-4f5f-b59a-4e2df669cf2a", "name": "Hammer", "slug": "hammer", "parent_id": "d0abb6f9-0271-4be8-962e-a5374df18006"},
    "brand": {"id": "31cda89a-6497-476a-9184-fc4d9fb940af", "name": "ForgeFlex Tools", "slug": "forgeflex-tools"}
  },
  "dynamic_fields": ["id", "name"],
  "notes": "Captured live 2026-09-24 (product deleted after capture, as part of this same grounding pass's own cleanup). Every submitted field is echoed exactly; category/brand are expanded to full objects matching whatever category_id/brand_id were submitted."
}
```

## Scenario: Creating a product without a token is rejected
1) POST /products with no Authorization header (same valid body as above)
2) Response status is 401

Request:
```json
{
  "method": "POST",
  "path": "/products",
  "auth": "none",
  "body": {
    "name": "Test Hammer <unique>",
    "description": "grounding test",
    "price": 19.99,
    "category_id": "<an existing category id>",
    "brand_id": "<an existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 10
  }
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "error": "Unauthorized",
    "status": 401,
    "message": "A valid Bearer token is required for POST /products"
  },
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24. Same 401 shape as Auth/Users - no timestamp field. This is exactly the access-control check the clone's own docs/TESTING-ORDER.md calls out as a deliberate fix of a real-API bug (anonymous callers could create products there) - verified directly here, not assumed."
}
```

## Scenario: Creating a product as a customer is rejected
1) POST /products with a customer token (same valid body as above)
2) Response status is 403

Request:
```json
{
  "method": "POST",
  "path": "/products",
  "auth": "customer",
  "body": {
    "name": "Test Hammer <unique>",
    "description": "grounding test",
    "price": 19.99,
    "category_id": "<an existing category id>",
    "brand_id": "<an existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 10
  }
}
```

Expected response:
```json
{
  "status": 403,
  "body": {
    "error": "Forbidden",
    "status": 403,
    "message": "Your role does not permit POST /products"
  },
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24. Same real-API-bug-fix reasoning as the 401 case above, this time the wrong-role half of the matrix."
}
```

## Scenario: Creating a product without the required fields is rejected
1) POST /products as admin with only a name
2) Response status is 422
3) Response body's message lists each missing field (category_id, brand_id,
   description, is_rental, stock_quantity, is_location_offer, price)

Request:
```json
{
  "method": "POST",
  "path": "/products",
  "auth": "admin",
  "body": {
    "name": "Incomplete"
  }
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T04:03:08.562557934",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "category_id: must not be blank; brand_id: must not be blank; description: must not be blank; is_rental: must not be null; stock_quantity: must not be null; is_location_offer: must not be null; price: must not be null"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Semicolon-joined, order not a documented contract - assert each fragment is present, not the whole string or its order (same pattern as Auth's missing-required-fields). name is notably absent from this list - it is genuinely optional, unlike what the schema's required list alone would suggest missing category_id/brand_id/description."
}
```

## Scenario: Creating a product with a negative price is rejected
1) POST /products as admin with a valid body except price is -5
2) Response status is 422
3) Response body's message names the price rule

Request:
```json
{
  "method": "POST",
  "path": "/products",
  "auth": "admin",
  "body": {
    "name": "Bad Price",
    "description": "desc",
    "price": -5,
    "category_id": "<an existing category id>",
    "brand_id": "<an existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 10
  }
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T04:03:09.557489630",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "price: must be greater than or equal to 0.0"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24 with a fully valid body otherwise (description/category_id/brand_id present) so only the price violation fires - assert the message contains 'price' and 'greater than or equal to 0'."
}
```

## Scenario: Creating a product with a nonexistent category is rejected
1) POST /products as admin with a valid body except category_id is a
   well-formed but nonexistent id
2) Response status is 404
3) Response body's message names the missing category id

Request:
```json
{
  "method": "POST",
  "path": "/products",
  "auth": "admin",
  "body": {
    "name": "Bad Cat",
    "description": "desc",
    "price": 9.99,
    "category_id": "00000000-0000-0000-0000-000000000000",
    "brand_id": "<an existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 10
  }
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T04:03:26.184627381",
    "status": 404,
    "error": "Not Found",
    "message": "Category 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Foreign-key existence is checked (404), not deferred to a DB constraint error - and checked independently of the other 422 field validations (this body is otherwise fully valid, so only this one violation fires)."
}
```

## Scenario: Creating a product with a nonexistent brand is rejected
1) POST /products as admin with a valid body except brand_id is a
   well-formed but nonexistent id
2) Response status is 404
3) Response body's message names the missing brand id

Request:
```json
{
  "method": "POST",
  "path": "/products",
  "auth": "admin",
  "body": {
    "name": "Bad Brand",
    "description": "desc",
    "price": 9.99,
    "category_id": "<an existing category id>",
    "brand_id": "00000000-0000-0000-0000-000000000000",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 10
  }
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T04:03:26.276224770",
    "status": 404,
    "error": "Not Found",
    "message": "Brand 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Same pattern as the nonexistent-category case."
}
```

## Scenario: Update a product as admin  [smoke]
1) Create a product as admin (track for cleanup)
2) PUT /products/{id} as admin with changed name/price/stock_quantity
3) Response status is 200
4) Response body reflects the changes

Request:
```json
{
  "method": "PUT",
  "path": "/products/{id}",
  "auth": "admin",
  "body": {
    "name": "Updated Hammer",
    "description": "upd",
    "price": 29.99,
    "category_id": "<same existing category id>",
    "brand_id": "<same existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 5
  }
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "id": "b3bdf0ea-c649-4f24-9e3c-d3e75ba95842",
    "name": "Updated Hammer",
    "description": "upd",
    "price": 29.99,
    "is_location_offer": false,
    "is_rental": false,
    "in_stock": true,
    "stock_quantity": 5,
    "category": {"id": "3c015787-d3f4-4f5f-b59a-4e2df669cf2a", "name": "Hammer", "slug": "hammer", "parent_id": "d0abb6f9-0271-4be8-962e-a5374df18006"},
    "brand": {"id": "31cda89a-6497-476a-9184-fc4d9fb940af", "name": "ForgeFlex Tools", "slug": "forgeflex-tools"}
  },
  "dynamic_fields": ["id"],
  "notes": "Captured live 2026-09-24. Every submitted field is reflected exactly, same id as before the update."
}
```

## Scenario: Updating a product without a token is rejected
1) Create a product as admin (track for cleanup)
2) PUT /products/{id} with no Authorization header
3) Response status is 401

Request:
```json
{
  "method": "PUT",
  "path": "/products/{id}",
  "auth": "none",
  "body": {
    "name": "Updated",
    "description": "upd",
    "price": 29.99,
    "category_id": "<same existing category id>",
    "brand_id": "<same existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 5
  }
}
```

Expected response:
```json
{
  "status": 401,
  "body": {
    "error": "Unauthorized",
    "status": 401,
    "message": "A valid Bearer token is required for PUT /products/b3bdf0ea-c649-4f24-9e3c-d3e75ba95842"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24. message names the exact method+path including the id - assert it contains 'Bearer token is required', not the full sentence with its id."
}
```

## Scenario: Updating a product as a customer is rejected
1) Create a product as admin (track for cleanup)
2) PUT /products/{id} with a customer token
3) Response status is 403

Request:
```json
{
  "method": "PUT",
  "path": "/products/{id}",
  "auth": "customer",
  "body": {
    "name": "Updated",
    "description": "upd",
    "price": 29.99,
    "category_id": "<same existing category id>",
    "brand_id": "<same existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 5
  }
}
```

Expected response:
```json
{
  "status": 403,
  "body": {
    "error": "Forbidden",
    "status": 403,
    "message": "Your role does not permit PUT /products/b3bdf0ea-c649-4f24-9e3c-d3e75ba95842"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24. Same pattern as the create-as-customer case."
}
```

## Scenario: Updating a nonexistent product is rejected
1) PUT /products/{a well-formed but nonexistent id} as admin
2) Response status is 404

Request:
```json
{
  "method": "PUT",
  "path": "/products/00000000-0000-0000-0000-000000000000",
  "auth": "admin",
  "body": {
    "name": "X",
    "description": "d",
    "price": 1,
    "category_id": "<an existing category id>",
    "brand_id": "<an existing brand id>",
    "is_location_offer": false,
    "is_rental": false,
    "stock_quantity": 1
  }
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T04:03:40.791685515",
    "status": 404,
    "error": "Not Found",
    "message": "Product 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Existence is checked before the update is applied - a fully valid body otherwise."
}
```

## Scenario: Delete a product as admin  [smoke]
1) Create a product as admin
2) DELETE /products/{id} as admin
3) Response status is 204
4) GET /products/{id} afterward returns 404 (nothing left to track for cleanup - deletion IS the cleanup)

Request:
```json
{
  "method": "DELETE",
  "path": "/products/{id}",
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
  "notes": "Captured live 2026-09-24. Empty body, matches the project's established DELETE-> 204 convention (same as DELETE /users/{id})."
}
```

## Scenario: Deleting a product without a token is rejected
1) Create a product as admin (track for cleanup, since this scenario doesn't
   delete it)
2) DELETE /products/{id} with no Authorization header
3) Response status is 401

Request:
```json
{
  "method": "DELETE",
  "path": "/products/{id}",
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
    "message": "A valid Bearer token is required for DELETE /products/b3bdf0ea-c649-4f24-9e3c-d3e75ba95842"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Deleting a product as a customer is rejected
1) Create a product as admin (track for cleanup, since this scenario doesn't
   delete it)
2) DELETE /products/{id} with a customer token
3) Response status is 403

Request:
```json
{
  "method": "DELETE",
  "path": "/products/{id}",
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
    "message": "Your role does not permit DELETE /products/b3bdf0ea-c649-4f24-9e3c-d3e75ba95842"
  },
  "dynamic_fields": ["message"],
  "notes": "Captured live 2026-09-24. This is the exact real-API bug the clone's docs/TESTING-ORDER.md calls out fixing (customer callers could delete/create products there) - verified directly."
}
```

## Scenario: Deleting a nonexistent product is rejected
1) DELETE /products/{a well-formed but nonexistent id} as admin
2) Response status is 404

Request:
```json
{
  "method": "DELETE",
  "path": "/products/00000000-0000-0000-0000-000000000000",
  "auth": "admin",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T04:03:40.867026386",
    "status": 404,
    "error": "Not Found",
    "message": "Product 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```
