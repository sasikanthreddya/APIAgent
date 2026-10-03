package com.qa.api.steps;

import com.qa.api.context.TestContext;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;
import org.testng.Assert;

/**
 * Assertions shared across every feature - status code checks only. Anything that
 * reads a resource-specific body belongs in that resource's Steps class.
 */
public class CommonSteps {

    private final TestContext context;

    public CommonSteps(TestContext context) {
        this.context = context;
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expected) {
        Assertions.assertThat(context.getLastResponse().statusCode())
                .as("status, body was: %s", context.getLastResponse().asString())
                .isEqualTo(expected);
    }

    /**
     * Only for genuinely target-dependent status codes (e.g. a step that must hold on
     * both the clone and the public API). Never to paper over an unverified code.
     */
    @Then("the response status is {int} or {int}")
    public void theResponseStatusIsEitherOf(int a, int b) {
        int actual = context.getLastResponse().statusCode();
        Assertions.assertThat(actual).isIn(a, b);
    }

    /**
     * The equivalent of the TypeScript project's test.fail(): passes while the
     * documented defect is present, and fails loudly - distinctly from an ordinary
     * assertion failure - if the API's behaviour has changed either way. Only ever
     * used for a bug verified directly and dated in the flow's Markdown.
     */
    @Then("the response status should be {int} \\(known defect: API currently returns {int})")
    public void theResponseStatusShouldBeKnownDefect(int correctStatus, int defectStatus) {
        int actual = context.getLastResponse().statusCode();
        if (actual == defectStatus) {
            return; // documented defect still present - this is the expected-to-fail state
        }
        if (actual == correctStatus) {
            Assert.fail("DEFECT APPEARS FIXED: API now returns " + correctStatus + " as it should. "
                    + "Remove the @known-defect handling from the feature and its flow.");
        }
        Assert.fail("Unexpected status " + actual + " - neither the correct " + correctStatus
                + " nor the documented defect " + defectStatus);
    }
}
