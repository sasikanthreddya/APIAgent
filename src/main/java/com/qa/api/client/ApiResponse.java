package com.qa.api.client;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * Minimal wrapper around REST Assured's Response so step definitions have a
 * stable call shape (.statusCode(), .jsonPath(), .asString()) regardless of
 * which client built the request.
 */
public final class ApiResponse {

    private final Response raw;

    public ApiResponse(Response raw) {
        this.raw = raw;
    }

    public int statusCode() {
        return raw.getStatusCode();
    }

    public String asString() {
        return raw.getBody().asString();
    }

    public JsonPath jsonPath() {
        return raw.jsonPath();
    }
}
