# Sprint input - review gate (legacy, superseded 2026-09-24)

**Superseded.** As of 2026-09-24, a resource's request/expected-response
pairs are embedded directly in its `flows/<resource>.md` - one file to
review instead of a flow doc plus two folders of per-scenario JSON. See
`CLAUDE.md`'s review gate section and the `flow-doc-gen` skill
(`.claude/skills/flow-doc-gen/`). `flows/auth.md` and `flows/users.md` have
both been migrated to that shape (auth generated, users still awaiting
review), and cross-checked field-by-field against every file below - the
JSON files in this folder are no longer canonical for either and cover no
scenario that isn't already in `flows/*.md`.

Kept only for historical reference, reorganized 2026-09-24 into one
subfolder per resource (`auth/`, `users/`) instead of the original flat
shared `requests/`/`expected-responses/` pair - still useful to browse
per-resource, just no longer the active convention. Do not add new files
here; a new resource's draft goes entirely into `flows/<resource>.md`.

---

This is where an API group's contract gets agreed before any code exists.
Nothing here is executed; nothing downstream (flow -> feature -> steps ->
client) gets generated until you review these files and say "generate".

## Process

1. Agent reads the clone's live Swagger (`http://localhost:8070/swagger-ui/index.html`,
   raw spec at `http://localhost:8070/v3/api-docs`) and exercises the real
   endpoint with curl - never guesses a field or status code.
2. Agent writes a flow doc (`flows/<resource>.md`) plus one request file and
   one matching expected-response file per scenario in the two folders below.
3. You review all three. Correct anything that's wrong.
4. You say "generate" (for this resource, or by name). Only then does the
   agent write the `.feature`, step definitions and client method, wire up
   `config.properties` keys if new ones are needed, and run the suite green.

## Folders (legacy shape, kept as-is per resource)

| Folder | Holds |
|---|---|
| `<resource>/requests/` | One JSON file per scenario: method, path, auth role, body |
| `<resource>/expected-responses/` | The matching file: status code and response body, captured from the live clone |

Filenames pair up 1:1 within a resource's own subfolder:
`auth/requests/auth-01-register-full-profile.json` <->
`auth/expected-responses/auth-01-register-full-profile.json`. The number is
the scenario's position in the resource's flow doc. (Originally these two
folders were flat and shared across every resource, distinguished only by
filename prefix - reorganized into one subfolder per resource on
2026-09-24 for easier browsing; content unchanged.)

## Request file shape

```json
{
  "method": "POST",
  "path": "/users/register",
  "auth": "none",
  "body": { "...": "..." }
}
```

`auth` is one of `none`, `customer`, `admin`, or `token:<placeholder>` when the
scenario needs a token that isn't one of the two seeded accounts (e.g. the
account this same scenario just registered).

## Expected-response file shape

```json
{
  "status": 201,
  "body": { "...": "..." },
  "dynamic_fields": ["id", "created_at"],
  "notes": "one line on anything non-obvious, e.g. why a field is null"
}
```

`dynamic_fields` lists keys in `body` that will differ on every real run
(ids, timestamps, tokens) - the generated assertions check their type/shape,
not their exact value. Everything else in `body` is asserted for an exact
match unless the flow doc says otherwise.
