# flow-sha: dff26660cd44341031a86d834977ef8656b2564a7a42187b29573bc218153290
@users
Feature: Users - own profile

  @smoke
  Scenario: A logged-in user can fetch their own profile
    Given I have registered a unique user and logged in
    When I fetch my own profile
    Then the response status is 200
    And my profile's id, email, first_name and last_name match the registration

  Scenario: Fetching the profile without a token is rejected
    When I fetch my own profile with no token
    Then the response status is 401
    And the response message contains "Bearer token is required"

  Scenario: Fetching the profile with a malformed token is rejected
    When I fetch my own profile with a malformed token
    Then the response status is 401
    And the response message contains "Bearer token is required"
