package com.qa.api.client;

import java.util.Map;

/**
 * Cart resource: create, get, add/update/remove item, delete. Fully
 * anonymous - no endpoint needs a token, confirmed live 2026-09-24 (a bearer
 * token doesn't even change the response body).
 */
public final class CartClient extends BaseClient {

    public CartClient() {
        super(null);
    }

    public ApiResponse create() {
        return Http.post(url("/carts"), headers(), null);
    }

    public ApiResponse get(String cartId) {
        return Http.get(url("/carts/" + cartId), headers());
    }

    public ApiResponse addItem(String cartId, Map<String, Object> body) {
        return Http.post(url("/carts/" + cartId), headers(), body);
    }

    public ApiResponse updateQuantity(String cartId, Map<String, Object> body) {
        return Http.put(url("/carts/" + cartId + "/product/quantity"), headers(), body);
    }

    public ApiResponse removeItem(String cartId, String productId) {
        return Http.delete(url("/carts/" + cartId + "/product/" + productId), headers());
    }

    public ApiResponse delete(String cartId) {
        return Http.delete(url("/carts/" + cartId), headers());
    }
}
