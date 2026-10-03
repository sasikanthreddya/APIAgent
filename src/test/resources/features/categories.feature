# flow-sha: 5326d4674fe0ce47d0238d5109876435e695f36f5f9d7c92cd0203f7981aedc1
@categories
Feature: Categories - catalogue read and admin write

  @smoke
  Scenario: List categories
    When I list all categories
    Then the response status is 200
    And the response body is a non-empty array of categories

  @smoke
  Scenario: Get a category by id
    Given an existing category's id
    When I get that category
    Then the response status is 200
    And the response's id matches the requested category

  Scenario: Getting a category by a nonexistent id is rejected
    When I get a nonexistent category
    Then the response status is 404

  @smoke
  Scenario: Create a root category as admin
    Given a valid category payload
    When I create that category as admin
    Then the response status is 201
    And the created category echoes the submitted name with an auto-generated slug and no parent

  Scenario: Create a category with a parent as admin
    Given I have created a root category as admin
    And a valid category payload with that category as its parent
    When I create that category as admin
    Then the response status is 201
    And the created category's parent id matches the parent

  Scenario Outline: Creating a category without the right role is rejected
    Given a valid category payload
    When I create that category as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                 |
      | anonymous | 401    | Bearer token is required |
      | customer  | 403    | does not permit          |

  Scenario: Creating a category without a name is rejected
    Given an empty category payload
    When I create that category as admin
    Then the response status is 422
    And the response message contains "name: must not be blank"

  Scenario Outline: Creating a category with a single invalid field is rejected
    Given a valid category payload with "<field>" set to "<value>"
    When I create that category as admin
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | field     | value                                | status | fragment                                                          |
      | name      | Hammer                               | 409    | already exists                                                    |
      | parent_id | 00000000-0000-0000-0000-000000000000 | 404    | Parent category 00000000-0000-0000-0000-000000000000 not found   |

  @smoke
  Scenario: Update a category as admin
    Given I have created a root category as admin
    And the category's name is changed
    When I update that category as admin
    Then the response status is 200
    And the updated category reflects the changes

  Scenario Outline: Updating a category without the right role is rejected
    Given I have created a root category as admin
    When I update that category as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                 |
      | anonymous | 401    | Bearer token is required |
      | customer  | 403    | does not permit          |

  Scenario: Updating a category without a name is rejected
    Given I have created a root category as admin
    And the category payload is emptied
    When I update that category as admin
    Then the response status is 422
    And the response message contains "name: must not be blank"

  Scenario: Updating a nonexistent category is rejected
    When I update a nonexistent category as admin
    Then the response status is 404

  @smoke
  Scenario: Delete a category as admin
    Given I have created a root category as admin
    When I delete that category as admin
    Then the response status is 204
    And the category no longer exists

  Scenario Outline: Deleting a category without the right role is rejected
    Given I have created a root category as admin
    When I delete that category as <role>
    Then the response status is <status>
    And the response message contains "<fragment>"

    Examples:
      | role      | status | fragment                 |
      | anonymous | 401    | Bearer token is required |
      | customer  | 403    | does not permit          |

  Scenario: Deleting a nonexistent category is rejected
    When I delete a nonexistent category as admin
    Then the response status is 404

  Scenario: Deleting a category with a child leaves the child orphaned
    Given I have created a root category as admin
    And I have created a category as admin with that category as its parent
    When I delete that category as admin
    Then the response status is 204
    And the child category still exists with a dangling parent reference
