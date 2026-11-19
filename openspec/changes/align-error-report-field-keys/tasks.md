## 0. Decide (blocking - nothing below starts until these are answered)

- [ ] 0.1 Confirm with BA that prosecutioncasefile business-error keys should be shown as CSV column names in the error report (`proposal.md` > Decisions needed 1-2)
- [ ] 0.2 Choose where to translate: A report time (recommended) / B on receipt / C in prosecutioncasefile (decision 3)
- [ ] 0.3 Agree the rule for prosecutioncasefile keys with no CSV column: raw key / blank / friendly label (decision 4)
- [ ] 0.4 Agree whether direct JSON API submissions (no `fileName`) keep raw keys (decision 5)
- [ ] 0.5 Decide the related question: keep schema errors as case errors, or split `defendant.*`/`offence.*` into `defendantErrors`; and whether prosecutioncasefile defendant errors should show the case URN instead of `prosecutorDefendantReference`
- [ ] 0.6 Record the decisions in `proposal.md` and update the tasks below to match

## 1. Mapping (assumes option A + CSV column names)

- [ ] 1.1 List every prosecutioncasefile `FieldName` value civil summons submissions can produce (`cpp-context-prosecution-casefile`, `prosecutioncasefile-domain-aggregate/.../validation/rules/FieldName.java`, plus inline keys such as `dateOfBirth`, `offenceChargeDate`, `groupProsecutions.size`)
- [ ] 1.2 Add the key -> CSV column mapping in `stagingprosecutorscivil-query-view`, targeting `SummonsProsecutionCsvColumns` names (the constants live in command-api; copy the names or move the constants to a shared module)
- [ ] 1.3 Apply the agreed unmapped-key rule

## 2. Error report

- [ ] 2.1 `SubmissionErrorDetailsCsvBuilder`: translate `key` before writing the `Field` column
- [ ] 2.2 `CivilProsecutionQueryView.buildSubmissionErrorDetailsCsvResponse`: pass whether the submission came from a CSV upload (`Submission.getFileName() != null`) so only CSV submissions are translated (if decision 0.4 says so)
- [ ] 2.3 Leave stored data, events and the JSON `submission-details` response unchanged

## 3. Tests

- [ ] 3.1 `SubmissionErrorDetailsCsvBuilderTest`: prosecutioncasefile keys translated; CSV-column keys from schema errors unchanged; unmapped keys follow the agreed rule; non-CSV submissions keep raw keys (if applicable)
- [ ] 3.2 Mapping test that fails if a known prosecutioncasefile key or a template column is left unmapped
- [ ] 3.3 Update `SubmissionErrorDetailsCsvIT` expected CSV (`offence_offenceCode` -> `offence.offenceDetails.cjsOffenceCode` for a CSV-upload submission) and add an IT for a business error on a complaints-file upload

## 4. Verification

- [ ] 4.1 `mvn clean install`
- [ ] 4.2 `./runIntegrationTests.sh` (Docker + `CPP_DOCKER_DIR`)
