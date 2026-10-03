package com.qa.api.steps;

import com.qa.api.client.ApiResponse;
import com.qa.api.client.AuthClient;
import com.qa.api.context.ResourceType;
import com.qa.api.context.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import org.assertj.core.api.Assertions;

import java.util.HashMap;
import java.util.Map;

/** Steps for the Auth resource: POST /users/register and POST /users/login. */
public class AuthSteps {

    private final TestContext context;

    private Map<String, Object> registrationPayload;
    private String loginEmail;
    private String loginPassword;

    public AuthSteps(TestContext context) {
        this.context = context;
    }

    @Given("a unique new user with a full profile")
    public void aUniqueNewUserWithAFullProfile() {
        registrationPayload = fullProfilePayload();
    }

    @Given("a unique new user with only the required fields")
    public void aUniqueNewUserWithOnlyTheRequiredFields() {
        registrationPayload = minimalPayload();
    }

    @Given("a new user's registration payload with only an email")
    public void aNewUsersRegistrationPayloadWithOnlyAnEmail() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", uniqueEmail("x"));
        registrationPayload = payload;
    }

    @Given("a unique new user with {string} set to {string}")
    public void aUniqueNewUserWithFieldSetTo(String field, String value) {
        registrationPayload = minimalPayload();
        registrationPayload.put(field, value);
    }

    @Given("I have registered a unique user")
    public void iHaveRegisteredAUniqueUser() {
        registrationPayload = minimalPayload();
        ApiResponse response = AuthClient.anonymous().register(registrationPayload);
        context.setLastResponse(response);
        trackIfCreated(response);
        loginEmail = (String) registrationPayload.get("email");
        loginPassword = (String) registrationPayload.get("password");
    }

    @Given("an email that has never been registered")
    public void anEmailThatHasNeverBeenRegistered() {
        loginEmail = uniqueEmail("nobody");
    }

    @When("I register that user")
    public void iRegisterThatUser() {
        ApiResponse response = AuthClient.anonymous().register(registrationPayload);
        context.setLastResponse(response);
        trackIfCreated(response);
    }

    @When("I register another user with that same email")
    public void iRegisterAnotherUserWithThatSameEmail() {
        Map<String, Object> payload = minimalPayload();
        payload.put("email", registrationPayload.get("email"));
        ApiResponse response = AuthClient.anonymous().register(payload);
        context.setLastResponse(response);
        trackIfCreated(response);
    }

    @When("I log in with that user's email and password")
    public void iLogInWithThatUsersEmailAndPassword() {
        login(loginEmail, loginPassword);
    }

    @When("I log in with that user's email and an incorrect password")
    public void iLogInWithThatUsersEmailAndAnIncorrectPassword() {
        login(loginEmail, "WrongPass1!");
    }

    @When("I log in with that email and any password")
    public void iLogInWithThatEmailAndAnyPassword() {
        login(loginEmail, "Password123!");
    }

    @When("I log in with that user's email and no password")
    public void iLogInWithThatUsersEmailAndNoPassword() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", loginEmail);
        ApiResponse response = AuthClient.anonymous().login(payload);
        context.setLastResponse(response);
    }

    @Then("the registered user's role is {string}")
    public void theRegisteredUsersRoleIs(String role) {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("role")).isEqualTo(role);
    }

    @Then("the registered user's email matches the request")
    public void theRegisteredUsersEmailMatchesTheRequest() {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("email"))
                .isEqualTo(registrationPayload.get("email"));
    }

    @Then("every submitted profile field is echoed back exactly")
    public void everySubmittedProfileFieldIsEchoedBackExactly() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("first_name")).isEqualTo(registrationPayload.get("first_name"));
        Assertions.assertThat(body.getString("last_name")).isEqualTo(registrationPayload.get("last_name"));
        Assertions.assertThat(body.getString("phone")).isEqualTo(registrationPayload.get("phone"));
        Assertions.assertThat(body.getString("dob")).isEqualTo(registrationPayload.get("dob"));

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) registrationPayload.get("address");
        Assertions.assertThat(body.getString("address.street")).isEqualTo(address.get("street"));
        Assertions.assertThat(body.getString("address.house_number")).isEqualTo(address.get("house_number"));
        Assertions.assertThat(body.getString("address.city")).isEqualTo(address.get("city"));
        Assertions.assertThat(body.getString("address.state")).isEqualTo(address.get("state"));
        Assertions.assertThat(body.getString("address.country")).isEqualTo(address.get("country"));
        Assertions.assertThat(body.getString("address.postal_code")).isEqualTo(address.get("postal_code"));
    }

    @Then("phone, dob and every address field in the response are null")
    public void phoneDobAndEveryAddressFieldInTheResponseAreNull() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("phone")).isNull();
        Assertions.assertThat(body.getString("dob")).isNull();
        Assertions.assertThat(body.getString("address.street")).isNull();
        Assertions.assertThat(body.getString("address.house_number")).isNull();
        Assertions.assertThat(body.getString("address.city")).isNull();
        Assertions.assertThat(body.getString("address.state")).isNull();
        Assertions.assertThat(body.getString("address.country")).isNull();
        Assertions.assertThat(body.getString("address.postal_code")).isNull();
    }

    @Then("the response message contains {string}")
    public void theResponseMessageContains(String fragment) {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("message"))
                .as("message, body was: %s", context.getLastResponse().asString())
                .contains(fragment);
    }

    @Then("the response message is {string}")
    public void theResponseMessageIs(String expected) {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("message"))
                .as("message, body was: %s", context.getLastResponse().asString())
                .isEqualTo(expected);
    }

    @Then("the response message names the conflicting email")
    public void theResponseMessageNamesTheConflictingEmail() {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("message"))
                .contains((String) registrationPayload.get("email"));
    }

    @Then("the response body has a valid access token, bearer token type and a positive expiry")
    public void theResponseBodyHasAValidAccessTokenBearerTokenTypeAndAPositiveExpiry() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("access_token").split("\\.")).hasSize(3);
        Assertions.assertThat(body.getString("token_type")).isEqualTo("bearer");
        Assertions.assertThat(body.getInt("expires_in")).isPositive();
    }

    private void login(String email, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        ApiResponse response = AuthClient.anonymous().login(payload);
        context.setLastResponse(response);
    }

    private void trackIfCreated(ApiResponse response) {
        if (response.statusCode() == 201) {
            context.track(ResourceType.USER, response.jsonPath().getString("id"));
        }
    }

    private Map<String, Object> minimalPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("first_name", "Min");
        payload.put("last_name", "Imal");
        payload.put("email", uniqueEmail("min"));
        payload.put("password", "Password123!");
        return payload;
    }

    private Map<String, Object> fullProfilePayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("first_name", "Jane");
        payload.put("last_name", "Doe");
        payload.put("email", uniqueEmail("jane.doe"));
        payload.put("password", "Password123!");
        payload.put("phone", "0612345678");
        payload.put("dob", "1990-05-15");

        Map<String, Object> address = new HashMap<>();
        address.put("street", "12 Main Street");
        address.put("house_number", "12");
        address.put("city", "Amsterdam");
        address.put("state", "NH");
        address.put("country", "Netherlands");
        address.put("postal_code", "1011AB");
        payload.put("address", address);

        return payload;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + System.nanoTime() + "@toolshop.test";
    }
}
