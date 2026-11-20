## 1. Endpoint implementation

- [x] 1.1 Add `ComplaintsFilesTemplateQueryApi` in `stagingprosecutorscivil-query-api`, streaming `complaints-files-template.csv` from the classpath with `Content-Type: text/csv` and `Content-Disposition: attachment; filename="complaints-files-template.csv"`, returning `404` if the resource is missing
- [x] 1.2 Add the generated-adapter resource `DefaultQueryApiComplaintsFilesTemplateResource` delegating to `ComplaintsFilesTemplateQueryApi`
- [x] 1.3 Add `complaints-files-template.csv` static resource to `src/main/resources`
- [x] 1.4 Add `/complaints-files-template` path to `staging_prosecutors_civil_query_api.raml.raml`, mapped to message name `stagingprosecutorscivil.complaints-files-template` with `responseType: text/csv`

## 2. Access control

- [x] 2.1 Add `getBulkCasePermission()` to `RuleConstants`, mirroring `getCivilCasePermission()` but with `withObject("BULK_CASE")`
- [x] 2.2 Add a Drools rule for `stagingprosecutorscivil.complaints-files-template` using `hasPermission`, mirroring the `submission-details` rule's shape
- [x] 2.3 Add the rule to the existing query-api `.drl` file rather than creating a second file, keeping one `.drl` per module
- [x] 2.4 Rename `query-charge-prosecution.drl` to `stagingprosecutorscivil-query-api.drl` to reflect it now holds all query-api access-control rules
- [x] 2.5 Add `QueryComplaintsFilesTemplateRulesTest` verifying the rule grants access when the caller holds the `BULK_CASE`/`GrantAccess` permission

## 3. Tests

- [x] 3.1 Add `ComplaintsFilesTemplateQueryApiTest` covering the success path (headers + content) and the missing-resource `404` path
- [x] 3.2 Add `DefaultQueryApiComplaintsFilesTemplateResourceTest` verifying the resource delegates to `ComplaintsFilesTemplateQueryApi`
- [x] 3.3 Add `ComplaintsFilesTemplateIT` integration test verifying the endpoint returns `200`, `text/csv`, an `attachment` disposition with the expected filename, and CSV content
- [x] 3.4 Add `getComplaintsFilesTemplate()` helper to `StagingProsecutorsCivilUtils` for the integration test

## 4. Verification

- [x] 4.1 Run `mvn clean test -pl stagingprosecutorscivil-query/stagingprosecutorscivil-query-api -Dtest=QueryComplaintsFilesTemplateRulesTest` to confirm the access-control rule loads correctly after merging into the renamed `.drl` file (a stale `target/classes` copy of the old `query-complaints-files-template.drl` initially caused a false duplicate-rule error; clean build resolved it)
- [ ] 4.2 Run `./runIntegrationTests.sh` (requires Docker + `CPP_DOCKER_DIR`) to confirm `ComplaintsFilesTemplateIT` passes end-to-end
