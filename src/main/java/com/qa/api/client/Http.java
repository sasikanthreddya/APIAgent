package com.qa.api.client;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Thin wrapper around REST Assured's given()/when()/then() - the transport every
 * resource client goes through.
 *
 * Re-tested 2026-09-23 against toolshop-clone-api (localhost:8070) on this same
 * machine after an earlier session had swapped this to java.net.http.HttpClient,
 * citing an unreliable REST Assured HTTPBuilder under Maven-launched Surefire
 * (IllegalStateException: Target host is null, synthetic 404s, "connection still
 * allocated"). That failure did not reproduce here: the full suite passes with
 * given()/when()/then() restored. If it ever resurfaces, capture the exact
 * stack trace before reaching for a workaround again.
 */
final class Http {

    private Http() {
    }

    static ApiResponse get(String url, Map<String, String> headers) {
        return new ApiResponse(request(headers, null).when().get(url).then().extract().response());
    }

    static ApiResponse post(String url, Map<String, String> headers, Object body) {
        return new ApiResponse(request(headers, body).when().post(url).then().extract().response());
    }

    static ApiResponse put(String url, Map<String, String> headers, Object body) {
        return new ApiResponse(request(headers, body).when().put(url).then().extract().response());
    }

    static ApiResponse delete(String url, Map<String, String> headers) {
        return new ApiResponse(request(headers, null).when().delete(url).then().extract().response());
    }

    static String withQueryParam(String url, String name, String value) {
        return url + "?" + name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static RequestSpecification request(Map<String, String> headers, Object body) {
        RequestSpecification spec = RestAssured.given().headers(headers);
        return body == null ? spec : spec.contentType(ContentType.JSON).body(body);
    }
}
