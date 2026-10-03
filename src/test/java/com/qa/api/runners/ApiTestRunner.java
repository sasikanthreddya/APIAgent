package com.qa.api.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.qa.api.steps",
        // @known-defect scenarios (not tagged @public-api-only) still run by default -
        // they assert the documented current (buggy) behaviour, not the correct one.
        // @public-api-only is excluded by default because the default target
        // (ConfigManager) is toolshop-clone-api, which fixes that specific bug -
        // run those deliberately, see authorization.feature's header comment.
        tags = "not @public-api-only",
        plugin = {
                "pretty",
                "junit:target/cucumber-results.xml",
                "html:target/cucumber-report.html",
                // results land in target/allure-results (src/test/resources/allure.properties)
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        }
)
public class ApiTestRunner extends AbstractTestNGCucumberTests {

    // parallel = true corrupted requests across threads here: REST Assured's
    // io.restassured.RestAssured.given() builder is not safe under Cucumber-TestNG's
    // parallel DataProvider - path segments from one scenario's request leaked into
    // another's (observed: "/carts/{cartId}/carts/<uuid>") and connections failed
    // with "Target host is null". The whole suite runs in well under a minute
    // sequentially; not worth chasing a thread-safe REST Assured client config for that.
    @Override
    @org.testng.annotations.DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
