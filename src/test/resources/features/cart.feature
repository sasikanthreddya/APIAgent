# flow-sha: fbe30a187319b3bd23f728adfa6b35707444fb8e2d43b958ff00871ada55ceb0
@cart
Feature: Cart - anonymous shopping cart

  @smoke
  Scenario: Create a cart
    When I create a cart
    Then the response status is 201
    And the cart is empty

  @smoke
  Scenario: Get a cart by id
    Given I have created a cart
    When I get that cart
    Then the response status is 200
    And the cart is empty

  Scenario: Getting a cart by a nonexistent id is rejected
    When I get a nonexistent cart
    Then the response status is 404

  @smoke
  Scenario: Add an item to a cart
    Given I have created a cart
    When I add an existing product to that cart with quantity 2
    Then the response status is 200
    And the cart has that product with quantity 2 and the correct line total

  Scenario: Adding the same product again merges the quantity
    Given I have created a cart
    And I add an existing product to that cart with quantity 2
    When I add an existing product to that cart with quantity 1
    Then the response status is 200
    And the cart has that product with quantity 3 and the correct line total

  Scenario: Adding an item to a nonexistent cart is rejected
    When I add an existing product to a nonexistent cart
    Then the response status is 404

  Scenario: Adding a nonexistent product to a cart is rejected
    Given I have created a cart
    When I add a nonexistent product to that cart
    Then the response status is 404

  Scenario: Adding an item without a valid quantity is rejected
    Given I have created a cart
    When I add an existing product to that cart with quantity 0
    Then the response status is 422
    And the response message contains "quantity: must be greater than or equal to 1"

  Scenario: Adding an item without a product id is rejected
    Given I have created a cart
    When I add an item with no product id to that cart
    Then the response status is 422
    And the response message contains "product_id: must not be blank"

  Scenario: Adding a quantity beyond the product's stock is not rejected
    Given I have created a cart
    When I add an existing product to that cart with quantity 9999
    Then the response status is 200
    And the cart has that product with quantity 9999 and the correct line total

  @smoke
  Scenario: Update an item's quantity in a cart
    Given I have created a cart
    And I add an existing product to that cart with quantity 2
    When I update that product's quantity in the cart to 5
    Then the response status is 200
    And the cart has that product with quantity 5 and the correct line total

  Scenario: Updating the quantity of a product not in the cart is rejected
    Given I have created a cart
    When I update a nonexistent product's quantity in the cart to 3
    Then the response status is 404

  @smoke
  Scenario: Remove an item from a cart
    Given I have created a cart
    And I add an existing product to that cart with quantity 2
    When I remove that product from the cart
    Then the response status is 200
    And the cart is empty

  Scenario: Removing a product not in the cart is rejected
    Given I have created a cart
    When I remove a product that isn't in the cart
    Then the response status is 404

  @smoke
  Scenario: Delete a cart
    Given I have created a cart
    When I delete that cart
    Then the response status is 204
    And the cart no longer exists

  Scenario: Deleting a nonexistent cart is rejected
    When I delete a nonexistent cart
    Then the response status is 404
