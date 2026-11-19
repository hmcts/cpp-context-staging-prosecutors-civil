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

## Decisions needed (blocking - agree with BA before starting)

1. **Unify the keys at all?** The schema-error keys (CSV column names) were accepted as-is on
   2026-11-19; this change is only about prosecutioncasefile's business-error keys.
2. **Which style wins?** Recommended: translate prosecutioncasefile keys **to CSV column names**,
   because those are what the user sees in their file. The reverse is not workable - many CSV
   columns have no prosecutioncasefile `FieldName` equivalent.
3. **Where to translate?** Options:
   - **A. At report time** in `SubmissionErrorDetailsCsvBuilder` (query-view) - stored data stays as
     prosecutioncasefile sent it, so it also fixes existing FAILED submissions; JSON
     `submission-details` responses keep the raw keys. *(Recommended)*
   - **B. On receipt** in the event processor/listener when storing rejection errors - the JSON
     `submission-details` response also shows CSV names, but existing rows are not fixed without a
     catch-up/replay, and the stored data no longer matches what prosecutioncasefile sent.
   - **C. In prosecutioncasefile** - not recommended: its keys are shared with other channels (SPI,
     CPS), not just civil bulk uploads.
4. **Unmapped keys:** when a prosecutioncasefile key has no CSV column (e.g. `groupProsecutions.size`,
   `ouCode`, `courtReceivedToCode`), keep the raw key, leave `Field` blank, or use a friendly label?
5. **Scope of the report:** only complaints-file (CSV) submissions have CSV column names to map to.
   Decide whether direct JSON API submissions (`POST /cases`, no `fileName`) keep raw keys - the
   builder can tell them apart because only CSV uploads carry `fileName`.

### Related open question (decide at the same time)

- **Schema errors as `defendantErrors`?** All stagingcivil schema errors are currently stored as case
  errors (`Error Type` = `Case`, reference = `case.urn`), even for `defendant.*` / `offence.*`
  columns, whereas prosecutioncasefile reports defendant/offence problems as `defendantErrors`
  (`Error Type` = `Defendant`, reference = `prosecutorDefendantReference`). Current view: keep them
  as case errors, because CAD-1528 AC1 says the reference column shows the prosecutor *case*
  reference and every CSV row is exactly one case + one defendant. If the BA wants consistency, the
  better fix may be the opposite direction - show the case URN for prosecutioncasefile's defendant
  errors too.

## What Changes (assuming option A + CSV column names)

- Add a prosecutioncasefile-key -> CSV-column mapping (e.g. `ProsecutionCaseFileFieldToCsvColumn`) in
  `stagingprosecutorscivil-query-view`, covering every `FieldName` value that civil summons
  submissions can produce.
- `SubmissionErrorDetailsCsvBuilder` maps the `key` through it before writing the `Field` column,
  only for submissions that came from a complaints CSV (`fileName` present), applying the
  unmapped-key rule from decision 4.
- Leave stored data, events and the JSON `submission-details` response unchanged.

## Capabilities

### Modified Capabilities

- Submission error report CSV (`stagingcivil.submission-error-details`): the `Field` column shows
  the user's CSV column name for business errors as well as schema errors.

## Impact

- `stagingprosecutorscivil-query-view`: `SubmissionErrorDetailsCsvBuilder`, new mapping class, tests.
- `stagingprosecutorscivil-query-api` / `CivilProsecutionQueryView`: pass whether the submission came
  from a CSV upload (`fileName`) into the builder.
- `stagingprosecutorscivil-integration-test`: `SubmissionErrorDetailsCsvIT` expected CSV changes
  (`offence_offenceCode` -> `offence.offenceDetails.cjsOffenceCode`, if the submission has a
  `fileName`).
- The mapping must be kept in step with prosecutioncasefile's `FieldName` enum and the CSV template
  (`SummonsProsecutionCsvColumns`) - add a test that fails when a template column or known key is
  unmapped.
- No change to prosecutioncasefile, events, schemas or the viewstore.
