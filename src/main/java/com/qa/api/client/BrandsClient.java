package com.qa.api.client;

import java.util.Map;

/**
 * Brands resource: list, get by id, create, update, delete. Reads (GET) are
 * public; writes (POST/PUT/DELETE) are admin-only - the same access-control
 * matrix as Products/Categories, verified independently live 2026-09-24.
 *
 * {@code GET /brands} returns a plain JSON array, not a paginated wrapper -
 * same as Categories, confirmed live.
 */
public final class BrandsClient extends BaseClient {

    public BrandsClient(String bearerToken) {
        super(bearerToken);
    }

    public static BrandsClient anonymous() {
        return new BrandsClient(null);
    }

    public static BrandsClient as(Role role) {
        return new BrandsClient(AuthClient.tokenFor(role));
    }

    public ApiResponse list() {
        return Http.get(url("/brands"), headers());
    }

    public ApiResponse getById(String id) {
        return Http.get(url("/brands/" + id), headers());
    }

    public ApiResponse create(Map<String, Object> body) {
        return Http.post(url("/brands"), headers(), body);
    }

    public ApiResponse update(String id, Map<String, Object> body) {
        return Http.put(url("/brands/" + id), headers(), body);
    }

    public ApiResponse delete(String id) {
        return Http.delete(url("/brands/" + id), headers());
    }
}
