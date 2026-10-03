package com.qa.api.steps;

import com.qa.api.client.AuthClient;
import com.qa.api.client.BrandsClient;
import com.qa.api.client.CartClient;
import com.qa.api.client.CategoriesClient;
import com.qa.api.client.ProductsClient;
import com.qa.api.client.Role;
import com.qa.api.context.ResourceType;
import com.qa.api.context.TestContext;
import io.cucumber.java.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes everything a scenario tracked via {@code TestContext.track()}, in
 * {@link ResourceType}'s declaration order, using whichever client that
 * resource needs.
 *
 * Gains one branch per resource, wired to that resource's client, exactly
 * when that resource is approved - the log line in the default case is a
 * safety net so a future resource that forgets its cleanup branch fails
 * loudly in test output instead of silently leaking data.
 */
public class Hooks {

    private static final Logger log = LoggerFactory.getLogger(Hooks.class);

    private final TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @After
    public void cleanUpCreatedResources() {
        for (ResourceType type : ResourceType.values()) {
            for (String id : context.created(type)) {
                switch (type) {
                    case USER -> AuthClient.deleteRegisteredUser(id);
                    case PRODUCT -> ProductsClient.as(Role.ADMIN).delete(id);
                    case CATEGORY -> CategoriesClient.as(Role.ADMIN).delete(id);
                    case BRAND -> BrandsClient.as(Role.ADMIN).delete(id);
                    case CART -> new CartClient().delete(id);
                    default -> log.warn("Tracked {} {} but no cleanup handler is wired in Hooks yet - "
                            + "add one when this resource's client is generated", type, id);
                }
            }
        }
    }
}
