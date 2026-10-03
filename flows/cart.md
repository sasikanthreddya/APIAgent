Service Type: Cart (POST /carts, GET /carts/{cartId}, POST /carts/{cartId},
PUT /carts/{cartId}/product/quantity, DELETE /carts/{cartId}/product/{productId},
DELETE /carts/{cartId})

Grounded 2026-09-24 against the running toolshop-clone-api (localhost:8070), via
its live Swagger (`/v3/api-docs`) and direct curl calls - not from the OpenAPI
schema alone. Every request/expected-response pair below was captured from a
real call this run. Cross-referenced against the clone's own
`docs/TESTING-ORDER.md` Phase 4, which confirms Cart works entirely
anonymously.

Request contract (AddCartItemRequest, from the live schema - required-ness not
reliable in the schema itself, but this one happens to match what's actually
enforced):
  product_id  string   - required (blank -> 422), must reference an existing
                          product (unknown -> 404 "Product ... not found")
  quantity    integer, >= 1  - required (< 1 -> 422 "must be greater than or
                                equal to 1")

Response contract (CartResponse): id, cart_items (array of Item: id,
product_id, product [full nested ProductResponse], quantity, line_total),
total (sum of every item's line_total). No field ties a cart to a user, even
when created with a bearer token - the response body is identical either way
(confirmed live); whatever "attaches" a token's cart happens outside what
these endpoints expose.

## Background
No auth needed anywhere in this resource - every scenario runs anonymously.
Every scenario needs a real product id, looked up at runtime via Products'
already-generated client (`ProductsClient`) - Products is already generated,
so this is reuse, not pre-building ahead (same reasoning as Brands' known-
defect scenario reusing `ProductsClient`).

## Scenario: Create a cart  [smoke]
1) POST /carts with no body
2) Response status is 201
3) Response body has a new id, empty cart_items, total 0

Request:
```json
{
  "method": "POST",
  "path": "/carts",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 201,
  "body": {"id": "51124eff-8e3c-47a5-bfa4-4f238df003e3", "cart_items": [], "total": 0},
  "dynamic_fields": ["id"],
  "notes": "Captured live 2026-09-24 (cart deleted after capture). NOTE: springdoc declares 200 for this operation like every other create - live is 201, same pattern as every other creation endpoint in this codebase."
}
```

## Scenario: Get a cart by id  [smoke]
1) Create a cart
2) GET /carts/{that cart's id}
3) Response status is 200
4) Response body matches what was just created

Request:
```json
{
  "method": "GET",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {"id": "51124eff-8e3c-47a5-bfa4-4f238df003e3", "cart_items": [], "total": 0},
  "dynamic_fields": ["id"],
  "notes": "Captured live 2026-09-24. Identical shape to the create response."
}
```

## Scenario: Getting a cart by a nonexistent id is rejected
1) GET /carts/{a well-formed but nonexistent id}
2) Response status is 404

