---
name: code-gen
description: "Generates the .feature file, step definitions, resource client, config keys, and ResourceType/Hooks cleanup wiring for a resource whose flows/<resource>.md has already been reviewed and approved. Use when the user says \"generate <resource>\" (or just \"generate\" for the one resource currently under review), or invokes \"/code-gen <resource>\" by name. Refuses to run against a flow that hasn't been reviewed."
---

## What this does

Runs gate step 4 from CLAUDE.md's review gate for one resource: the flow doc
is already approved, now build it. Produces the `.feature` file, step
definitions, client method(s), any new `config.properties` keys, and the
`ResourceType`/`Hooks` wiring for whatever that resource's scenarios create -
then runs the suite green. One resource per invocation - never touch another
resource's files while doing this, even if its flow also looks ready.

## Input

The resource name (matching a `flows/<resource>.md` file). If the user just
said "generate" with a resource already under discussion in this
conversation, use that one. If it's ambiguous, ask which resource rather
than guessing.

## Preconditions - check before writing anything

- `flows/<resource>.md` exists and reads as a finished draft (has scenarios,
  each with a Request/Expected-response JSON pair - the format
  `flow-doc-gen` produces).
- The user actually approved it (said "generate" for this resource, or
  invoked this skill naming it). If neither happened, stop and ask - don't
  infer approval from the draft merely existing or looking complete.

## Steps

1. **Re-read `CLAUDE.md`** for the current conventions - transport
   (`Http`/`ApiResponse`/`BaseClient`), the `AuthClient.tokenFor(Role)`
   seeded-token cache (once Auth exists, every other client's `as(Role)`
   goes through that, never a second login-caching mechanism), cleanup
   registry (`TestContext`/`ResourceType`/`Hooks`), and existing generated
   resources under `src/main/java/.../client/` and
   `src/test/java/.../steps/` so new code matches their shape exactly.

2. **Read `flows/<resource>.md` fully.** It is the sole source of truth for
   this pass - not the schema, not memory. Before trusting an embedded
   request/expected-response pair, sanity-check it against the live clone
   with curl, same as grounding does: re-verify anything whose capture date
   is more than a few days old, or that looks inconsistent with a sibling
   scenario in the same file (a field present in one similar response and
   silently missing in another is exactly the kind of drift worth
   re-checking - see the 2026-09-24 Auth session, where a re-check caught
   the 401 login body actually including `timestamp` when the draft said it
   didn't). If you find drift, fix the flow doc's embedded JSON to match
   live reality, tell the user exactly what changed and why, and only then
   continue - don't silently generate against a description you know is
   wrong, and don't stop and wait for a second approval for a
   ground-truth correction (only for an actual design/scope question).

3. **Compute the flow-sha**: sha256 of `flows/<resource>.md`'s exact
   content, for the `.feature` file's `# flow-sha: <hex>` header line
   (see `scripts/check-flow-drift.mjs`).

4. **Write `src/test/resources/features/<resource>.feature`** - one
   Scenario per flow scenario, in the same order, Gherkin phrased close to
   the flow's numbered steps. Tag `@smoke` for every `[smoke]`-tagged
   scenario and a resource tag (e.g. `@products`) on the Feature. Reuse
   `CommonSteps`' existing status-assertion steps rather than redefining
   them; only add a `@known-defect` step if the flow doc documents one with
   a verified date and status.

   Collapse a cluster of flow scenarios into one `Scenario Outline` +
   `Examples` **only** when they share the exact same step shape and the
   *same arity of assertions* - not just the same final status code. The
   test is: can every row be expressed as "take the same baseline payload,
   override exactly one field, check exactly one output" (or an equally
   uniform pattern)? If yes, one Outline row per case. If a case needs a
   different number of fields changed, a nested/structured body, multiple
   assertions where siblings need only one, or a multi-request sequence
   (create-then-act), it does not fit - leave it as its own `Scenario`
   rather than padding empty Examples columns or joining assertions into a
   delimited cell to force the fit. (Auth precedent: "short password" and
   "malformed email" fit - one field overridden, one message back, so they
   became one Outline. "Missing required fields" looked similar at a
   glance - same 422 status - but omits three fields and checks three
   message fragments, a different shape, so it stayed a separate Scenario.
   Don't let a shared status code alone justify merging.)

5. **Write/extend `src/main/java/com/qa/api/client/<Resource>Client.java`**
   extending `BaseClient` - one method per endpoint the scenarios actually
   call, no assertions, URLs built via `ConfigManager`/`url(...)` only.
   Follow the established convention: `XClient.anonymous()`,
   `XClient.as(Role.ADMIN)` / `XClient.as(Role.CUSTOMER)` (via
   `AuthClient.tokenFor(Role)`), `new XClient(token)` for an arbitrary
   token. Don't reimplement token caching or login.

   If a scenario needs a read-only lookup against another tag's *already
   seeded* data purely for test setup (e.g. an existing category/brand id
   to build a valid product payload) - and that other resource hasn't been
   through its own review/generate pass yet - add that one lookup call
   directly to the current resource's own client rather than standing up a
   client (or any part of a flow/feature) for the other resource ahead of
   its own gate. This mirrors `AuthClient.deleteRegisteredUser`, which
   calls `DELETE /users/{id}` (tagged `Users`, not `Auth`) because Auth
   needed it for cleanup before a `UsersClient` existed (see
   `AuthClient.java`'s class javadoc) - and the same call decided for
   Products' dependency on Categories/Brands (2026-09-24): a direct
   `GET /categories`/`GET /brands` lookup inside `ProductsClient`, not a
   pre-built `CategoriesClient`/`BrandsClient`. This is for *setup only* -
   never assert against the borrowed endpoint's response beyond extracting
   the id you need, and never let it grow into create/update/delete for
   that other resource; that only happens once that resource is reviewed
   and generated on its own pass.

6. **Write `src/test/java/com/qa/api/steps/<Resource>Steps.java`** - step
   definitions matching the feature's Gherkin text exactly, assertions live
   here only, no raw URL building, constructor-injected `TestContext`.

7. **Add `ResourceType` case(s) and the matching `Hooks` branch(es)** only
   for what this resource's own scenarios create - wired to that resource's
   client's delete method (or an existing one, e.g. `AuthClient`, if that's
   what actually owns the delete endpoint, same reasoning as Auth's `USER`
   cleanup). Use `System.nanoTime()`-suffixed names, never a fixed string,
   for anything a scenario creates. No hardcoded ids - derive them from a
   list call or from what the scenario itself created.

8. **Add any new `config.properties` keys** this resource's client needs
   (never a hardcoded secret - seeded creds only, `-D`-overridable).

9. **Build and run**: `mvn clean test-compile`, then
   `mvn test -Dcucumber.filter.tags="@<resource-tag>"` for this resource's
   scenarios, then the full `mvn test` to confirm nothing else regressed,
   then `node scripts/check-flow-drift.mjs --write` to pin the feature's
   flow-sha. Only this step - actual code generation - warrants a full
   regression run; a flow-doc-only correction (step 2 above) does not.

10. **Report plainly**: pass/fail counts, what was generated, and confirm
    no test data was left behind (spot-check via an admin list call if the
    resource's cleanup is new). Don't report success if anything failed or
    if cleanup couldn't be verified.
