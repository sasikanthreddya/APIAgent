package com.qa.api.steps;

import com.qa.api.client.ApiResponse;
import com.qa.api.client.AuthClient;
import com.qa.api.client.UsersClient;
import com.qa.api.context.ResourceType;
import com.qa.api.context.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import org.assertj.core.api.Assertions;

import java.util.HashMap;
import java.util.Map;

/** Steps for the Users resource: GET /users/me. */
public class UsersSteps {

    private final TestContext context;

    private String token;
    private String registeredId;
    private String registeredEmail;
    private String registeredFirstName;
    private String registeredLastName;

    public UsersSteps(TestContext context) {
        this.context = context;
    }

    @Given("I have registered a unique user and logged in")
    public void iHaveRegisteredAUniqueUserAndLoggedIn() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("first_name", "Users");
        payload.put("last_name", "Flow");
        payload.put("email", uniqueEmail("usersflow"));
        payload.put("password", "Password123!");

        ApiResponse registerResponse = AuthClient.anonymous().register(payload);
        registeredId = registerResponse.jsonPath().getString("id");
        context.track(ResourceType.USER, registeredId);
        registeredEmail = (String) payload.get("email");
        registeredFirstName = (String) payload.get("first_name");
        registeredLastName = (String) payload.get("last_name");

        Map<String, Object> loginPayload = new HashMap<>();
        loginPayload.put("email", registeredEmail);
        loginPayload.put("password", payload.get("password"));
        ApiResponse loginResponse = AuthClient.anonymous().login(loginPayload);
        token = loginResponse.jsonPath().getString("access_token");
    }

    @When("I fetch my own profile")
    public void iFetchMyOwnProfile() {
        context.setLastResponse(new UsersClient(token).me());
    }

    @When("I fetch my own profile with no token")
    public void iFetchMyOwnProfileWithNoToken() {
        context.setLastResponse(UsersClient.anonymous().me());
    }

    @When("I fetch my own profile with a malformed token")
    public void iFetchMyOwnProfileWithAMalformedToken() {
        context.setLastResponse(new UsersClient("not.a.jwt").me());
    }

    @Then("my profile's id, email, first_name and last_name match the registration")
    public void myProfileMatchesTheRegistration() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("id")).isEqualTo(registeredId);
        Assertions.assertThat(body.getString("email")).isEqualTo(registeredEmail);
        Assertions.assertThat(body.getString("first_name")).isEqualTo(registeredFirstName);
        Assertions.assertThat(body.getString("last_name")).isEqualTo(registeredLastName);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + System.nanoTime() + "@toolshop.test";
    }
}