Request:
```json
{
  "method": "GET",
  "path": "/carts/00000000-0000-0000-0000-000000000000",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T06:02:44.983680603",
    "status": 404,
    "error": "Not Found",
    "message": "Cart 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Add an item to a cart  [smoke]
1) Create a cart
2) POST /carts/{cartId} with an existing product id and quantity 2
3) Response status is 200
4) Response body's cart_items has one entry for that product with quantity 2
   and line_total = price * 2; total equals that same line_total

Request:
```json
{
  "method": "POST",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": {"product_id": "<an existing product id>", "quantity": 2}
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "id": "51124eff-8e3c-47a5-bfa4-4f238df003e3",
    "cart_items": [
      {
        "id": "0932fa5a-ffe2-4cde-95ad-8576a77667f6",
        "product_id": "36fbaf37-a18a-444e-a3f0-24e63699ee67",
        "product": {"id": "36fbaf37-a18a-444e-a3f0-24e63699ee67", "name": "Safety Goggles", "price": 24.26},
        "quantity": 2,
        "line_total": 48.52
      }
    ],
    "total": 48.52
  },
  "dynamic_fields": ["id", "cart_items[0].id", "cart_items[0].product_id", "cart_items[0].product", "cart_items[0].line_total", "total"],
  "notes": "Captured live 2026-09-24 against product 'Safety Goggles' (price 24.26): line_total = 24.26 * 2 = 48.52, total = 48.52 (only item). Assert line_total/total are computed correctly for whatever product/price the scenario actually uses (price varies per catalogue), not this exact number - and that the nested product object is the full ProductResponse shape."
}
```

## Scenario: Adding the same product again merges the quantity
1) Create a cart and add an existing product with quantity 2
2) POST /carts/{cartId} again with the SAME product id and quantity 1
3) Response status is 200
4) cart_items still has exactly one entry for that product, now with
   quantity 3 (2 + 1) and line_total recalculated - not a second line item

Request:
```json
{
  "method": "POST",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": {"product_id": "<the same product id from step 1>", "quantity": 1}
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "id": "51124eff-8e3c-47a5-bfa4-4f238df003e3",
    "cart_items": [
      {"product_id": "36fbaf37-a18a-444e-a3f0-24e63699ee67", "quantity": 6, "line_total": 145.56}
    ],
    "total": 145.56
  },
  "dynamic_fields": ["id", "cart_items[0].id", "cart_items[0].product", "cart_items[0].line_total", "total"],
  "notes": "Captured live 2026-09-24 (this exact run added quantity 2, then 3 more via a follow-up quantity update, then 1 more - ending at 6; the scenario itself should use a clean 2-then-1 sequence expecting quantity 3). Non-obvious behavior worth asserting explicitly: adding an already-present product_id increments its existing cart_items entry rather than appending a duplicate line."
}
```

## Scenario: Adding an item to a nonexistent cart is rejected
1) POST /carts/{a well-formed but nonexistent cart id} with a valid item
2) Response status is 404
3) Response body's message names the missing cart

Request:
```json
{
  "method": "POST",
  "path": "/carts/00000000-0000-0000-0000-000000000000",
  "auth": "none",
  "body": {"product_id": "<an existing product id>", "quantity": 1}
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T06:02:45.153967844",
    "status": 404,
    "error": "Not Found",
    "message": "Cart 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Cart existence is checked before product existence (see the sibling scenario below, where a valid cart + bad product gives a different message naming the product, not the cart)."
}
```

## Scenario: Adding a nonexistent product to a cart is rejected
1) Create a cart
2) POST /carts/{cartId} with a well-formed but nonexistent product_id
3) Response status is 404
4) Response body's message names the missing product

Request:
```json
{
  "method": "POST",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": {"product_id": "00000000-0000-0000-0000-000000000000", "quantity": 1}
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T06:02:45.073333601",
    "status": 404,
    "error": "Not Found",
    "message": "Product 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Adding an item without a valid quantity is rejected
1) Create a cart
2) POST /carts/{cartId} with an existing product_id and quantity 0
3) Response status is 422
4) Response body's message names the quantity rule

Request:
```json
{
  "method": "POST",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": {"product_id": "<an existing product id>", "quantity": 0}
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T06:02:45.280754437",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "quantity: must be greater than or equal to 1"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24. Unlike most required-field validation elsewhere in this codebase, this one DOES match its schema constraint (minimum: 1) exactly - confirmed live rather than assumed, same rigor either way."
}
```

## Scenario: Adding an item without a product id is rejected
1) Create a cart
2) POST /carts/{cartId} with only a quantity (no product_id)
3) Response status is 422
4) Response body's message names the product_id rule

Request:
```json
{
  "method": "POST",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": {}
}
```

Expected response:
```json
{
  "status": 422,
  "body": {
    "timestamp": "2026-09-24T06:02:45.359552788",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "product_id: must not be blank"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```

## Scenario: Adding a quantity beyond the product's stock is not rejected
1) Create a cart
2) POST /carts/{cartId} with an existing product_id and a quantity far
   exceeding that product's stock_quantity
3) Response status is 200 - the item is added anyway, no stock check

Request:
```json
{
  "method": "POST",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": {"product_id": "<an existing product id>", "quantity": 9999}
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "cart_items": [{"product_id": "36fbaf37-a18a-444e-a3f0-24e63699ee67", "quantity": 9999}]
  },
  "dynamic_fields": ["id", "cart_items", "total"],
  "notes": "Captured live 2026-09-24. This is real, deliberate behavior, not a defect - Cart never checks stock; docs/TESTING-ORDER.md Phase 5 confirms stock is only enforced at checkout (POST /invoices), which must fail before any stock is touched. Worth asserting explicitly since it's easy to wrongly assume the cart itself guards against overselling."
}
```

## Scenario: Update an item's quantity in a cart  [smoke]
1) Create a cart and add an existing product with quantity 2
2) PUT /carts/{cartId}/product/quantity with that product_id and quantity 5
3) Response status is 200
4) That item's quantity is now 5 and its line_total/total are recalculated

