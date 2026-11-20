## Why

The `charge-prosecution` and `summons-prosecution` request schemas (and the internal referral schemas they `$ref`) carry several fields that are not defined anywhere in the **Common Platform Civil Interface (CPCI) External API Specification** (v1.4, dated 2026-07-03). These extra fields were never part of the agreed contract with civil authority prosecutors and should be removed so the API surface matches the published specification exactly.

## What Changes

- **BREAKING**: Remove `aliases` from `individual.json` — the PDF's entire "Alias Array" field table is struck through (deprecated), and `additionalProperties: false` means any caller currently sending this field will start being rejected.
- **BREAKING**: Remove `bailConditions` from `individual.json` — not present anywhere in the PDF's Defendant Related Fields table.
- **BREAKING**: Remove `numPreviousConvictions` from `defendant-details.json` — not present anywhere in the PDF's Defendant Related Fields table.
- **BREAKING**: Remove `aliasOrganisationNames` from `organisation.json` — not present anywhere in the PDF's Case/Prosecution or Defendant tables.
- **BREAKING**: Remove `backDuty`, `backDutyDateFrom`, `backDutyDateTo` from `offence-details.json` — not present in the PDF's Offence Related Fields table or example payload.
- **BREAKING**: Remove `vehicleMake`, `vehicleRegistrationMark` from `offence-details.json` — not present in the PDF's Offence Related Fields table (present only in the PDF's worked example, but the field-definition table is the agreed source of truth per team decision).
- **BREAKING**: Remove `offenceDateCode` from `offence-details.json` — not present in the PDF's Offence Related Fields table as its own field (only referenced in another field's business-rule text).
- **BREAKING**: Remove `occupationCode` from `individual.json` — the PDF's Defendant Related Fields table lists this field as `defendantOccupationCode` (Format `N5`, Description "An occupation code") with the entire row struck through, indicating it is deprecated. The doc's field-name column differs from the actual wire field (`occupationCode`, as shown in the PDF's own example payload and in the schema), but it is the same field. Confirmed with requester.
- Update all fixtures/payloads that currently populate the removed fields so existing test suites keep passing: the command-api unit test fixture and the six integration-test payload files under `stagingprosecutorscivil-integration-test`.

No fields are being added or renamed in this change (e.g. the PDF's "New attribute" fields — `hearingType`, `startDateRangeOfHearing`, `endDateRangeOfHearing`, `relatedReferenceNumber` — are out of scope; `relatedReferenceNumber` already exists in the schema). Four schema files (`self-defined-information.json`, `prosecution-submission-details.json`, `summons-code.json`, `initiation-code.json`) were found to be dead code, unreferenced by the charge/summons-prosecution `$ref` graph — they are explicitly left untouched by this change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `civil-prosecution-request-schema`: The request schema for charge-prosecution and summons-prosecution (and their shared internal referral schemas: `individual.json`, `defendant-details.json`, `organisation.json`, `offence-details.json`) no longer accepts the fields listed above; any request containing them will be rejected by the existing `additionalProperties: false` constraint.

## Impact

- **Affected schemas** (`stagingprosecutorscivil-domain/stagingprosecutorscivil-domain-message/src/raml/json/schema/`): `individual.json`, `defendant-details.json`, `organisation.json`, `offence-details.json`.
- **Affected consumers** (all `$ref` the schemas above, no direct edits needed but must be re-verified): `stagingprosecutorscivil-command-api` (charge-prosecution.json, summons-prosecution.json), `stagingprosecutorscivil-command-handler` (command.charge-prosecution.json, command.summons-prosecution.json), event schemas in `stagingprosecutorscivil-event-listener` / `stagingprosecutorscivil-event-processor`.
- **Test fixtures requiring updates**:
  - `stagingprosecutorscivil-command-api/src/test/resources/valid-charge-prosecution-request.json` (uses `bailConditions`)
  - `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-with-related-reference.json`
  - `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json` (also uses `occupationCode`)
  - `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json` (also uses `occupationCode`)
  - `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-mandatory-fields-only.json` (also uses `occupationCode`)
  - `stagingprosecutorscivil-integration-test/src/test/resources/payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json` (also uses `occupationCode`)
  - `stagingprosecutorscivil-integration-test/src/test/resources/payload/summons/stagingprosecutors.submit-summons-prosecution-with-related-reference.json`
- **Additional production-code impact for `occupationCode`** (mirrors the pattern found for the other seven fields during verification): `IndividualToProsecutionCaseFileIndividualConverter` (`stagingprosecutorscivil-event-processor`) forwards `occupationCode` to the outbound Prosecution Case File integration via `.withOccupationCode(...)`; this mapping call, its unit test assertion in `IndividualToProsecutionCaseFileIndividualConverterTest`, and the `Prosecutors` test-data builder's `.withOccupationCode(1)` call all need updating. `AbstractProsecutionSchemaValidationTest` also has a negative-scenario assertion (line ~421) asserting on `occupationCode`'s max-value validation message, which becomes stale once the field is removed.
- **Considered and ruled out**: the PDF's Defendant Related Fields table also strikes through `organisationTelephoneNumber`, but this field does not exist anywhere in the current schemas (only the distinct, non-struck-through `companyTelephoneNumber` exists) — no action needed.
- **External API consumers**: civil authority prosecutor systems currently sending any of the removed fields will start receiving 4xx rejection — this is a breaking API contract change and should be communicated ahead of release.
