package com.qa.api.client;

import com.qa.api.config.ConfigManager;

import java.util.EnumMap;
import java.util.Map;

/**
 * Auth resource: POST /users/register and POST /users/login, both open (no token
 * needed). Also owns the seeded-account token cache every other resource client's
 * {@code as(Role)} goes through (see BaseClient), and the admin-authenticated
 * delete used only by Hooks to clean up users a scenario registered - DELETE
 * /users/{id} is tagged "Users" in the clone's Swagger, not "Auth", but no Users
 * client exists yet to own it, and cleanup of what Auth creates can't wait for
 * that resource's own review pass.
 */
public final class AuthClient extends BaseClient {

    private static final Map<Role, String> TOKEN_CACHE = new EnumMap<>(Role.class);

    private AuthClient(String bearerToken) {
        super(bearerToken);
    }

    public static AuthClient anonymous() {
        return new AuthClient(null);
    }

    /** A cached bearer token for one of the two seeded accounts - logs in once per role per run. */
    public static String tokenFor(Role role) {
        return TOKEN_CACHE.computeIfAbsent(role, AuthClient::loginSeeded);
    }

    private static String loginSeeded(Role role) {
        String email = role == Role.ADMIN ? ConfigManager.adminEmail() : ConfigManager.customerEmail();
        String password = role == Role.ADMIN ? ConfigManager.adminPassword() : ConfigManager.customerPassword();
        ApiResponse response = anonymous().login(Map.of("email", email, "password", password));
        return response.jsonPath().getString("access_token");
    }

    public ApiResponse register(Map<String, Object> body) {
        return Http.post(url("/users/register"), headers(), body);
    }

    public ApiResponse login(Map<String, Object> body) {
        return Http.post(url("/users/login"), headers(), body);
    }

    /** Deletes a user by id, as admin - used only by Hooks, never asserted on directly. */
    public static void deleteRegisteredUser(String id) {
        new AuthClient(tokenFor(Role.ADMIN)).delete(id);
    }

    private ApiResponse delete(String id) {
        return Http.delete(url("/users/" + id), headers());
    }
}