Request:
```json
{
  "method": "PUT",
  "path": "/carts/{cartId}/product/quantity",
  "auth": "none",
  "body": {"product_id": "<the product id from step 1>", "quantity": 5}
}
```

Expected response:
```json
{
  "status": 200,
  "body": {
    "cart_items": [{"product_id": "36fbaf37-a18a-444e-a3f0-24e63699ee67", "quantity": 5, "line_total": 121.30}],
    "total": 121.30
  },
  "dynamic_fields": ["id", "cart_items[0].id", "cart_items[0].product", "cart_items[0].line_total", "total"],
  "notes": "Captured live 2026-09-24 (24.26 * 5 = 121.30 for this run's product/price - assert the arithmetic, not this exact number)."
}
```

## Scenario: Updating the quantity of a product not in the cart is rejected
1) Create a cart (no items added)
2) PUT /carts/{cartId}/product/quantity with a product_id that isn't in
   that cart
3) Response status is 404
4) Response body's message names both the product and the cart

Request:
```json
{
  "method": "PUT",
  "path": "/carts/{cartId}/product/quantity",
  "auth": "none",
  "body": {"product_id": "00000000-0000-0000-0000-000000000000", "quantity": 3}
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T06:03:04.979987894",
    "status": 404,
    "error": "Not Found",
    "message": "Product 00000000-0000-0000-0000-000000000000 is not in cart f4f4f273-963e-4fa1-8256-0b3c5c8e8e81"
  },
  "dynamic_fields": ["timestamp", "message"],
  "notes": "Captured live 2026-09-24. Distinct message shape from 'Product ... not found' - this one names both the product and the specific cart, since the product genuinely exists, it's just not in this cart's items."
}
```

## Scenario: Remove an item from a cart  [smoke]
1) Create a cart and add an existing product
2) DELETE /carts/{cartId}/product/{productId}
3) Response status is 200 (not 204 - this endpoint returns the updated cart)
4) Response body's cart_items no longer contains that product, total is 0

Request:
```json
{
  "method": "DELETE",
  "path": "/carts/{cartId}/product/{productId}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 200,
  "body": {"id": "f4f4f273-963e-4fa1-8256-0b3c5c8e8e81", "cart_items": [], "total": 0},
  "dynamic_fields": ["id"],
  "notes": "Captured live 2026-09-24. Unlike every DELETE elsewhere in this codebase (204 empty body), removing one item from a cart returns 200 with the updated cart - deleting the whole cart (see below) is the one that returns 204."
}
```

## Scenario: Removing a product not in the cart is rejected
1) Create a cart (no items added)
2) DELETE /carts/{cartId}/product/{a well-formed but not-in-cart product id}
3) Response status is 404

Request:
```json
{
  "method": "DELETE",
  "path": "/carts/{cartId}/product/{productId}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T06:03:05.297177229",
    "status": 404,
    "error": "Not Found",
    "message": "Product 36fbaf37-a18a-444e-a3f0-24e63699ee67 is not in cart f4f4f273-963e-4fa1-8256-0b3c5c8e8e81"
  },
  "dynamic_fields": ["timestamp", "message"],
  "notes": "Captured live 2026-09-24. Same message pattern as the quantity-update case."
}
```

## Scenario: Delete a cart  [smoke]
1) Create a cart
2) DELETE /carts/{cartId}
3) Response status is 204
4) GET /carts/{cartId} afterward returns 404 (deletion IS the cleanup, same
   as every other resource's admin delete - no tracking needed)

Request:
```json
{
  "method": "DELETE",
  "path": "/carts/{cartId}",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 204,
  "body": null,
  "dynamic_fields": [],
  "notes": "Captured live 2026-09-24. Empty body - same convention as every other full-resource DELETE in this codebase, unlike the single-item removal above which returns 200 with a body."
}
```

## Scenario: Deleting a nonexistent cart is rejected
1) DELETE /carts/{a well-formed but nonexistent id}
2) Response status is 404

Request:
```json
{
  "method": "DELETE",
  "path": "/carts/00000000-0000-0000-0000-000000000000",
  "auth": "none",
  "body": null
}
```

Expected response:
```json
{
  "status": 404,
  "body": {
    "timestamp": "2026-09-24T06:03:33.084555181",
    "status": 404,
    "error": "Not Found",
    "message": "Cart 00000000-0000-0000-0000-000000000000 not found"
  },
  "dynamic_fields": ["timestamp"],
  "notes": "Captured live 2026-09-24."
}
```
