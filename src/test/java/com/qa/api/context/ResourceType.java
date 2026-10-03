package com.qa.api.context;

/**
 * Everything a scenario can create and must clean up, in cleanup order
 * (declaration order = deletion order in Hooks).
 *
 * A case is added here only alongside the client method and step that create
 * that resource, once you approve it - never ahead of that approval.
 */
public enum ResourceType {
    /** A user account created via POST /users/register (AuthSteps). Deleted via AuthClient. */
    USER,
    /** A product created via POST /products (ProductsSteps). Deleted via ProductsClient. */
    PRODUCT,
    /** A category created via POST /categories (CategoriesSteps). Deleted via CategoriesClient. */
    CATEGORY,
    /**
     * A brand created via POST /brands (BrandsSteps). Deleted via BrandsClient.
     * Must stay declared after PRODUCT: deleting a brand still referenced by a
     * product 500s (known defect, see flows/brands.md) - PRODUCT's cleanup
     * running first is what lets this resource's own cleanup succeed.
     */
    BRAND,
    /** A cart created via POST /carts (CartSteps). Deleted via CartClient. */
    CART
}
