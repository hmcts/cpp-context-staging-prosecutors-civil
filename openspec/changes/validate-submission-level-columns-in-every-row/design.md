## Context

`SummonsProsecutionCsvToJsonConverter.convert` turns the CSV into one `Summons`: one `prosecutionCases` entry per row, plus a single `prosecutingAuthority` and `hearingDetails` read from row 1. `DefaultCommandApiComplaintsFilesResource` then JSON-schema-validates the converted `Summons` with `stagingcivil.summons` and merges the conversion errors with the schema violations (mapped by `SummonsSchemaViolationMapper`), dropping a schema violation whose row and field were already reported by the conversion.

The JSON schema only ever sees row 1's submission-level values, so per-row checks have to happen in the converter.

## Goals / Non-Goals

**Goals:**
- Report a blank or malformed submission-level value in any row, against that row's case.
- Report any row whose submission-level value differs from row 1's.
- Keep forwarding row 1's values only.

**Non-Goals:**
- Per-case hearing details - the business model stays "one hearing per file".
- Changing what happens when row 1's `prosecutingAuthority` is blank or the first row does not match the template - still a file-level rejection.
- Changing the JSON schemas.

## Decisions

- **Validate in the converter, not the schema mapper.** The schema is applied to the collapsed `Summons`; only the converter sees every row.
- **Compare against row 1's raw trimmed value, except times.** The CSV parser already trims. Dates must be exactly `YYYY-MM-DD`, so text equality is value equality. The `timeOfHearing` pattern allows optional leading zeros, so times are normalised to `HH:MM:SS` before comparing.
- **One error per cell.** `INCONSISTENT_VALUE` is skipped when the row's value is blank or malformed, and when row 1's value is blank or malformed - row 1 already has its own error, and flagging every correct later row would bury it.
- **Don't exclude the case.** Submission-level problems are moved out of the row's error list before the "row has errors" check, as row 1's hearing problems already are, so the case is still converted and schema-validated.
- **Duplicate the two simple `hearing-details.json` rules in Java** (`timeOfHearing` pattern, `courtHearingLocation` max length 7) so every row gets them. The schema still validates row 1's values; the resource's row+field de-duplication keeps a single error for row 1.
- **`prosecutingAuthority` format is not checked per row.** Row 1's value is schema-validated and checked against the calling user's organisation; other rows only need to be present and equal to it.

## Risks / Trade-offs

- **[Risk] The Java copies of the hearing time pattern / location length can drift from `hearing-details.json`.** → Mitigation: `SubmissionLevelColumnRulesTest` loads the schema and fails if they differ.
- **[Trade-off] More errors in the report** for a file with a file-wide mistake (e.g. 101 rows × blank hearing time = 101 errors). This is intended: each case is listed with its own problem.

## Migration Plan

Code-only change in `stagingprosecutorscivil-command-api`; no data migration. Rollback is a revert.

## Open Questions

None.
