package com.qa.api.client;

import com.qa.api.config.ConfigManager;

import java.util.Collections;
import java.util.Map;

/**
 * Products resource: list, get by id, search, related, create, update, delete.
 * Catalogue reads (GET) are public; writes (POST/PUT/DELETE) are admin-only -
 * verified live 2026-09-24 against docs/TESTING-ORDER.md's Phase 3
 * access-control matrix (401 anonymous, 403 customer, 2xx admin).
 */
public final class ProductsClient extends BaseClient {

    private static String cachedCategoryId;
    private static String cachedBrandId;

    public ProductsClient(String bearerToken) {
        super(bearerToken);
    }

    public static ProductsClient anonymous() {
        return new ProductsClient(null);
    }

    public static ProductsClient as(Role role) {
        return new ProductsClient(AuthClient.tokenFor(role));
    }

    public ApiResponse list() {
        return Http.get(url("/products"), headers());
    }

    public ApiResponse listByCategory(String categoryId) {
        return Http.get(url("/products", "categoryId", categoryId), headers());
    }

    public ApiResponse getById(String id) {
        return Http.get(url("/products/" + id), headers());
    }

    public ApiResponse search(String query) {
        return Http.get(url("/products/search", "q", query), headers());
    }

    public ApiResponse searchWithoutQuery() {
        return Http.get(url("/products/search"), headers());
    }

    public ApiResponse related(String id) {
        return Http.get(url("/products/" + id + "/related"), headers());
    }

    public ApiResponse create(Map<String, Object> body) {
        return Http.post(url("/products"), headers(), body);
    }

    public ApiResponse update(String id, Map<String, Object> body) {
        return Http.put(url("/products/" + id), headers(), body);
    }

    public ApiResponse delete(String id) {
        return Http.delete(url("/products/" + id), headers());
    }

    /**
     * Setup-only lookups against seeded Categories/Brands data - Products needs a
     * real category_id/brand_id to build a valid payload, but Categories/Brands
     * haven't been through their own review/generate pass yet (see code-gen
     * SKILL.md's step 5: borrow the read, don't pre-build the resource). Cached
     * for the run since seeded categories/brands don't change mid-suite. Never
     * asserted on directly, never grows into create/update/delete for either.
     */
    public static synchronized String anyCategoryId() {
        if (cachedCategoryId == null) {
            ApiResponse response = Http.get(ConfigManager.url("/categories"), Collections.emptyMap());
            cachedCategoryId = response.jsonPath().getString("[0].id");
        }
        return cachedCategoryId;
    }

    public static synchronized String anyBrandId() {
        if (cachedBrandId == null) {
            ApiResponse response = Http.get(ConfigManager.url("/brands"), Collections.emptyMap());
            cachedBrandId = response.jsonPath().getString("[0].id");
        }
        return cachedBrandId;
    }
}
