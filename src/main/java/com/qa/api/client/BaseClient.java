package com.qa.api.client;

import com.qa.api.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared plumbing for every resource client: holds the (nullable) bearer token and
 * builds the request headers from it. Subclasses add one method per endpoint and
 * nothing else - no assertions, no test data, no URL building outside ConfigManager.
 *
 * The convention every generated client follows: {@code XClient.anonymous()} (no
 * Authorization header), {@code XClient.as(Role.ADMIN)} (a cached seeded-account
 * token - once the Auth resource is generated, that lookup goes through whatever
 * class owns login/token-caching) and {@code new XClient(token)} (an arbitrary
 * token, e.g. a throwaway user's).
 */
public abstract class BaseClient {

    private final String bearerToken;

    protected BaseClient(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    protected Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (bearerToken != null) {
            headers.put("Authorization", "Bearer " + bearerToken);
        }
        return headers;
    }

    protected String url(String path) {
        return ConfigManager.url(path);
    }

    protected String url(String path, String queryName, String queryValue) {
        return Http.withQueryParam(ConfigManager.url(path), queryName, queryValue);
    }
}
