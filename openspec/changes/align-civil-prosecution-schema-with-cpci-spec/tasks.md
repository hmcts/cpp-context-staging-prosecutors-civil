## 1. Schema edits

- [x] 1.1 Remove `aliases` property (and its `minItems`/`items` block) from `stagingprosecutorscivil-domain/stagingprosecutorscivil-domain-message/src/raml/json/schema/individual.json`
- [x] 1.2 Remove `bailConditions` property from `individual.json`
- [x] 1.3 Remove `numPreviousConvictions` property from `stagingprosecutorscivil-domain/stagingprosecutorscivil-domain-message/src/raml/json/schema/defendant-details.json`
- [x] 1.4 Remove `aliasOrganisationNames` property from `stagingprosecutorscivil-domain/stagingprosecutorscivil-domain-message/src/raml/json/schema/organisation.json`
- [x] 1.5 Remove `backDuty`, `backDutyDateFrom`, `backDutyDateTo` properties from `stagingprosecutorscivil-domain/stagingprosecutorscivil-domain-message/src/raml/json/schema/offence-details.json`
- [x] 1.6 Remove `vehicleMake`, `vehicleRegistrationMark` properties from `offence-details.json`
- [x] 1.7 Remove `offenceDateCode` property from `offence-details.json`
- [x] 1.8 Re-check each edited file's `required` array — confirm none of the removed fields were listed there (none currently are, per the design analysis) and remove any that are
- [x] 1.9 (Added mid-implementation, confirmed with requester) Remove `occupationCode` property from `individual.json` — the PDF's `defendantOccupationCode` row in the Defendant Related Fields table is struck through in its entirety; it is the same field as the schema's `occupationCode` (PDF's field-name column just uses a different label than the wire field)

## 2. Fixture updates

- [x] 2.1 Remove `bailConditions` line from `stagingprosecutorscivil-command-api/src/test/resources/valid-charge-prosecution-request.json`
- [x] 2.2 Remove `numPreviousConvictions` and `offenceDateCode` from `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-with-related-reference.json`
- [x] 2.3 Remove `numPreviousConvictions`, `aliases`, `offenceDateCode`, `backDuty`, `backDutyDateFrom`, `backDutyDateTo`, `vehicleMake`, `vehicleRegistrationMark` from `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json`
- [x] 2.4 Remove the same field set (both defendant blocks) from `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json`
- [x] 2.5 Remove the same field set from `stagingprosecutorscivil-integration-test/src/test/resources/payload/charge/stagingprosecutors.submit-charge-prosecution-mandatory-fields-only.json`
- [x] 2.6 Remove the same field set from `stagingprosecutorscivil-integration-test/src/test/resources/payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json`
- [x] 2.7 Remove `numPreviousConvictions` and `offenceDateCode` from `stagingprosecutorscivil-integration-test/src/test/resources/payload/summons/stagingprosecutors.submit-summons-prosecution-with-related-reference.json`
- [x] 2.8 Re-run the repo-wide grep for the seven removed field names across all non-`target` JSON files to confirm no fixture was missed
- [x] 2.9 (Added mid-implementation, confirmed with requester) Remove `numPreviousConvictions`, `offenceDateCode`, `backDuty`, `backDutyDateFrom`, `backDutyDateTo`, `vehicleMake`, `vehicleRegistrationMark` from the 8 RAML documentation example bodies discovered by the 2.8 re-grep (`stagingprosecutorscivil-command-api/src/raml/json/*.json` x4, `stagingprosecutorscivil-command-handler/src/raml/json/*.json` x2, `stagingprosecutorscivil-event-processor` and `stagingprosecutorscivil-event-listener` `src/yaml/json/*.json` x2 each)
- [x] 2.10 (Added mid-implementation, confirmed with requester) Remove `occupationCode: 666` from the 4 integration-test payloads that carry it: `stagingprosecutors.submit-charge-prosecution-single-case.json`, `stagingprosecutors.submit-charge-prosecution-all-fields.json`, `stagingprosecutors.submit-charge-prosecution-mandatory-fields-only.json`, `stagingprosecutors.submit-summons-prosecution-all-fields.json`
- [x] 2.11 (Added mid-implementation, confirmed with requester) Remove `occupationCode: 666` from the same 10 RAML/event documentation example bodies touched in 2.9 (`stagingprosecutorscivil-command-api/src/raml/json/*.json` x4, `stagingprosecutorscivil-command-handler/src/raml/json/*.json` x2, `stagingprosecutorscivil-event-processor` and `stagingprosecutorscivil-event-listener` `src/yaml/json/*.json` x2 each)

## 3. Verification

- [x] 3.1 Run `mvn test -pl stagingprosecutorscivil-command/stagingprosecutorscivil-command-api` and confirm the updated fixtures still validate successfully against the trimmed schemas — found and fixed 6 stale negative-scenario assertions in `AbstractProsecutionSchemaValidationTest.java` that asserted on the removed fields' validation messages (bailConditions, aliases, offenceDateCode, backDuty, backDutyDateFrom, backDutyDateTo). All 221 tests pass.
- [x] 3.2 Run `mvn clean install -DskipITs` for the full reactor build to confirm no other module's tests reference the removed fields — surfaced a real production-code impact: `stagingprosecutorscivil-event-processor`'s `DefendantToProsecutionCaseFileDefendantConverter`, `OffenceToProsecutionCaseFileOffenceConverter`, and `IndividualToProsecutionCaseFileIndividualConverter` forwarded these 7 fields to the separate outbound Prosecution Case File integration. Confirmed with requester and removed the corresponding `.with*(...)` mapping calls (plus matching updates to the `Prosecutors` test-data builder and 3 converter unit tests) — those fields now go through as null/absent to Prosecution Case File. Full reactor build (`stagingprosecutorscivil-service` down to leaf modules) is green, 0 failures across all 22 modules.
- [ ] 3.3 Run `./runIntegrationTests.sh` (requires Docker + `CPP_DOCKER_DIR`) to confirm the updated integration-test payloads still submit successfully end-to-end — requester already ran this once against the original 7-field removal, but it now needs re-running after 1.9/2.10/2.11 (`occupationCode` removal) land, since those payload files are touched again
- [x] 3.4 Manually diff each edited schema file against the corresponding PDF field-definition table one more time to confirm every remaining property has a matching table row — confirmed clean for `individual.json`, `defendant-details.json`, `organisation.json`, `offence-details.json`
- [x] 3.5 (Added mid-implementation, confirmed with requester) Remove the `occupationCode` max-value negative-scenario assertion (~line 421) from `AbstractProsecutionSchemaValidationTest.java`, mirroring the 6 stale assertions fixed in 3.1
- [x] 3.6 (Added mid-implementation, confirmed with requester) Remove the `occupationCode` mapping from `IndividualToProsecutionCaseFileIndividualConverter` (the individual-level `.withOccupationCode(...)` call — the pre-existing, unrelated `parentGuardian`-level `.withOccupationCode(null)` hardcoded default was left untouched since it was never wired to our schema), and update `IndividualToProsecutionCaseFileIndividualConverterTest`'s assertion and the `Prosecutors` test-data builder's `.withOccupationCode(1)` call, mirroring the 3.2 production-code fix for the other 7 fields. Full reactor build (`mvn clean install -DskipITs`) green, 0 failures.

## 4. Follow-up (not part of this change)

- [ ] 4.1 Raise with the CPCI spec authors whether `vehicleMake`/`vehicleRegistrationMark` should be added back to the Offence Related Fields table, given they appear in the spec's own worked example
- [ ] 4.2 Track the CPCI "New attribute" fields (`hearingType`, `startDateRangeOfHearing`, `endDateRangeOfHearing`) as a separate additive change
