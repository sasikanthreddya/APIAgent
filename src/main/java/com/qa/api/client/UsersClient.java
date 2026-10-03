package com.qa.api.client;

/**
 * Users resource - own profile only (GET /users/me). The rest of the Users
 * resource (list, get by id, update, delete - all admin- or ownership-gated)
 * is out of scope for this pass; add it here when that flow is reviewed and
 * approved.
 */
public final class UsersClient extends BaseClient {

    public UsersClient(String bearerToken) {
        super(bearerToken);
    }

    public static UsersClient anonymous() {
        return new UsersClient(null);
    }

    public static UsersClient as(Role role) {
        return new UsersClient(AuthClient.tokenFor(role));
    }

    public ApiResponse me() {
        return Http.get(url("/users/me"), headers());
    }
}
