package com.qa.api.client;

import java.util.Map;

/**
 * Categories resource: list, get by id, create, update, delete. Reads (GET)
 * are public; writes (POST/PUT/DELETE) are admin-only - the same
 * access-control matrix as Products, verified independently live 2026-09-24.
 *
 * {@code GET /categories} returns a plain JSON array, not a paginated
 * wrapper (unlike Products) - confirmed live, not assumed from the schema.
 */
public final class CategoriesClient extends BaseClient {

    public CategoriesClient(String bearerToken) {
        super(bearerToken);
    }

    public static CategoriesClient anonymous() {
        return new CategoriesClient(null);
    }

    public static CategoriesClient as(Role role) {
        return new CategoriesClient(AuthClient.tokenFor(role));
    }

    public ApiResponse list() {
        return Http.get(url("/categories"), headers());
    }

    public ApiResponse getById(String id) {
        return Http.get(url("/categories/" + id), headers());
    }

    public ApiResponse create(Map<String, Object> body) {
        return Http.post(url("/categories"), headers(), body);
    }

    public ApiResponse update(String id, Map<String, Object> body) {
        return Http.put(url("/categories/" + id), headers(), body);
    }

    public ApiResponse delete(String id) {
        return Http.delete(url("/categories/" + id), headers());
    }
}
