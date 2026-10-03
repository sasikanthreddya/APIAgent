---
name: flow-doc-gen
description: "Grounds one resource against the running toolshop-clone-api's live Swagger + curl and drafts flows/<resource>.md - the single review-gate artifact for that resource (flow steps and every scenario's request/expected-response JSON, embedded inline). Use when the user names a service/resource to draft or ground (e.g. \"/flow-doc-gen Products\", \"draft the Cart flow\", \"ground Orders\"). Writes nothing else - no .feature, steps, client, or config. Stops for review."
---

## What this does

Runs gate steps 1-2 from CLAUDE.md's review gate for one resource: ground live,
then draft. Produces exactly one file, `flows/<resource>.md`, and stops. It
never writes a `.feature` file, step definitions, a client, or touches
`config.properties`, `ResourceType`, or `Hooks` - that's `code-gen`'s job,
only after the user reviews this file and says "generate".

This replaces the old two-folder `sprint-input/` convention (separate
`requests/*.json` + `expected-responses/*.json` per scenario). Same
precision, one file: each scenario's request/expected-response JSON now
lives inline in the flow doc, right under its numbered steps, instead of in
two separate files per scenario. `sprint-input/` is fully retired - every
resource that ever used it has since been migrated to this inline shape.
Never create anything under `sprint-input/` for a new resource.

## Input

The resource/service name (e.g. "Products", "Cart", "Orders"). If not
supplied, ask which one - match it against a tag in the live Swagger
(`GET /v3/api-docs`), not a guess. If the name doesn't match any tag
exactly, list the tags that exist and ask the user to pick one rather than
assuming.

## Steps

1. **Re-read `CLAUDE.md`** in the project root before doing anything else -
   target base URL, seeded creds, and all seven non-negotiable rules apply
   here exactly as they do to a manual pass. In particular: never write a
   field, status code, or error message from memory, from the schema alone,
   or from the public Toolshop API - only from what curl actually returns
   this run.

2. **Fetch the live spec**: `curl <api.baseUrl>/v3/api-docs` (base URL from
   `src/test/resources/config.properties` unless the user says otherwise).
   Filter to the paths/operations whose Swagger `tags` match the requested
   resource. Note the schema's response section is unreliable (springdoc
   defaults most operations to `200`) - it tells you shapes and field
   constraints, never the real status code.

3. **Exercise every matching operation live with curl** - at least one
   positive case per operation, plus whichever of the project's standard
   negative cases actually apply to it: 401 (no token), 403 (wrong role),
   404, 409, 422, 400 (business-rule rejection). Skip a negative case
   entirely rather than inventing one that doesn't apply to that operation.
   Use a `System.nanoTime()`-style unique suffix for anything you create
   during grounding, and note whether curl's own cleanup is your
   responsibility (it usually isn't - grounding calls aren't scenario
   fixtures, but don't leave obvious junk data behind either; delete what
   you created via curl once you've captured its response).

4. **Only if the user reports (or you notice) a genuinely confirmed bug**
   that would need `@known-defect` handling: verify it directly with curl,
   note the exact observed status/body and today's date. Don't add
   known-defect scenarios speculatively.

5. **Write `flows/<resource-lowercase>.md`** - the *only* file this skill
   produces. Follow the shape already established in `flows/auth.md`:

   ```markdown
   Service Type: <Resource> (<METHOD path>, <METHOD path>, ...)

   Grounded <date> against the running toolshop-clone-api (<base url>), via
   its live Swagger (`/v3/api-docs`) and direct curl calls - not from the
   OpenAPI schema alone. Every request/expected-response pair below was
   captured from a real call.

   Request contract (<RequestSchemaName>, from the live schema):
     <field>  <type, constraints>  - required/optional (validation -> status)
     ...

   Response contract (<ResponseSchemaName>):
     <field>, <field>, ...

   ## Background
   <auth/setup every scenario in this file needs, or "(none - ...)">

   ## Scenario: <name>  [smoke]
   1) <step>
   2) <step>
   ...

   Request:
   ```json
   { "method": "...", "path": "...", "auth": "none|customer|admin|token:<placeholder>", "body": { ... } }
   ```

   Expected response:
   ```json
   {
     "status": <int>,
     "body": { ... exactly as returned ... },
     "dynamic_fields": ["id", "created_at", "..."],
     "notes": "one line on anything non-obvious - captured live <date>"
   }
   ```
   ```

   Repeat the `## Scenario` block (numbered steps + Request + Expected
   response) for every scenario. `dynamic_fields` lists keys in `body` that
   differ on every real run (ids, timestamps, tokens) - everything else is
   an exact-match assertion unless the scenario's steps say otherwise.
   `auth` is `none`, `customer`, `admin`, or `token:<placeholder>` for a
   token the scenario creates for itself (e.g. a user it just registered).

6. **Stop.** Report the file path and a one-line scenario count/summary.
   Tell the user to review it and correct anything wrong, then say
   "generate <resource>" (or invoke `/code-gen <resource>`) when ready. Do
   not proceed to `code-gen`'s job yourself, even if the draft looks
   obviously correct.
