# flow-sha: 60a7245aad6e6fc8ed9599a1f98d0c1f78d1913d198ffb2e5d4872609e4f01e7
@brands
Feature: Brands - catalogue read and admin write

  @smoke
  Scenario: List brands
    When I list all brands
    Then the response status is 200
    And the response body is a non-empty array of brands

  @smoke
  Scenario: Get a brand by id
    Given an existing brand's id
    When I get that brand
    Then the response status is 200
    And the response's id matches the requested brand

  Scenario: Getting a brand by a nonexistent id is rejected
    When I get a nonexistent brand
    Then the response status is 404

  @smoke
  Scenario: Create a brand as admin
    Given a valid brand payload
    When I create that brand as admin
    Then the response status is 201
    And the created brand echoes the submitted name with an auto-generated slug

  Scenario Outline: Creating a brand without the right role is rejected
    Given a valid brand payload
    When I create that brand as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                 |
      | anonymous | 401    | Bearer token is required |
      | customer  | 403    | does not permit          |

  Scenario: Creating a brand without a name is rejected
    Given an empty brand payload
    When I create that brand as admin
    Then the response status is 422
    And the response message contains "name: must not be blank"

  Scenario: Creating a brand with a duplicate name is rejected
    Given a brand payload with the name "ForgeFlex Tools"
    When I create that brand as admin
    Then the response status is 409
    And the response message contains "already exists"

  @smoke
  Scenario: Update a brand as admin
    Given I have created a brand as admin
    And the brand's name is changed
    When I update that brand as admin
    Then the response status is 200
    And the updated brand reflects the changes

  Scenario Outline: Updating a brand without the right role is rejected
    Given I have created a brand as admin
    When I update that brand as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                 |
      | anonymous | 401    | Bearer token is required |
      | customer  | 403    | does not permit          |

  Scenario: Updating a brand without a name is rejected
    Given I have created a brand as admin
    And the brand payload is emptied
    When I update that brand as admin
    Then the response status is 422
    And the response message contains "name: must not be blank"

  Scenario: Updating a nonexistent brand is rejected
    When I update a nonexistent brand as admin
    Then the response status is 404

  @smoke
  Scenario: Delete a brand as admin
    Given I have created a brand as admin
    When I delete that brand as admin
    Then the response status is 204
    And the brand no longer exists

  Scenario Outline: Deleting a brand without the right role is rejected
    Given I have created a brand as admin
    When I delete that brand as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                 |
      | anonymous | 401    | Bearer token is required |
      | customer  | 403    | does not permit          |

  Scenario: Deleting a nonexistent brand is rejected
    When I delete a nonexistent brand as admin
    Then the response status is 404

  @known-defect
  Scenario: Deleting a brand referenced by a product
    Given I have created a brand as admin
    And I have created a product as admin using that brand
    When I delete that brand as admin
    Then the response status should be 409 (known defect: API currently returns 500)
    And the brand still exists
