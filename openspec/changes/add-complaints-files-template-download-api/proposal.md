## Why

Civil authority prosecutors submit complaints files via a CSV upload, but there is no way for them to obtain the correctly-shaped CSV template from the service — they currently have to source it out-of-band. A new `stagingprosecutorscivil-query-api` endpoint is needed so the template can be downloaded directly from the API.

## What Changes

- Add a new `GET /complaints-files-template` endpoint to `stagingprosecutorscivil-query-api` that streams a static `complaints-files-template.csv` resource back to the caller as a `text/csv` attachment download.
- Register the new query message `stagingprosecutorscivil.complaints-files-template` in the RAML contract, mapped to `responseType: text/csv`.
- Add an access-control rule restricting the endpoint to members of the `Non CPS Prosecutors` or `System Users` groups (`RuleConstants.getComplaintsFilesTemplateGroups()`), added to the existing `stagingprosecutorscivil-query-api.drl` rule file rather than a new `.drl` file, per team convention of one `.drl` per module/session.
- Return `404 Not Found` if the CSV resource is ever missing from the classpath, rather than failing with a raw exception.

## Capabilities

### New Capabilities

- `complaints-files-template-download-api`: A query-api endpoint that returns the static complaints-files CSV template as a downloadable attachment, gated by group-based access control.

### Modified Capabilities

None.

## Impact

- **Affected code**:
  - `stagingprosecutorscivil-query-api`: new `ComplaintsFilesTemplateQueryApi`, new generated-adapter implementation `DefaultQueryApiComplaintsFilesTemplateResource`, new CSV resource `complaints-files-template.csv`, RAML addition, access-control rule addition to `stagingprosecutorscivil-query-api.drl`, new `getComplaintsFilesTemplateGroups()` in `RuleConstants`.
  - `stagingprosecutorscivil-integration-test`: new `ComplaintsFilesTemplateIT`, new `getComplaintsFilesTemplate()` helper in `StagingProsecutorsCivilUtils`.
- **External API consumers**: civil authority prosecutor systems gain a new, additive `GET /complaints-files-template` endpoint; no existing endpoint changes shape or behaviour.
- **Access control**: only callers in the `Non CPS Prosecutors` or `System Users` groups can call the endpoint; other authenticated callers receive an access-denied outcome from the Drools rule engine.
