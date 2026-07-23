## Context

`charge-prosecution.json` and `summons-prosecution.json` (in `stagingprosecutorscivil-command-api`) both `$ref` a shared graph of internal schemas in `stagingprosecutorscivil-domain-message`:

```
charge-prosecution.json / summons-prosecution.json
 └─ prosecutionCases[] → prosecution-case.json
     └─ defendants[] → defendant.json
         ├─ defendantDetails → defendant-details.json
         ├─ individual → individual.json
         │    ├─ nameDetails / aliases → name-details.json
         │    ├─ contactDetails → contact-details.json
         │    ├─ parentGuardian → parent-guardian.json (+ nested schemas)
         │    └─ custodyStatus → custody-status.json
         ├─ organisation → organisation.json
         └─ offences[] → offence.json
             └─ offenceDetails → offence-details.json
 └─ hearingDetails → hearing-details.json
```

Every object schema in this graph sets `additionalProperties: false`, so removing a property from the schema is sufficient on its own to make the API reject that field — no separate validation code needs to change.

This same shared graph is also `$ref`'d by the command-handler's internal command schemas and by the event-listener/event-processor's event schemas, so a field removed here is removed everywhere in the pipeline simultaneously; there is exactly one place to edit per field.

Four schema files were confirmed (via a repo-wide grep for `$ref` usages) to be unreferenced by this graph or by anything else: `self-defined-information.json`, `prosecution-submission-details.json`, `summons-code.json`, `initiation-code.json`. These are dead code and are explicitly out of scope for this change.

## Goals / Non-Goals

**Goals:**
- Remove the seven fields identified in the proposal from the four schema files that declare them, so the schemas exactly match the CPCI spec's field-definition tables.
- Keep every existing fixture/payload that currently exercises those fields passing, by removing the field usages from the fixtures rather than leaving them to fail validation.

**Non-Goals:**
- Adding the CPCI spec's "New attribute" fields (`hearingType`, `startDateRangeOfHearing`, `endDateRangeOfHearing`) — a separate, additive change.
- Renaming fields or restructuring the JSON shape to match the PDF's example payload nesting (e.g. `defendantPerson`/`selfDefinedInformation`) — the PDF's worked example uses a different shape than the actual schema/API today, and reconciling that is out of scope here.
- Deleting the four dead/unreferenced schema files.
- Changing `required` semantics for fields that remain (e.g. no mandatory/optional flips).

## Decisions

- **Edit the shared domain-message schema files directly, not the command-api wrapper schemas.** The eight fields all live in `individual.json`, `defendant-details.json`, `organisation.json`, and `offence-details.json`. Editing there automatically propagates to both prosecution types and to the command-handler/event schemas, avoiding duplicated edits and drift between charge and summons.
- **Treat the PDF's field-definition tables as the sole source of truth, not the worked JSON example.** This was confirmed with the requester: `vehicleMake`/`vehicleRegistrationMark` appear only in the example, not the table, and are being removed on that basis. Applied the same rule consistently to `offenceDateCode` (referenced only in another field's business-rule prose, not given its own table row).
- **Strikethrough in a field-definition table is a removal signal, not just missing-from-table.** `aliases` was already identified this way (the whole Alias Array table is struck through). A second pass over the PDF found the `defendantOccupationCode` row in the Defendant Related Fields table is also struck through in its entirety — same signal, same treatment. Confirmed with requester that the PDF's field-name column (`defendantOccupationCode`) and the actual wire field (`occupationCode`, per the PDF's own example payload and the schema) are the same field.
- **`organisationTelephoneNumber` (also struck through) is out of scope — not because it's a lower priority, but because there's nothing to remove.** A repo-wide grep found no schema anywhere declares this field; only the distinct, non-struck-through `companyTelephoneNumber` exists. Recorded here so a future pass over the PDF doesn't re-flag it.
- **Update fixtures in the same change rather than as follow-up.** `additionalProperties: false` means any fixture still sending a removed field will fail schema validation the moment the schema changes; leaving that for later would leave the build red between commits.
- **Leave the four dead schema files alone.** They are unreferenced by the charge/summons `$ref` graph, so deleting them doesn't affect this schema's contract and carries its own independent risk (something else — a test, a doc generator — might still reference them by path even though nothing `$ref`s them at the JSON-schema level).

## Risks / Trade-offs

- **[Risk] Breaking change for live callers.** Any civil authority prosecutor system currently sending `aliases`, `bailConditions`, `numPreviousConvictions`, `aliasOrganisationNames`, `backDuty*`, `vehicleMake`, `vehicleRegistrationMark`, `offenceDateCode`, or `occupationCode` will start receiving 4xx rejections. → Mitigation: this is an intentional, spec-driven contract correction; coordinate release communication with prosecuting authorities ahead of deployment, per standard breaking-change practice for this API.
- **[Risk] `occupationCode` is forwarded downstream, unlike a purely request-schema field.** `IndividualToProsecutionCaseFileIndividualConverter` maps it onto the outbound Prosecution Case File integration today, the same pattern task 3.2's verification pass found for the other seven fields. → Mitigation: remove the `.withOccupationCode(...)` mapping call and its test coverage in the same change, exactly as was done for the other fields; the field goes through as null/absent to Prosecution Case File going forward.
- **[Risk] vehicleMake/vehicleRegistrationMark removal contradicts the PDF's own worked example.** The example payload in the spec document still shows these fields being sent. → Mitigation: explicitly confirmed with the requester to follow the table over the example; flagged in the proposal so the discrepancy is visible to reviewers and can be raised back with the spec's authors if it turns out to be a documentation gap rather than an intentional removal.
- **[Risk] Missed fixture/test file.** A repo-wide grep was used to find all JSON files referencing the seven field names, but a future JSON payload (e.g. added after this change was scoped) could still use one. → Mitigation: the schema's `additionalProperties: false` will surface this immediately as a test failure, not a silent data-loss bug.

## Migration Plan

1. Remove the seven fields from `individual.json`, `defendant-details.json`, `organisation.json`, `offence-details.json`.
2. Update the one command-api unit test fixture and six integration-test payload fixtures that reference the removed fields.
3. Run `mvn clean install` (unit tests) and `./runIntegrationTests.sh` (integration tests) to confirm nothing else regresses.
4. No database/viewstore migration is required — this is a request-schema-only change.
5. Rollback is a straight revert of the schema/fixture edits; no data migration to reverse.

## Open Questions

None outstanding — all scope decisions were confirmed with the requester before writing this design.
