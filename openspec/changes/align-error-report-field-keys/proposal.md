## Why

The submission error report (`GET /submissions/{submissionId}` with `Accept: text/csv`, built by
`SubmissionErrorDetailsCsvBuilder`) writes each problem value's `key` into the `Field` column
unchanged. That key comes from two different sources, in two different naming styles:

| Source | Where the key comes from | Example `Field` value |
|---|---|---|
| Schema validation in stagingcivil (CAD-1528/CAD-1966, commit `e6f00f0`) | CSV template column name (`SummonsProsecutionCsvColumns`) | `defendant.individual.dateOfBirth` |
| Business validation in prosecutioncasefile (`public.prosecutioncasefile.*-rejected` events) | prosecutioncasefile `FieldName` enum value (`prosecutioncasefile-domain-aggregate/.../validation/rules/FieldName.java`) | `individual_selfDefinedInformation_dateOfBirth` |

Nothing in stagingcivil translates prosecutioncasefile keys: the event processor and listener store
`caseErrors`/`defendantErrors` exactly as received, and the CSV builder copies `key` verbatim
(`SubmissionErrorDetailsCsvBuilder.java`, `appendProblems`). `SubmissionErrorDetailsCsvIT` asserts
this today: `cad5a01,Defendant,OFFENCE_CODE_NOT_SUPPORTED,offence_offenceCode,AX03547`.

Problem for the user (CAD-1528 AC1: "User will need to refer to this CSV and correct their original
file"): prosecutioncasefile keys such as `individual_selfDefinedInformation_dateOfBirth` do not match
any header in the CSV they uploaded, so they cannot easily find the field to fix.

A single report never mixes the two styles: a file with schema errors is never sent to
prosecutioncasefile. The inconsistency is between reports for different uploads.

### Same field, both sources

| Field | prosecutioncasefile key | stagingcivil key (CSV column) |
|---|---|---|
| Date of birth | `individual_selfDefinedInformation_dateOfBirth` | `defendant.individual.dateOfBirth` |
| Postcode | `individual_personalInformation_address_postcode` | `defendant.address.postcode` |
| Laid date | `offence_laidDate` | `offence.offenceDetails.laidDate` |
| Offence sequence no. | `offence_offenceSequenceNo` | `offence.offenceDetails.offenceSequenceNo` |
| Offence code | `offence_offenceCode` | `offence.offenceDetails.cjsOffenceCode` |
| Parent/guardian DOB | `individual_parentGuardianInformation_dateOfBirth` | `defendant.individual.parentGuardian.individual.dateOfBirth` |
| Name / ID / PNC | `title`, `forename`, `prosecutorDefendantId`, `pncIdentifier` | `defendant.individual.nameDetails.title`, `...forename`, `defendant.prosecutorDefendantId`, `defendant.pncIdentifier` |

## Decisions (agreed 2026-11-20)

1. **Unify the keys:** yes - prosecutioncasefile business-error keys are shown as CSV template
   column names. Schema-error keys (already column names) are unchanged.
2. **Which style wins:** CSV column names (`SummonsProsecutionCsvColumns`).
3. **Where to translate:** **A - at report time** in `SubmissionErrorDetailsCsvBuilder`. Stored data,
   events and the JSON `submission-details` response keep prosecutioncasefile's raw keys; existing
   FAILED submissions are fixed too.
4. **Unmapped keys:** keep the raw key (e.g. `groupProsecutions.size`, `ouCode`, `chargeDate`).
5. **Scope:** only complaints-file (CSV) submissions are translated (`Submission.fileName` present).
   Direct JSON API submissions (`POST /cases`) keep raw keys.
6. **Schema errors as `defendantErrors`:** yes - schema errors in `defendant.*` / `offence.*`
   columns are stored as `defendantErrors` (`Error Type` = `Defendant`, reference =
   `prosecutorDefendantReference` = the row's `defendant.prosecutorDefendantId`). Submission-level
   (`prosecutingAuthority`, `hearingDetails.*`), `case.*` and unreadable-row errors stay case errors
   (reference = `case.urn`). prosecutioncasefile's own defendant errors keep their defendant reference.
7. **Missing references:** when the row has no `case.urn` / `defendant.prosecutorDefendantId`, the
   `prosecutorCaseReference` / `prosecutorDefendantReference` is left out (null) - no made-up
   `Row <n>` label. The report's `Reference` column is blank for such rows.
8. **One shared mapping:** the CSV template column names and the prosecutioncasefile-key ->
   column mapping live together in `stagingprosecutorscivil-domain-common`
   (`uk.gov.moj.cpp.staging.civil.common.csv`), used by both the CSV -> JSON conversion
   (command-api) and the error report (query-view), so the two directions cannot drift apart.

## What Changes

- Move `SummonsProsecutionCsvColumns` from command-api to `stagingprosecutorscivil-domain-common`
  and add `ProsecutionCaseFileFieldToCsvColumn` beside it, mapping every prosecutioncasefile key
  that has a template column to a `SummonsProsecutionCsvColumns` constant.
- `SubmissionErrorDetailsCsvBuilder` maps each `key` through it before writing the `Field` column,
  only for complaints-file submissions; unmapped keys are written unchanged.
- Complaints-file schema errors: `ComplaintsFileValidationError` carries the row's defendant
  reference; `CivilProsecutionApi.rejectComplaintsFile` splits errors into `caseErrors` and
  `defendantErrors`. `stagingcivil.command.reject-complaints-file` and
  `stagingprosecutorscivil.event.complaints-file-validation-failed` gain an optional
  `defendantErrors` (`defendant-problem`); `caseErrors` becomes optional (at least one of the two is
  always present). The listener stores `defendantErrors` in the existing `defendant_errors` column.
- Case/defendant references are null rather than `Row <n>` when the row has none.

## Capabilities

### Modified Capabilities

- Submission error report CSV (`stagingcivil.submission-error-details`): for complaints-file uploads
  the `Field` column shows the user's CSV column name for business errors as well as schema errors,
  and defendant/offence schema errors are reported as `Defendant` rows.

## Impact

- `stagingprosecutorscivil-domain-common`: `SummonsProsecutionCsvColumns` (moved),
  `ProsecutionCaseFileFieldToCsvColumn` (new) + mapping test.
- `stagingprosecutorscivil-command-api`: depends on domain-common; converter/schema-violation mapper
  record the defendant reference and leave missing references null; `CivilProsecutionApi` splits
  case/defendant errors.
- `stagingprosecutorscivil-command-handler`, `-domain-message`, `-domain-aggregate`,
  `-event-listener`: `defendantErrors` added to the reject-complaints-file command and
  complaints-file-validation-failed event (additive; `caseErrors` no longer required).
- `stagingprosecutorscivil-query-view`: depends on domain-common; `SubmissionErrorDetailsCsvBuilder`
  translates keys for CSV submissions; `CivilProsecutionQueryView` passes `fileName != null`.
- `stagingprosecutorscivil-integration-test`: new complaints-upload ITs for a defendant-column
  schema error and for a translated business-error key. `SubmissionErrorDetailsCsvIT` (JSON API
  submission) keeps raw keys and is unchanged.
- The mapping must be kept in step with prosecutioncasefile's `FieldName` enum and the CSV
  template - `ProsecutionCaseFileFieldToCsvColumnTest` fails when a known key is neither mapped nor
  listed as unmapped, or a mapped column is not in the template.
- No change to prosecutioncasefile or the viewstore schema.
