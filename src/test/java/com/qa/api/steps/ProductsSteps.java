package com.qa.api.steps;

import com.qa.api.client.ApiResponse;
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

/** Steps for the Products resource: list, get, search, related, create, update, delete. */
public class ProductsSteps {

    private static final String NONEXISTENT_ID = "00000000-0000-0000-0000-000000000000";

    private final TestContext context;

    private Map<String, Object> productPayload;
    private String productId;
    private String filterCategoryId;

    public ProductsSteps(TestContext context) {
        this.context = context;
    }

    @Given("a valid product payload")
    public void aValidProductPayload() {
        productPayload = validPayload();
    }

    @Given("a valid product payload with {string} set to {string}")
    public void aValidProductPayloadWithFieldSetTo(String field, String value) {
        productPayload = validPayload();
        productPayload.put(field, "price".equals(field) ? Double.parseDouble(value) : value);
    }

    @Given("a product payload with only a name")
    public void aProductPayloadWithOnlyAName() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Incomplete");
        productPayload = payload;
    }

    @Given("a product id from the product list")
    public void aProductIdFromTheProductList() {
        ApiResponse response = ProductsClient.anonymous().list();
        productId = response.jsonPath().getString("data[0].id");
    }

    @Given("a category id from the category list")
    public void aCategoryIdFromTheCategoryList() {
        filterCategoryId = ProductsClient.anyCategoryId();
    }

    @Given("I have created a product as admin")
    public void iHaveCreatedAProductAsAdmin() {
        productPayload = validPayload();
        ApiResponse response = ProductsClient.as(Role.ADMIN).create(productPayload);
        productId = response.jsonPath().getString("id");
        context.track(ResourceType.PRODUCT, productId);
    }

    @Given("the product's name, price and stock quantity are changed")
    public void theProductsFieldsAreChanged() {
        productPayload.put("name", "Updated Hammer");
        productPayload.put("price", 29.99);
        productPayload.put("stock_quantity", 5);
    }

    @When("I list all products")
    public void iListAllProducts() {
        context.setLastResponse(ProductsClient.anonymous().list());
    }

    @When("I get that product")
    public void iGetThatProduct() {
        context.setLastResponse(ProductsClient.anonymous().getById(productId));
    }

    @When("I get a nonexistent product")
    public void iGetANonexistentProduct() {
        context.setLastResponse(ProductsClient.anonymous().getById(NONEXISTENT_ID));
    }

    @When("I search products for {string}")
    public void iSearchProductsFor(String query) {
        context.setLastResponse(ProductsClient.anonymous().search(query));
    }

    @When("I search products with no query parameter")
    public void iSearchProductsWithNoQueryParameter() {
        context.setLastResponse(ProductsClient.anonymous().searchWithoutQuery());
    }

    @When("I get related products for that product")
    public void iGetRelatedProductsForThatProduct() {
        context.setLastResponse(ProductsClient.anonymous().related(productId));
    }

    @When("I filter products by that category")
    public void iFilterProductsByThatCategory() {
        context.setLastResponse(ProductsClient.anonymous().listByCategory(filterCategoryId));
    }

    @When("I create that product as {word}")
    public void iCreateThatProductAsRole(String role) {
        ApiResponse response = clientForRole(role).create(productPayload);
        context.setLastResponse(response);
        trackIfCreated(response);
    }

    @When("I update that product as {word}")
    public void iUpdateThatProductAsRole(String role) {
        context.setLastResponse(clientForRole(role).update(productId, productPayload));
    }

    @When("I update a nonexistent product as admin")
    public void iUpdateANonexistentProductAsAdmin() {
        context.setLastResponse(ProductsClient.as(Role.ADMIN).update(NONEXISTENT_ID, validPayload()));
    }

    @When("I delete that product as {word}")
    public void iDeleteThatProductAsRole(String role) {
        context.setLastResponse(clientForRole(role).delete(productId));
    }

    @When("I delete a nonexistent product as admin")
    public void iDeleteANonexistentProductAsAdmin() {
        context.setLastResponse(ProductsClient.as(Role.ADMIN).delete(NONEXISTENT_ID));
    }

    @Then("the response body is page 1 of a paginated product catalogue")
    public void theResponseBodyIsPage1OfAPaginatedProductCatalogue() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getInt("current_page")).isEqualTo(1);
        Assertions.assertThat(body.getInt("per_page")).isEqualTo(20);
        List<Object> data = body.getList("data");
        Assertions.assertThat(data).isNotEmpty();
        Assertions.assertThat(body.getString("data[0].id")).isNotBlank();
        Assertions.assertThat(body.getString("data[0].category.id")).isNotBlank();
        Assertions.assertThat(body.getString("data[0].brand.id")).isNotBlank();
    }

    @Then("the response body is a paginated list of products")
    public void theResponseBodyIsAPaginatedListOfProducts() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getInt("current_page")).isGreaterThanOrEqualTo(1);
        Assertions.assertThat(body.getList("data")).isNotNull();
    }

    @Then("the response's id matches the requested product")
    public void theResponsesIdMatchesTheRequestedProduct() {
        Assertions.assertThat(context.getLastResponse().jsonPath().getString("id")).isEqualTo(productId);
    }

    @Then("category and brand are present as nested objects")
    public void categoryAndBrandArePresentAsNestedObjects() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("category.id")).isNotBlank();
        Assertions.assertThat(body.getString("brand.id")).isNotBlank();
    }

    @Then("every returned item's name contains {string} regardless of case")
    public void everyReturnedItemsNameContainsRegardlessOfCase(String term) {
        List<String> names = context.getLastResponse().jsonPath().getList("data.name", String.class);
        Assertions.assertThat(names).isNotEmpty();
        Assertions.assertThat(names).allSatisfy(name ->
                Assertions.assertThat(name.toLowerCase()).contains(term.toLowerCase()));
    }

    @Then("every returned item's category id equals the requested category")
    public void everyReturnedItemsCategoryIdEqualsTheRequestedCategory() {
        List<String> categoryIds = context.getLastResponse().jsonPath().getList("data.category.id", String.class);
        Assertions.assertThat(categoryIds).isNotEmpty();
        Assertions.assertThat(categoryIds).allMatch(id -> id.equals(filterCategoryId));
    }

    @Then("the created product echoes the submitted fields")
    public void theCreatedProductEchoesTheSubmittedFields() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("name")).isEqualTo(productPayload.get("name"));
        Assertions.assertThat(body.getString("description")).isEqualTo(productPayload.get("description"));
        Assertions.assertThat(body.getDouble("price")).isEqualTo((Double) productPayload.get("price"));
        Assertions.assertThat(body.getBoolean("is_location_offer")).isEqualTo((Boolean) productPayload.get("is_location_offer"));
        Assertions.assertThat(body.getBoolean("is_rental")).isEqualTo((Boolean) productPayload.get("is_rental"));
        Assertions.assertThat(body.getInt("stock_quantity")).isEqualTo((Integer) productPayload.get("stock_quantity"));
        Assertions.assertThat(body.getBoolean("in_stock")).isTrue();
        Assertions.assertThat(body.getString("category.id")).isEqualTo(productPayload.get("category_id"));
        Assertions.assertThat(body.getString("brand.id")).isEqualTo(productPayload.get("brand_id"));
    }

    @Then("the updated product reflects the changes")
    public void theUpdatedProductReflectsTheChanges() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getString("id")).isEqualTo(productId);
        Assertions.assertThat(body.getString("name")).isEqualTo(productPayload.get("name"));
        Assertions.assertThat(body.getDouble("price")).isEqualTo((Double) productPayload.get("price"));
        Assertions.assertThat(body.getInt("stock_quantity")).isEqualTo((Integer) productPayload.get("stock_quantity"));
    }

    @Then("the product no longer exists")
    public void theProductNoLongerExists() {
        ApiResponse response = ProductsClient.anonymous().getById(productId);
        Assertions.assertThat(response.statusCode()).isEqualTo(404);
    }

    private ProductsClient clientForRole(String role) {
        return switch (role) {
            case "admin" -> ProductsClient.as(Role.ADMIN);
            case "customer" -> ProductsClient.as(Role.CUSTOMER);
            case "anonymous" -> ProductsClient.anonymous();
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }

    private void trackIfCreated(ApiResponse response) {
        if (response.statusCode() == 201) {
            context.track(ResourceType.PRODUCT, response.jsonPath().getString("id"));
        }
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Test Hammer " + System.nanoTime());
        payload.put("description", "grounding test");
        payload.put("price", 19.99);
        payload.put("category_id", ProductsClient.anyCategoryId());
        payload.put("brand_id", ProductsClient.anyBrandId());
        payload.put("is_location_offer", false);
        payload.put("is_rental", false);
        payload.put("stock_quantity", 10);
        return payload;
    }
}
