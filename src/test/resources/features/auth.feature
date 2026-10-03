# flow-sha: ef22ef8b1daed7646759986bf15f6aa96d537ad75dd3a61d7499214b175ae9a9
@auth
Feature: Auth - registration and login

  @smoke
  Scenario: Register with a full profile
    Given a unique new user with a full profile
    When I register that user
    Then the response status is 201
    And the registered user's role is "CUSTOMER"
    And the registered user's email matches the request
    And every submitted profile field is echoed back exactly

  @smoke
  Scenario: Register with only the required fields
    Given a unique new user with only the required fields
    When I register that user
    Then the response status is 201
    And the registered user's role is "CUSTOMER"
    And the registered user's email matches the request
    And phone, dob and every address field in the response are null

  Scenario: Registering with an already-used email is rejected
    Given I have registered a unique user
    When I register another user with that same email
    Then the response status is 409
    And the response message contains "already exists"
    And the response message names the conflicting email

  Scenario: Registering without the required fields is rejected
    Given a new user's registration payload with only an email
    When I register that user
    Then the response status is 422
    And the response message contains "first_name: must not be blank"
    And the response message contains "last_name: must not be blank"
    And the response message contains "password: must not be blank"

  Scenario Outline: Registering with a single invalid field is rejected
    Given a unique new user with "<field>" set to "<value>"
    When I register that user
    Then the response status is 422
    And the response message contains "<fragment>"

    Examples:
      | field    | value        | fragment                                   |
      | password | short        | password: size must be between 8           |
      | email    | not-an-email | email: must be a well-formed email address |

  @smoke
  Scenario: A newly registered user can log in
    Given I have registered a unique user
    When I log in with that user's email and password
    Then the response status is 200
    And the response body has a valid access token, bearer token type and a positive expiry

  Scenario: Login with the wrong password is rejected
    Given I have registered a unique user
    When I log in with that user's email and an incorrect password
    Then the response status is 401
    And the response message is "Invalid email or password"

  Scenario: Login with an email that was never registered is rejected
    Given an email that has never been registered
    When I log in with that email and any password
    Then the response status is 401
    And the response message is "Invalid email or password"

  Scenario: Login without a password is rejected
    Given I have registered a unique user
    When I log in with that user's email and no password
    Then the response status is 422
