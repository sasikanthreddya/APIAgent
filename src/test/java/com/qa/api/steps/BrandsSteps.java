package com.qa.api.steps;

import com.qa.api.client.ApiResponse;
import com.qa.api.client.BrandsClient;
import com.qa.api.client.ProductsClient;
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

/** Steps for the Brands resource: list, get, create, update, delete. */
public class BrandsSteps {

    private static final String NONEXISTENT_ID = "00000000-0000-0000-0000-000000000000";

    private final TestContext context;

    private Map<String, Object> brandPayload;
    private String brandId;

    public BrandsSteps(TestContext context) {
        this.context = context;
    }

    @Given("a valid brand payload")
    public void aValidBrandPayload() {
        brandPayload = validPayload();
    }

    @Given("an empty brand payload")
    public void anEmptyBrandPayload() {
        brandPayload = new HashMap<>();
    }

    @Given("the brand payload is emptied")
    public void theBrandPayloadIsEmptied() {
        brandPayload = new HashMap<>();
    }

    @Given("a brand payload with the name {string}")
    public void aBrandPayloadWithTheName(String name) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        brandPayload = payload;
    }

    @Given("the brand's name is changed")
    public void theBrandsNameIsChanged() {
        brandPayload.put("name", "Updated Test Brand " + System.nanoTime());
    }

    @Given("an existing brand's id")
    public void anExistingBrandsId() {
        ApiResponse response = BrandsClient.anonymous().list();
        brandId = response.jsonPath().getString("[0].id");
    }

    @Given("I have created a brand as admin")
    public void iHaveCreatedABrandAsAdmin() {
        brandPayload = validPayload();
        ApiResponse response = BrandsClient.as(Role.ADMIN).create(brandPayload);
        brandId = response.jsonPath().getString("id");
        context.track(ResourceType.BRAND, brandId);
    }

    @Given("I have created a product as admin using that brand")
    public void iHaveCreatedAProductAsAdminUsingThatBrand() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Temp Product " + System.nanoTime());
        payload.put("description", "defect-verification product");
        payload.put("price", 1.0);
        payload.put("category_id", ProductsClient.anyCategoryId());
        payload.put("brand_id", brandId);
        payload.put("is_location_offer", false);
        payload.put("is_rental", false);
        payload.put("stock_quantity", 1);
        ApiResponse response = ProductsClient.as(Role.ADMIN).create(payload);
        context.track(ResourceType.PRODUCT, response.jsonPath().getString("id"));
    }

    @When("I list all brands")
    public void iListAllBrands() {
        context.setLastResponse(BrandsClient.anonymous().list());
    }

    @When("I get that brand")
    public void iGetThatBrand() {
        context.setLastResponse(BrandsClient.anonymous().getById(brandId));
    }

    @When("I get a nonexistent brand")
    public void iGetANonexistentBrand() {
        context.setLastResponse(BrandsClient.anonymous().getById(NONEXISTENT_ID));
    }

    @When("I create that brand as {word}")
    public void iCreateThatBrandAsRole(String role) {
        ApiResponse response = clientForRole(role).create(brandPayload);
        context.setLastResponse(response);
        trackIfCreated(response);
    }

    @When("I update that brand as {word}")
    public void iUpdateThatBrandAsRole(String role) {
        context.setLastResponse(clientForRole(role).update(brandId, brandPayload));
    }

    @When("I update a nonexistent brand as admin")
    public void iUpdateANonexistentBrandAsAdmin() {
        context.setLastResponse(BrandsClient.as(Role.ADMIN).update(NONEXISTENT_ID, validPayload()));
    }

    @When("I delete that brand as {word}")
    public void iDeleteThatBrandAsRole(String role) {
        context.setLastResponse(clientForRole(role).delete(brandId));
    }

    @When("I delete a nonexistent brand as admin")
    public void iDeleteANonexistentBrandAsAdmin() {
        context.setLastResponse(BrandsClient.as(Role.ADMIN).delete(NONEXISTENT_ID));
    }

    @Then("the response body is a non-empty array of brands")
    public void theResponseBodyIsANonEmptyArrayOfBrands() {
        JsonPath body = context.getLastResponse().jsonPath();
        List<Object> brands = body.getList("$");
        Assertions.assertThat(brands).isNotEmpty();
        Assertions.assertThat(body.getString("[0].id")).isNotBlank();
        Assertions.assertThat(body.getString("[0].name")).isNotBlank();
        Assertions.assertThat(body.getString("[0].slug")).isNotBlank();
    }

    @Then("the response's id matches the requested brand")
    public void theResponsesIdMatchesTheRequestedBrand() {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("id")).isEqualTo(brandId);
    }

    @Then("the created brand echoes the submitted name with an auto-generated slug")
    public void theCreatedBrandEchoesTheSubmittedNameWithAnAutoGeneratedSlug() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("name")).isEqualTo(brandPayload.get("name"));
        Assertions.assertThat(body.getString("slug")).isNotBlank();
    }

    @Then("the updated brand reflects the changes")
    public void theUpdatedBrandReflectsTheChanges() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("id")).isEqualTo(brandId);
        Assertions.assertThat(body.getString("name")).isEqualTo(brandPayload.get("name"));
    }

    @Then("the brand no longer exists")
    public void theBrandNoLongerExists() {
        ApiResponse response = BrandsClient.anonymous().getById(brandId);
        Assertions.assertThat(response.statusCode()).isEqualTo(404);
    }

    @Then("the brand still exists")
    public void theBrandStillExists() {
        ApiResponse response = BrandsClient.anonymous().getById(brandId);
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
    }

    private BrandsClient clientForRole(String role) {
        return switch (role) {
            case "admin" -> BrandsClient.as(Role.ADMIN);
            case "customer" -> BrandsClient.as(Role.CUSTOMER);
            case "anonymous" -> BrandsClient.anonymous();
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }

    private void trackIfCreated(ApiResponse response) {
        if (response.statusCode() == 201) {
            context.track(ResourceType.BRAND, response.jsonPath().getString("id"));
        }
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Test Brand " + System.nanoTime());
        return payload;
    }
}
