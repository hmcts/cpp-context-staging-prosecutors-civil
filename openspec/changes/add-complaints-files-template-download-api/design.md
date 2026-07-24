## Context

`stagingprosecutorscivil-query-api` currently exposes a single read endpoint, `GET /submissions/{submissionId}`, backed by `CivilProsecutionQueryView` reading from the viewstore. The complaints-files template does not depend on any stored state — it is a fixed CSV shape that callers need before they can submit a complaints file — so it does not fit the existing viewstore-backed query pattern and is served as a static classpath resource instead.

Access control in this module is enforced by a Drools session (`QUERY_API` kie-session, configured in `META-INF/kmodule.xml`) that loads every `.drl` file found under the `accesscontrol` resource package into one `KieBase`. Rules are matched by session, not by file, so which `.drl` file a rule lives in has no effect on runtime behaviour — only rule-name uniqueness across the whole package matters.

## Goals / Non-Goals

**Goals:**
- Let callers download the complaints-files CSV template over HTTP with correct `Content-Type` and `Content-Disposition` headers so browsers/HTTP clients treat it as a file download.
- Gate the endpoint with the same group-based access-control mechanism already used elsewhere in this module.
- Fail gracefully (`404`, not a `500`/uncaught exception) if the packaged CSV resource is ever missing.

**Non-Goals:**
- Generating the CSV template dynamically from schema/config — it is a fixed, hand-maintained file for this change.
- Versioning multiple template variants — there is exactly one template today.
- Changing the existing `/submissions/{submissionId}` endpoint or its access-control rule.

## Decisions

- **Serve the CSV as a classpath resource via `InputStream`, not a viewstore-backed query.** The template has no per-request variability, so there is nothing to query — reading a packaged resource is the simplest correct implementation and avoids an unnecessary DB round-trip.
- **One `.drl` file per query-api module.** Rather than adding a second `.drl` file (`query-complaints-files-template.drl`) alongside the existing one, the new rule was added to the single existing rule file for this module (renamed to `stagingprosecutorscivil-query-api.drl` to reflect that it now holds all query-api access-control rules, not just the charge-prosecution one). This avoids proliferating one-rule-per-file `.drl` files as more query endpoints are added, and matches how the Drools session already merges all `.drl` files in the package at runtime regardless of filename.
- **Restrict access via group membership (`isMemberOfAnyOfTheSuppliedGroups`), not the existing permission-based check (`hasPermission`/`getCivilCasePermission`).** The existing `submission-details` rule checks a `CIVIL_CASE`/`GrantAccess` permission tied to a specific case. The complaints-files template is not case-scoped — it is a general-purpose download available to any caller in the `Non CPS Prosecutors` or `System Users` groups — so a group-membership check is the correct fit rather than reusing the per-case permission check.
- **Return 404 with a plain-text body on a missing resource, rather than letting a `NullPointerException`/`IOException` propagate.** Keeps the failure mode explicit and testable (`shouldReturnNotFoundWhenResourceFileIsMissing`), rather than relying on the framework's generic error handling.

## Risks / Trade-offs

- **[Risk] The CSV template is a static file checked into source control.** If the required column shape changes, the file must be manually updated and re-released — there is no dynamic generation to keep it in sync automatically. → Mitigation: acceptable for now since there is only one template; revisit if multiple template variants or dynamic schemas are needed later.
- **[Risk] Renaming `query-charge-prosecution.drl` to `stagingprosecutorscivil-query-api.drl` touches a file unrelated to this specific endpoint.** → Mitigation: verified via `mvn clean test` that Drools loads rules by KieBase/session, not by filename, so the rename is behaviourally inert; the existing `submission-details` rule and its test are unaffected.

## Migration Plan

1. Add `ComplaintsFilesTemplateQueryApi`, its generated-adapter resource, and the packaged CSV resource.
2. Add the RAML path/mapping so the framework routes `stagingprosecutorscivil.complaints-files-template` to the new resource.
3. Add the access-control rule and `RuleConstants.getComplaintsFilesTemplateGroups()`, appended to the existing query-api `.drl` file.
4. No database/viewstore migration is required — this endpoint reads no persisted state.
5. Rollback is a straight revert of the added files/rule; nothing else depends on this endpoint yet.

## Open Questions

None outstanding.
