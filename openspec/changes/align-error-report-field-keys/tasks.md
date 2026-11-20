## 0. Decide

- [x] 0.1 prosecutioncasefile business-error keys shown as CSV column names (`proposal.md` > Decisions 1-2)
- [x] 0.2 Translate at report time - option A (decision 3)
- [x] 0.3 Unmapped keys keep the raw key (decision 4)
- [x] 0.4 Only complaints-file (CSV) submissions are translated; JSON API submissions keep raw keys (decision 5)
- [x] 0.5 Split `defendant.*`/`offence.*` schema errors into `defendantErrors`; prosecutioncasefile defendant errors keep their defendant reference (decision 6)
- [x] 0.6 Decisions recorded in `proposal.md`, including null references (decision 7) and the shared mapping module (decision 8)

## 1. Mapping (shared module)

- [x] 1.1 List every prosecutioncasefile key (`FieldName` enum + inline keys such as `dateOfBirth`, `offenceCode`, `groupProsecutions.size`) - kept in `ProsecutionCaseFileFieldToCsvColumnTest`
- [x] 1.2 Move `SummonsProsecutionCsvColumns` to `stagingprosecutorscivil-domain-common` (`uk.gov.moj.cpp.staging.civil.common.csv`); add `ProsecutionCaseFileFieldToCsvColumn` there, targeting its constants; command-api and query-view depend on domain-common
- [x] 1.3 Unmapped keys return empty -> raw key kept

## 2. Error report

- [x] 2.1 `SubmissionErrorDetailsCsvBuilder`: translate `key` before writing the `Field` column
- [x] 2.2 `CivilProsecutionQueryView.buildSubmissionErrorDetailsCsvResponse`: passes `Submission.getFileName() != null`
- [x] 2.3 Stored data, events and the JSON `submission-details` response unchanged

## 3. Schema errors as defendant errors

- [x] 3.1 `ComplaintsFileValidationError` carries the row's `defendant.prosecutorDefendantId`; `isDefendantError()` for `defendant.*`/`offence.*` columns
- [x] 3.2 Converter and `SummonsSchemaViolationMapper` set the defendant reference; case/defendant references are null (not `Row <n>`) when the row has none
- [x] 3.3 `CivilProsecutionApi.rejectComplaintsFile` splits errors into `caseErrors` / `defendantErrors`
- [x] 3.4 Schemas: optional `defendantErrors` (`defendant-problem`) on `stagingcivil.command.reject-complaints-file` and `stagingprosecutorscivil.event.complaints-file-validation-failed` (domain-message + listener copies); `caseErrors` no longer required; example payloads updated
- [x] 3.5 Aggregate, handler and listener pass/store `defendantErrors`

## 4. Tests

- [x] 4.1 `SubmissionErrorDetailsCsvBuilderTest`: keys translated; column keys unchanged; unmapped keys raw; non-CSV submissions raw; blank reference
- [x] 4.2 `ProsecutionCaseFileFieldToCsvColumnTest`: fails if a known key is neither mapped nor listed as unmapped, or a mapped column is not a template column
- [x] 4.3 Unit tests for the split and null references (command-api, handler, aggregate, listener)
- [x] 4.4 ITs: `ComplaintsFilesUploadIT` - defendant-column schema error reported as a `Defendant` row; prosecutioncasefile `offence_offenceCode` reported as `offence.offenceDetails.cjsOffenceCode`. `SubmissionErrorDetailsCsvIT` (JSON API) keeps raw keys - unchanged

## 5. Verification

- [x] 5.1 `mvn clean install`
- [x] 5.2 `./runIntegrationTests.sh` (Docker + `CPP_DOCKER_DIR`)
