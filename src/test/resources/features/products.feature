# flow-sha: f09de312978db9b6808b36249ab26afac883ace5cd5929e5d55d7cb0dba6c64b
@products
Feature: Products - catalogue read and admin write

  @smoke
  Scenario: List products
    When I list all products
    Then the response status is 200
    And the response body is page 1 of a paginated product catalogue

  @smoke
  Scenario: Get a product by id
    Given a product id from the product list
    When I get that product
    Then the response status is 200
    And the response's id matches the requested product
    And category and brand are present as nested objects

  Scenario: Getting a product by a nonexistent id is rejected
    When I get a nonexistent product
    Then the response status is 404

  @smoke
  Scenario: Search products by name
    When I search products for "hammer"
    Then the response status is 200
    And every returned item's name contains "hammer" regardless of case

  Scenario: Searching without a query parameter is rejected
    When I search products with no query parameter
    Then the response status is 400

  Scenario: Get related products
    Given a product id from the product list
    When I get related products for that product
    Then the response status is 200
    And the response body is a paginated list of products

  Scenario: Filter the product list by category
    Given a category id from the category list
    When I filter products by that category
    Then the response status is 200
    And every returned item's category id equals the requested category

  @smoke
  Scenario: Create a product as admin
    Given a valid product payload
    When I create that product as admin
    Then the response status is 201
    And the created product echoes the submitted fields

  Scenario Outline: Creating a product without the right role is rejected
    Given a valid product payload
    When I create that product as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                  |
      | anonymous | 401    | Bearer token is required  |
      | customer  | 403    | does not permit           |

  Scenario: Creating a product without the required fields is rejected
    Given a product payload with only a name
    When I create that product as admin
    Then the response status is 422
    And the response message contains "category_id: must not be blank"
    And the response message contains "brand_id: must not be blank"
    And the response message contains "description: must not be blank"
    And the response message contains "is_rental: must not be null"
    And the response message contains "stock_quantity: must not be null"
    And the response message contains "is_location_offer: must not be null"
    And the response message contains "price: must not be null"

  Scenario Outline: Creating a product with a single invalid field is rejected
    Given a valid product payload with "<field>" set to "<value>"
    When I create that product as admin
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | field       | value                                | status | fragment                                       |
      | price       | -5                                   | 422    | price: must be greater than or equal to 0      |
      | category_id | 00000000-0000-0000-0000-000000000000 | 404    | Category 00000000-0000-0000-0000-000000000000 not found |
      | brand_id    | 00000000-0000-0000-0000-000000000000 | 404    | Brand 00000000-0000-0000-0000-000000000000 not found    |

  @smoke
  Scenario: Update a product as admin
    Given I have created a product as admin
    And the product's name, price and stock quantity are changed
    When I update that product as admin
    Then the response status is 200
    And the updated product reflects the changes

  Scenario Outline: Updating a product without the right role is rejected
    Given I have created a product as admin
    When I update that product as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                  |
      | anonymous | 401    | Bearer token is required  |
      | customer  | 403    | does not permit           |

  Scenario: Updating a nonexistent product is rejected
    When I update a nonexistent product as admin
    Then the response status is 404

  @smoke
  Scenario: Delete a product as admin
    Given I have created a product as admin
    When I delete that product as admin
    Then the response status is 204
    And the product no longer exists

  Scenario Outline: Deleting a product without the right role is rejected
    Given I have created a product as admin
    When I delete that product as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                  |
      | anonymous | 401    | Bearer token is required  |
      | customer  | 403    | does not permit           |

  Scenario: Deleting a nonexistent product is rejected
    When I delete a nonexistent product as admin
    Then the response status is 404
