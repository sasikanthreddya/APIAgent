package com.qa.api.steps;

import com.qa.api.client.ApiResponse;
import com.qa.api.client.CartClient;
import com.qa.api.client.ProductsClient;
import com.qa.api.context.ResourceType;
import com.qa.api.context.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import org.assertj.core.api.Assertions;

import java.util.HashMap;
import java.util.Map;

/** Steps for the Cart resource: create, get, add/update/remove item, delete. */
public class CartSteps {

    private static final String NONEXISTENT_ID = "00000000-0000-0000-0000-000000000000";
    private static final CartClient CLIENT = new CartClient();

    private final TestContext context;

    private String cartId;
    private String productId;
    private double productPrice;

    public CartSteps(TestContext context) {
        this.context = context;
    }

    @Given("I have created a cart")
    public void iHaveCreatedACart() {
        ApiResponse response = CLIENT.create();
        cartId = response.jsonPath().getString("id");
        context.track(ResourceType.CART, cartId);
    }

    @When("I create a cart")
    public void iCreateACart() {
        ApiResponse response = CLIENT.create();
        context.setLastResponse(response);
        cartId = response.jsonPath().getString("id");
        context.track(ResourceType.CART, cartId);
    }

    @When("I get that cart")
    public void iGetThatCart() {
        context.setLastResponse(CLIENT.get(cartId));
    }

    @When("I get a nonexistent cart")
    public void iGetANonexistentCart() {
        context.setLastResponse(CLIENT.get(NONEXISTENT_ID));
    }

    @When("I add an existing product to that cart with quantity {int}")
    public void iAddAnExistingProductToThatCartWithQuantity(int quantity) {
        if (productId == null) {
            pickAnExistingProduct();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("product_id", productId);
        body.put("quantity", quantity);
        context.setLastResponse(CLIENT.addItem(cartId, body));
    }

    @When("I add an existing product to a nonexistent cart")
    public void iAddAnExistingProductToANonexistentCart() {
        pickAnExistingProduct();
        Map<String, Object> body = new HashMap<>();
        body.put("product_id", productId);
        body.put("quantity", 1);
        context.setLastResponse(CLIENT.addItem(NONEXISTENT_ID, body));
    }

    @When("I add a nonexistent product to that cart")
    public void iAddANonexistentProductToThatCart() {
        Map<String, Object> body = new HashMap<>();
        body.put("product_id", NONEXISTENT_ID);
        body.put("quantity", 1);
        context.setLastResponse(CLIENT.addItem(cartId, body));
    }

    @When("I add an item with no product id to that cart")
    public void iAddAnItemWithNoProductIdToThatCart() {
        context.setLastResponse(CLIENT.addItem(cartId, new HashMap<>()));
    }

    @When("I update that product's quantity in the cart to {int}")
    public void iUpdateThatProductsQuantityInTheCartTo(int quantity) {
        Map<String, Object> body = new HashMap<>();
        body.put("product_id", productId);
        body.put("quantity", quantity);
        context.setLastResponse(CLIENT.updateQuantity(cartId, body));
    }

    @When("I update a nonexistent product's quantity in the cart to {int}")
    public void iUpdateANonexistentProductsQuantityInTheCartTo(int quantity) {
        Map<String, Object> body = new HashMap<>();
        body.put("product_id", NONEXISTENT_ID);
        body.put("quantity", quantity);
        context.setLastResponse(CLIENT.updateQuantity(cartId, body));
    }

    @When("I remove that product from the cart")
    public void iRemoveThatProductFromTheCart() {
        context.setLastResponse(CLIENT.removeItem(cartId, productId));
    }

    @When("I remove a product that isn't in the cart")
    public void iRemoveAProductThatIsntInTheCart() {
        context.setLastResponse(CLIENT.removeItem(cartId, NONEXISTENT_ID));
    }

    @When("I delete that cart")
    public void iDeleteThatCart() {
        context.setLastResponse(CLIENT.delete(cartId));
    }

    @When("I delete a nonexistent cart")
    public void iDeleteANonexistentCart() {
        context.setLastResponse(CLIENT.delete(NONEXISTENT_ID));
    }

    @Then("the cart is empty")
    public void theCartIsEmpty() {
        JsonPath body = context.getLastResponse().jsonPath();
        Assertions.assertThat(body.getList("cart_items")).isEmpty();
        Assertions.assertThat(body.getDouble("total")).isEqualTo(0.0);
    }

    @Then("the cart has that product with quantity {int} and the correct line total")
    public void theCartHasThatProductWithQuantityAndTheCorrectLineTotal(int quantity) {
        JsonPath body = context.getLastResponse().jsonPath();
        double expectedLineTotal = Math.round(productPrice * quantity * 100.0) / 100.0;
        Assertions.assertThat(body.getString("cart_items[0].product_id")).isEqualTo(productId);
        Assertions.assertThat(body.getInt("cart_items[0].quantity")).isEqualTo(quantity);
        Assertions.assertThat(body.getDouble("cart_items[0].line_total")).isEqualTo(expectedLineTotal);
        Assertions.assertThat(body.getDouble("total")).isEqualTo(expectedLineTotal);
    }

    @Then("the cart no longer exists")
    public void theCartNoLongerExists() {
        ApiResponse response = CLIENT.get(cartId);
        Assertions.assertThat(response.statusCode()).isEqualTo(404);
    }

    private void pickAnExistingProduct() {
        ApiResponse response = ProductsClient.anonymous().list();
        productId = response.jsonPath().getString("data[0].id");
        productPrice = response.jsonPath().getDouble("data[0].price");
    }
}
