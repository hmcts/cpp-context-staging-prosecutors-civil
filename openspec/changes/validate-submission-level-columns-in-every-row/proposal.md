## Why

A complaints CSV file is one summons submission: `prosecutingAuthority`, `hearingDetails.courtHearingLocation`, `hearingDetails.dateOfHearing` and `hearingDetails.timeOfHearing` describe the whole file, so the converter reads them from row 1 only. Every row still carries these columns, and the business expects them to hold the same values in every row.

Because the other rows' values are never read, the error report is misleading:

- A file where every row leaves `hearingDetails.timeOfHearing` blank gets **one** `REQUIRED_FIELD_MISSING` error, against row 1's case reference - it reads as a problem with that one case.
- A file where row 57 has a different hearing time or court than row 1 is accepted silently, and every case is listed for row 1's hearing.

## What Changes

- The four submission-level columns are validated on **every** data row during conversion, and each problem is reported against that row's own case reference:
  - `hearingDetails.*`: required, `dateOfHearing` ISO `YYYY-MM-DD`, `timeOfHearing` `HH:MM:SS` (the `hearing-details.json` pattern), `courtHearingLocation` at most 7 characters.
  - `prosecutingAuthority` on rows 2..N: required. (A blank value in row 1 still rejects the whole file, as today.)
- A new error code `INCONSISTENT_VALUE` is reported when a row's value for one of the four columns differs from row 1's value. It is only raised when both values are present and well-formed, so each cell gets at most one error and a malformed row 1 value is reported once (on row 1) rather than against every later row. Hearing times are compared by value (`9:05:00` equals `09:05:00`, as the schema pattern allows both).
- These problems do not drop the row's case from conversion (row 1 already behaves this way), so the case is still schema-validated and its other problems appear in the same report.
- **Unchanged:** once the file passes validation, the submission sent forward is built from row 1's values only.

## Capabilities

### New Capabilities

- `complaints-file-validation`: per-row validation of the submission-level columns of a complaints CSV file.

### Modified Capabilities

None.

## Impact

- `stagingprosecutorscivil-command-api`:
  - `SummonsProsecutionCsvToJsonConverter` - per-row checks for the four columns.
  - `ComplaintsFileValidationError` - new `INCONSISTENT_VALUE` code.
  - `DefaultCommandApiComplaintsFilesResource` - no change: its existing row+field de-duplication already drops the JSON-schema violation for a row 1 hearing value the converter has now reported.
- Error report: files with blank/invalid/inconsistent submission-level columns now get one error per affected row instead of one for the file. `INCONSISTENT_VALUE` is a new value in the `Error Code` column.
- No schema, event, viewstore or downstream (Prosecution Case File) change.
