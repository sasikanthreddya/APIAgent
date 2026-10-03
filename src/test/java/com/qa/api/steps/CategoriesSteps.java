package com.qa.api.steps;

import com.qa.api.client.ApiResponse;
import com.qa.api.client.CategoriesClient;
import com.qa.api.client.Role;
import com.qa.api.context.ResourceType;
import com.qa.api.context.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import org.assertj.core.api.Assertions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Steps for the Categories resource: list, get, create, update, delete. */
public class CategoriesSteps {

    private static final String NONEXISTENT_ID = "00000000-0000-0000-0000-000000000000";

    private final TestContext context;

    private Map<String, Object> categoryPayload;
    private String categoryId;
    private String childCategoryId;

    public CategoriesSteps(TestContext context) {
        this.context = context;
    }

    @Given("a valid category payload")
    public void aValidCategoryPayload() {
        categoryPayload = validPayload();
    }

    @Given("a valid category payload with that category as its parent")
    public void aValidCategoryPayloadWithThatCategoryAsItsParent() {
        Map<String, Object> payload = validPayload();
        payload.put("parent_id", categoryId);
        categoryPayload = payload;
    }

    @Given("a valid category payload with {string} set to {string}")
    public void aValidCategoryPayloadWithFieldSetTo(String field, String value) {
        categoryPayload = validPayload();
        categoryPayload.put(field, value);
    }

    @Given("an empty category payload")
    public void anEmptyCategoryPayload() {
        categoryPayload = new HashMap<>();
    }

    @Given("the category payload is emptied")
    public void theCategoryPayloadIsEmptied() {
        categoryPayload = new HashMap<>();
    }

    @Given("the category's name is changed")
    public void theCategorysNameIsChanged() {
        categoryPayload.put("name", "Updated Root Cat " + System.nanoTime());
    }

    @Given("an existing category's id")
    public void anExistingCategorysId() {
        ApiResponse response = CategoriesClient.anonymous().list();
        categoryId = response.jsonPath().getString("[0].id");
    }

    @Given("I have created a root category as admin")
    public void iHaveCreatedARootCategoryAsAdmin() {
        categoryPayload = validPayload();
        ApiResponse response = CategoriesClient.as(Role.ADMIN).create(categoryPayload);
        categoryId = response.jsonPath().getString("id");
        context.track(ResourceType.CATEGORY, categoryId);
    }

    @Given("I have created a category as admin with that category as its parent")
    public void iHaveCreatedACategoryAsAdminWithThatCategoryAsItsParent() {
        Map<String, Object> payload = validPayload();
        payload.put("parent_id", categoryId);
        ApiResponse response = CategoriesClient.as(Role.ADMIN).create(payload);
        childCategoryId = response.jsonPath().getString("id");
        context.track(ResourceType.CATEGORY, childCategoryId);
    }

    @When("I list all categories")
    public void iListAllCategories() {
        context.setLastResponse(CategoriesClient.anonymous().list());
    }

    @When("I get that category")
    public void iGetThatCategory() {
        context.setLastResponse(CategoriesClient.anonymous().getById(categoryId));
    }

    @When("I get a nonexistent category")
    public void iGetANonexistentCategory() {
        context.setLastResponse(CategoriesClient.anonymous().getById(NONEXISTENT_ID));
    }

    @When("I create that category as {word}")
    public void iCreateThatCategoryAsRole(String role) {
        ApiResponse response = clientForRole(role).create(categoryPayload);
        context.setLastResponse(response);
        trackIfCreated(response);
    }

    @When("I update that category as {word}")
    public void iUpdateThatCategoryAsRole(String role) {
        context.setLastResponse(clientForRole(role).update(categoryId, categoryPayload));
    }

    @When("I update a nonexistent category as admin")
    public void iUpdateANonexistentCategoryAsAdmin() {
        context.setLastResponse(CategoriesClient.as(Role.ADMIN).update(NONEXISTENT_ID, validPayload()));
    }

    @When("I delete that category as {word}")
    public void iDeleteThatCategoryAsRole(String role) {
        context.setLastResponse(clientForRole(role).delete(categoryId));
    }

    @When("I delete a nonexistent category as admin")
    public void iDeleteANonexistentCategoryAsAdmin() {
        context.setLastResponse(CategoriesClient.as(Role.ADMIN).delete(NONEXISTENT_ID));
    }

    @Then("the response body is a non-empty array of categories")
    public void theResponseBodyIsANonEmptyArrayOfCategories() {
        JsonPath body = context.getLastResponse().jsonPath();
        List<Object> categories = body.getList("$");
        Assertions.assertThat(categories).isNotEmpty();
        Assertions.assertThat(body.getString("[0].id")).isNotBlank();
        Assertions.assertThat(body.getString("[0].name")).isNotBlank();
        Assertions.assertThat(body.getString("[0].slug")).isNotBlank();
    }

    @Then("the response's id matches the requested category")
    public void theResponsesIdMatchesTheRequestedCategory() {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("id")).isEqualTo(categoryId);
    }

    @Then("the created category echoes the submitted name with an auto-generated slug and no parent")
    public void theCreatedCategoryEchoesTheSubmittedNameWithAnAutoGeneratedSlugAndNoParent() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("name")).isEqualTo(categoryPayload.get("name"));
        Assertions.assertThat(body.getString("slug")).isNotBlank();
        Assertions.assertThat(body.getString("parent_id")).isNull();
    }

    @Then("the created category's parent id matches the parent")
    public void theCreatedCategorysParentIdMatchesTheParent() {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("parent_id"))
                .isEqualTo(categoryPayload.get("parent_id"));
    }

    @Then("the updated category reflects the changes")
    public void theUpdatedCategoryReflectsTheChanges() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("id")).isEqualTo(categoryId);
        Assertions.assertThat(body.getString("name")).isEqualTo(categoryPayload.get("name"));
    }

    @Then("the category no longer exists")
    public void theCategoryNoLongerExists() {
        ApiResponse response = CategoriesClient.anonymous().getById(categoryId);
        Assertions.assertThat(response.statusCode()).isEqualTo(404);
    }

    @Then("the child category still exists with a dangling parent reference")
    public void theChildCategoryStillExistsWithADanglingParentReference() {
        ApiResponse response = CategoriesClient.anonymous().getById(childCategoryId);
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.jsonPath().getString("parent_id")).isEqualTo(categoryId);
    }

    private CategoriesClient clientForRole(String role) {
        return switch (role) {
            case "admin" -> CategoriesClient.as(Role.ADMIN);
            case "customer" -> CategoriesClient.as(Role.CUSTOMER);
            case "anonymous" -> CategoriesClient.anonymous();
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }

    private void trackIfCreated(ApiResponse response) {
        if (response.statusCode() == 201) {
            context.track(ResourceType.CATEGORY, response.jsonPath().getString("id"));
        }
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Test Root Cat " + System.nanoTime());
        return payload;
    }
}
