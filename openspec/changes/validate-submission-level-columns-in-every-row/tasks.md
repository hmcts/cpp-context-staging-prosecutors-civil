## 1. Converter

- [x] 1.1 Add `INCONSISTENT_VALUE` to `ComplaintsFileValidationError`
- [x] 1.2 In `SummonsProsecutionCsvToJsonConverter.convert`, validate the hearing columns on every row (required, ISO date, `HH:MM:SS` time, location max 7) and `prosecutingAuthority` on rows 2..N (required)
- [x] 1.3 Report `INCONSISTENT_VALUE` for a row whose submission-level value differs from row 1's (both present, no other error on the cell)
- [x] 1.4 Keep submission-level problems from excluding the row's case; keep building `hearingDetails` / `prosecutingAuthority` from row 1 only

## 2. Tests

- [x] 2.1 `SummonsProsecutionCsvToJsonConverterTest`: blank in every row, inconsistent values, invalid values in a later row, blank later-row `prosecutingAuthority`, blank row 1 with later rows set - inputs in `src/test/resources/complaints-files/*.csv`
- [x] 2.2 Keep existing converter tests with a malformed row 1 value free of `INCONSISTENT_VALUE` for later rows; add a leading-zero time test
- [x] 2.3 Add `SubmissionLevelColumnRulesTest` checking the copied hearing rules against `hearing-details.json`
- [x] 2.4 Add ITs: hearing details blank in every row; inconsistent submission-level columns
- [x] 2.5 Update `DefaultCommandApiComplaintsFilesResourceTest`: a malformed hearing date in every row is now reported on every row

## 3. Verification

- [x] 3.1 `mvn test -pl stagingprosecutorscivil-command/stagingprosecutorscivil-command-api`
- [x] 3.2 Run the converter on `03CV1100050_101_cases.csv`: 101 `REQUIRED_FIELD_MISSING` errors each for `hearingDetails.courtHearingLocation` and `hearingDetails.timeOfHearing` (1111 errors in total, was 911)
- [x] 3.3 Run `./runIntegrationTests.sh`: 69 ITs, 0 failures (1 pre-existing skip in `OtherProsecutionIT`)
- [ ] 3.4 Upload the file end to end on a deployed environment and check the downloaded error report
