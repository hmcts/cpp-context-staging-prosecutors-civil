## ADDED Requirements

### Requirement: Submission-level columns are validated in every row
The system SHALL validate `hearingDetails.courtHearingLocation`, `hearingDetails.dateOfHearing` and `hearingDetails.timeOfHearing` in every data row of an uploaded complaints CSV file, and `prosecutingAuthority` in every data row after the first, reporting each problem against that row's row number and `case.urn`.

#### Scenario: Hearing columns blank in every row
- **WHEN** a complaints file has `hearingDetails.timeOfHearing` blank in all of its rows
- **THEN** the error report has one `REQUIRED_FIELD_MISSING` error for `hearingDetails.timeOfHearing` per row, each with that row's case reference

#### Scenario: Malformed hearing value in a later row
- **WHEN** row 3 has `hearingDetails.timeOfHearing` `9am`, `hearingDetails.courtHearingLocation` longer than 7 characters, or `hearingDetails.dateOfHearing` not in `YYYY-MM-DD`
- **THEN** row 3 gets `INVALID_VALUE` (time, location) or `INVALID_DATE_FORMAT` (date) for that column, with the row's value

#### Scenario: Prosecuting authority blank in a later row
- **WHEN** row 2 has a blank `prosecutingAuthority` and row 1 does not
- **THEN** row 2 gets `REQUIRED_FIELD_MISSING` for `prosecutingAuthority`

#### Scenario: Problems do not drop the case
- **WHEN** a row's only problems are in submission-level columns
- **THEN** that row's case is still converted and JSON-schema-validated, so its other problems are reported in the same upload

### Requirement: Submission-level columns hold the same value in every row
The system SHALL report an `INCONSISTENT_VALUE` error for a row whose `prosecutingAuthority`, `hearingDetails.courtHearingLocation`, `hearingDetails.dateOfHearing` or `hearingDetails.timeOfHearing` value differs from row 1's value for the same column.

#### Scenario: A later row has a different hearing time
- **WHEN** row 1 has `hearingDetails.timeOfHearing` `09:05:00` and row 2 has `10:00:00`
- **THEN** row 2 gets `INCONSISTENT_VALUE` for `hearingDetails.timeOfHearing` with value `10:00:00`, and row 1 gets no error

#### Scenario: Nothing to compare with
- **WHEN** row 1's value for a column is blank or malformed, or the later row's value is blank or malformed
- **THEN** no `INCONSISTENT_VALUE` error is raised for that cell - only the blank/malformed error

#### Scenario: Same time written with and without a leading zero
- **WHEN** row 1 has `hearingDetails.timeOfHearing` `9:05:00` and row 2 has `09:05:00`
- **THEN** no error is raised

### Requirement: Only row 1's submission-level values are sent forward
The system SHALL build the submitted summons' `prosecutingAuthority` and `hearingDetails` from row 1's values only.

#### Scenario: Consistent file
- **WHEN** every row of a complaints file has the same valid submission-level values and no other problems
- **THEN** the file is accepted and the summons sent forward carries those values once, from row 1
