package com.qa.api.context;

import com.qa.api.client.ApiResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Scenario-scoped shared state, constructor-injected into every Steps class by
 * cucumber-picocontainer (a fresh instance per scenario - no cross-scenario leakage).
 *
 * {@code lastResponse} is the one thing every resource's steps need, so it lives
 * here from day one. The cleanup registry ({@code track}/{@code created}) is
 * generic infrastructure for "anything a scenario creates must be deleted
 * afterwards" - it stays empty of resource types until a resource is reviewed
 * and generated (see {@link ResourceType}).
 *
 * Hand-off slots for a specific resource (e.g. "the product this scenario is
 * working with") are added here only once that resource's flow is approved -
 * not before.
 */
public class TestContext {

    private ApiResponse lastResponse;

    private final EnumMap<ResourceType, List<String>> created = new EnumMap<>(ResourceType.class);

    public ApiResponse getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(ApiResponse response) {
        this.lastResponse = response;
    }

    /** Registers something this scenario created so Hooks deletes it afterwards. */
    public void track(ResourceType type, String id) {
        created.computeIfAbsent(type, t -> new ArrayList<>()).add(id);
    }

    public List<String> created(ResourceType type) {
        return created.getOrDefault(type, Collections.emptyList());
    }
}
