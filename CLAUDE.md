# API Automation Agent (Java) - Toolshop

Java/Cucumber/TestNG API automation for the self-hosted
[toolshop-clone-api](../ToolshopCloneApi) (`localhost:8070`). MCP and the
clone's own Swagger ground the agent while it drafts and then writes
flows/features/steps; Jenkins runs plain compiled TestNG/Cucumber with **no
agent and no LLM in the loop**.

## The review gate - read this first, every time

Nothing gets built without going through this gate. No exceptions, no
"while I'm at it."

1. **Ground.** Read the clone's live Swagger
   (`http://localhost:8070/swagger-ui/index.html`, raw spec at
   `http://localhost:8070/v3/api-docs`) for the resource in question, and
   exercise its real endpoints with curl - positive and negative cases.
   Never write a field, status code or error message from memory or from
   the schema alone (springdoc's schema section is not reliable - see below).
2. **Draft, don't build.** Write a flow doc (`flows/<resource>.md`) with,
   for every scenario in it, a request JSON block and a matching
   expected-response JSON block embedded right under that scenario's
   numbered steps (see any generated `flows/*.md` for the shape - request
   and expected response inline, `dynamic_fields` + `notes` same as before).
   This is the only output of this step: one file, no `.feature`, no step
   definition, no client method. (An older two-folder `sprint-input/`
   convention preceded this - see `sprint-input/README.md` - superseded,
   left in place only for a resource drafted before the change.)
3. **Stop and wait.** The user reviews the flow doc and corrects anything
   wrong. Nothing proceeds until they say "generate" (for that resource, or
   by name).
4. **Generate.** Only then: the `.feature` file (from the flow, 1:1), the
   step definitions, the client method (extending `BaseClient`, no
   assertions in it), any new `config.properties` keys it needs, and the
   `ResourceType` case + `Hooks` branch if it creates something that must be
   cleaned up. Run the suite green before reporting done.

Steps 1-2 and step 4 can be run as the `flow-doc-gen` and `code-gen` skills
respectively (`/flow-doc-gen <resource>`, `/code-gen <resource>`) - they
follow this same gate, including the stop at step 3; they don't collapse it.

One resource (or one cohesive slice of a resource, matching the clone's own
Swagger tags) per pass through this gate. Never pre-build the next one.

## Target

`http://localhost:8070` (`ConfigManager`, default in `config.properties`).
This project's only source of truth is the **running clone**: its live
Swagger and its actual responses. There is no other spec file in this repo -
a static copy invites exactly the drift that caused problems before, so the
`toolshop-clone` MCP server in `.mcp.json` points at the clone's live
`/v3/api-docs`, not a checked-in file.

The clone's schema's response section is not reliable on its own: springdoc
documents `200` for almost every operation, while the live API answers `201`
for register/create/checkout and `204` for delete. Always verify the real
status and body with curl; the schema tells you shapes and field
constraints (types, `minLength`, required-ness), not status codes.

## Current state (2026-10-04)

Six resources generated and green: Auth, Users (own-profile slice only),
Products, Categories, Brands, Cart. **89 scenarios, `mvn test` all passing.**
Nothing else drafted yet.

| Resource | Flow | Feature | Scenarios | Notes |
|---|---|---|---|---|
| Auth | `flows/auth.md` | `auth.feature` | 10 | register + login |
| Users | `flows/users.md` | `users.feature` | 3 | `GET /users/me` only - list/get/update/delete deliberately deferred |
| Products | `flows/products.md` | `products.feature` | 22 | full catalogue CRUD, admin-gated writes |
| Categories | `flows/categories.md` | `categories.feature` | 20 | includes a verified orphaned-parent-category behavior |
| Brands | `flows/brands.md` | `brands.feature` | 18 | includes one `@known-defect` (deleting a referenced brand 500s, should be 409) |
| Cart | `flows/cart.md` | `cart.feature` | 16 | fully anonymous, no auth on any endpoint |

Not yet drafted: Invoices, and the rest of Users (list/get/update/delete -
Phase 6, deliberately last per the clone's own `docs/TESTING-ORDER.md`).

| Path | State |
|---|---|
| `src/main/java/.../client/Http.java`, `ApiResponse.java`, `BaseClient.java`, `Role.java` | Generic transport plumbing, resource-agnostic, done |
| `src/main/java/.../client/{Auth,Users,Products,Categories,Brands,Cart}Client.java` | One per generated resource, see table above |
| `src/main/java/.../config/ConfigManager.java` | Reads `config.properties`, `-D` overrides it, done |
| `src/test/java/.../context/TestContext.java` | Generic scenario state + cleanup registry, done |
| `src/test/java/.../context/ResourceType.java` | `USER`, `PRODUCT`, `CATEGORY`, `BRAND`, `CART` - declaration order is cleanup order (`PRODUCT` before `BRAND` matters: Brands' known-defect scenario needs its product cleaned up first) |
| `src/test/java/.../steps/CommonSteps.java` | Generic status assertions (incl. known-defect helper), done |
| `src/test/java/.../steps/Hooks.java` | Cleanup loop with a branch per `ResourceType` above |
| `src/test/java/.../steps/{Auth,Users,Products,Categories,Brands,Cart}Steps.java` | One per generated resource |
| `src/test/java/.../runners/ApiTestRunner.java` | Generic Cucumber-TestNG runner |
| `sprint-input/` | Legacy - superseded by inline JSON in `flows/*.md`, kept only for historical reference |
| `.claude/skills/flow-doc-gen/`, `.claude/skills/code-gen/` | Skills that run gate steps 1-2 and step 4 respectively (see the review gate above) |
| Everything else (client/steps/`.feature` per remaining resource) | Does not exist yet - generated only after "generate" |

## Non-negotiable rules

1. **Never invent an endpoint, field, or status code.** Every one must come
   from the live clone (Swagger + curl), never guessed, never carried over
   from the real public Toolshop API (different product, different bugs).
2. **Flows are the source of truth, not the .feature files.** Write or edit
   `flows/*.md` first (plain numbered steps, `[smoke]`/`[known-defect]` tags
   in brackets same as BookMyShowAgent's flows). Only then hand-write the
   matching `.feature`, and run `node scripts/check-flow-drift.mjs --write`
   to re-pin its flow-sha header.
3. **Never put an LLM in the execution path.** Tests are committed Java.
   MCP and manual curl are authoring aids only, never a runtime dependency.
4. **Clean up what you create.** Anything a scenario creates must be
   registered with `TestContext.track(ResourceType.X, id)`; `Hooks` deletes
   everything in `ResourceType` declaration order. Both gain a case/branch
   only when that resource is generated - never speculatively. Use a
   `System.nanoTime()`-suffixed name/email, never a fixed string.
5. **No hardcoded ids.** Derive ids at runtime from a list call or from
   something the scenario itself created.
6. **A `@known-defect` scenario documents a real, verified bug** - it does
   not exist to weaken an assertion you didn't want to fix. Only add one
   after confirming the behaviour directly with curl, and record the date
   and the actual observed status in the flow's Markdown.
7. **No secrets in source.** Credentials come from `config.properties`,
   overridable with `-D` (`ConfigManager`), with the clone's seeded accounts
   as defaults.

## HTTP transport - read before touching `client/`

`client/Http.java` uses REST Assured's `given()/when()/then()` directly. An
earlier session had swapped this to `java.net.http.HttpClient` after
reporting `IllegalStateException: Target host is null` under this machine's
Maven-launched Surefire; re-tested 2026-09-23 (repeated full runs) and it did
not reproduce. Every client returns `ApiResponse`
(`.statusCode()`/`.jsonPath()`/`.asString()`) regardless of the transport
underneath. If that failure ever resurfaces, capture the exact stack trace
and machine details before reaching for a workaround - don't silently
re-swap the transport.

## Layout

| Path | Holds |
|---|---|
| `flows/*.md` | The review gate *and* the source of truth once approved - plain numbered steps per resource, with each scenario's request/expected-response JSON embedded inline |
| `sprint-input/` | Legacy - the pre-2026-09-24 two-folder convention (separate request/expected-response files per scenario). Superseded by embedding JSON directly in `flows/*.md`; left in place only for a resource drafted before the change, never used for a new one |
| `src/test/resources/features/` | Gherkin, generated 1:1 from an approved flow |
| `src/test/resources/config.properties` | Base URL + seeded creds, `-D`-overridable |
| `src/test/java/.../steps/` | Step definitions, generated per resource - assertions live here only |
| `src/test/java/.../context/` | `TestContext` (scenario state + cleanup registry), `ResourceType` (cleanup order) |
| `src/main/java/.../client/` | `Http`, `ApiResponse`, `BaseClient`, `Role` - one more client class per generated resource, no assertions |
| `src/main/java/.../config/ConfigManager.java` | Loads `config.properties`, `-D` overrides |
| `scripts/check-flow-drift.mjs` | Fails the build if a flow changed but its `.feature` didn't |

**Clients never assert. Steps never build raw URLs.** Every generated client
extends `BaseClient` and offers the same three entry points:
`XClient.anonymous()`, `XClient.as(Role.ADMIN)` (a cached seeded token, once
Auth exists) and `new XClient(token)` (an arbitrary token).

## Conventions

- Tag fast, high-value scenarios `@smoke`; narrow a run with
  `-Dcucumber.filter.tags="@smoke"`. It **replaces** `ApiTestRunner`'s
  default `tags` expression entirely rather than composing with it.
- Cover the negative cases next to their positive counterpart: 401 (no
  token), 403 (wrong role), 404, 409, 422, and 400 for business-rule
  rejections - each verified live, never assumed.
- `CommonSteps.theResponseStatusIsEitherOf` (`"the response status is {int}
  or {int}"`) exists only for a status genuinely confirmed to vary by
  environment - never to paper over one that hasn't been checked.

## Commands

```bash
mvn clean test                                                       # full suite, against the clone
mvn test -Dcucumber.filter.tags="@smoke"                             # @smoke only
mvn test -Dapi.baseUrl=http://localhost:8091                         # against a different environment
node scripts/check-flow-drift.mjs                                    # verify flows match features
node scripts/check-flow-drift.mjs --write                            # re-pin after an intentional change
mvn allure:serve                                                     # Allure report from target/allure-results
```
