# Schema Mismatch Report — CPCI v1.1 Spec vs `cpp-context-staging-prosecutors-civil`

> Spec source: Common Platform Civil Interface External API Specification v1.1 (Draft, 22/04/2026).
> Repo state: branch `main` at `d0ea6cf` (artifact version `17.103.10-SNAPSHOT`).
> Schemas inspected:
> - `stagingprosecutorscivil-command/stagingprosecutorscivil-command-api/src/raml/staging_prosecutors_civil_command_api.raml`
> - `stagingprosecutorscivil-command/stagingprosecutorscivil-command-api/src/raml/json/schema/{charge,summons}-prosecution{,-with-submission-id}.json`
> - `stagingprosecutorscivil-domain/stagingprosecutorscivil-domain-message/src/raml/json/schema/*.json` (referenced via `$ref`)

Severity legend: 🔴 blocks v1.1 / breaks contract · 🟠 spec divergence needing decision · 🟡 minor / cosmetic / extras

---

## A. v1.1 ticket-driven gaps (CCT-1222 + CAD-1127)

| # | Spec element | Repo state | Severity | Notes |
|---|---|---|---|---|
| A1 | Path `POST /cases` | RAML defines `POST /prosecutions` | 🔴 | Spec internal URI section explicitly lists `…/stagingprosecutors-civil/cases`. CCT-1222. |
| A2 | External CT `application/vnd.hmcts.cjs.cpci-summons.v1+json` | RAML body uses `application/vnd.stagingprosecutorscivil.summons-prosecution+json` | 🔴 | Internal media type still under the legacy `summons-prosecution` name. Renaming is part of CCT-1222. |
| A3 | External CT `application/vnd.hmcts.cjs.cpci-other-cases.v1+json` (replaces `cpci-charge` + `cpci-enforcement`) | RAML body uses `application/vnd.stagingprosecutorscivil.charge-prosecution+json` only | 🔴 | The "charge" → "other-cases" rename has not happened. |
| A4 | Enforcement content-type removed; merged into `other-cases` | Repo has only `charge-prosecution.json` and `summons-prosecution.json` schemas — no enforcement schema is present | 🔴 / Open Q | Enforcement is *already* missing as a separate schema. Either (a) it was never split out (and the spec's "merge" is a no-op for this repo) or (b) it lives elsewhere. Confirm via Open Q3 in `requirements.md`. |
| A5 | Internal command names renamed (`stagingprosecutorscivil.charge-prosecution` → `other-cases`) | RAML `(mapping).name` still `stagingprosecutorscivil.charge-prosecution` | 🔴 | Will cascade through command handler beans, action mapper, etc. |
| A6 | New attribute `relatedReferenceNumber` (O, A36, alphanumeric + hyphens) | **Not present in any schema** (no `relatedURN` either) | 🔴 | FR-004. Add to `prosecution-submission-details.json` (or wherever the case-level fields live in the merged `other-cases` schema). |
| A7 | New error code `OFFENCE_CODE_NOT_SUPPORTED` returned by GetSubmissionStatus | grep across `*.java/raml/json/yaml` returns no matches | 🔴 | FR-005. Needs error enum + producer logic. |

---

## B. Top-level / structural divergence between spec example and repo

The v1.1 *Example Request Body* (page 16) shows a very different shape from what the repo schemas accept. This may be the deepest issue and likely needs spec-author confirmation before any field-level work.

| # | Spec example shape | Repo schema shape | Severity |
|---|---|---|---|
| B1 | Top-level: `{ "prosecutionSubmissionDetails": {…}, "defendant": {…} }` — single `defendant` | `{ "prosecutionCases": [ {…} ], "prosecutingAuthority": "…", "hearingDetails": {…} }` — array of cases, each with array of defendants | 🟠 Major |
| B2 | Defendant: `defendant.defendantPerson.{title, forename, surname, contactDetails, dateOfBirth, selfDefinedInformation, occupation, occupationCode, driverNumber, address, nationalInsuranceNumber}` | `defendant.{defendantDetails, individual\|organisation (oneOf), offences}` — name fields nested under `individual.nameDetails`, contact under `individual.contactDetails`, etc. | 🟠 Major |
| B3 | `prosecutingAuthority`, `informant`, `urn` live under `prosecutionSubmissionDetails` | Repo: `prosecutingAuthority` at root; `informant`, `urn` inside each `prosecutionCases[]` element | 🟠 |
| B4 | `prosecutorCosts` lives directly on `defendant` | Repo: under `defendant.defendantDetails.prosecutorCosts` | 🟡 |
| B5 | `selfDefinedInformation` block holds `gender`, `ethnicity`, `nationality`, `additionalNationality` | Repo: `gender`/`nationality`/`additionalNationality` exist on **both** `individual.json` *and* `self-defined-information.json` (duplicated); `ethnicity` only in both. The active path the validator follows isn't obvious. | 🟠 |

> **Recommendation:** Confirm with the spec author whether the example on p.16 is normative for v1.1 or merely illustrative. If normative, FR-006 expands significantly — most of this repo's command-api shape changes.

---

## C. Field-level mismatches (assuming current repo shape stays)

### C1. Case / hearing fields

| Spec field | Spec rule | Repo location | Mismatch | Sev |
|---|---|---|---|---|
| `urn` | A36, pattern `[A-Za-z0-9-]+` | `prosecution-case.json`: `maxLength: 36`, **no pattern** | Pattern not enforced | 🟠 |
| `paymentReference` | A37 | `prosecution-case.json`: `maxLength: 36` | Off-by-one (37 → 36) | 🟡 |
| `summonsCode` | S1, enum `A` / `E` / `M` / `W` | `summons-code.json`: `string`, `maxLength: 1`, **no enum** | Values not enumerated | 🟠 |
| `hearingType` | A10, O (added v1.0) | **Missing** from `hearing-details.json` and elsewhere | Never added | 🔴 |
| `startDateRangeOfHearing` | D10, M for enforcement (added v1.0) | **Missing** | Never added | 🔴 |
| `endDateRangeOfHearing` | D10, M for enforcement (added v1.0) | **Missing** | Never added | 🔴 |
| `dateOfHearing` | M for summons/charge, NA for enforcement | `hearing-details.json`: required for *all* variants | Required-flag too strict for enforcement path | 🟠 |
| `timeOfHearing` | T8, **O** | `hearing-details.json`: `required: [..., "timeOfHearing"]` | Required-flag mismatch (spec says optional) | 🟠 |
| `courthearinglocation` | M, A7 | `hearing-details.json`: `courtHearingLocation`, M, A7 | Casing differs from spec (`courthearinglocation` vs `courtHearingLocation`) — likely spec typo, but worth a confirm | 🟡 |

### C2. Defendant fields

| Spec field | Spec rule | Repo location | Mismatch | Sev |
|---|---|---|---|---|
| `prosecutorDefendantId` | M, A36, default `"1"`, alphanum + hyphens | `defendant-details.json`: M, `maxLength: 36`, `pattern: "^[a-zA-Z0-9-]*$"` (note `*`, not `+`, allows empty string) | Pattern allows empty; no default | 🟠 |
| `custodyStatus` | A1, enum `B` / `U` / `C` / `R` | `custody-status.json`: `string`, `maxLength: 1`, no enum | Values not enumerated | 🟠 |
| `gender` | N1, enum 0/1/2/9, CM | Defined in `gender.json` (enum 0,1,2,9 ✓), but **referenced from both** `individual.json` (required) **and** `self-defined-information.json` (required, but as plain integer with no enum) | Duplicate definition; `self-defined-information.json` lacks the enum constraint | 🟠 |
| `nationality` / `additionalNationality` | A3, ISO 3166-1 alpha-3 | Duplicated in `individual.json` and `self-defined-information.json` | Same field defined twice (and one site lacks the strict pattern) | 🟠 |
| `nationalInsuranceNumber` | A9 | `individual.json`: `pattern: "^(?!BG)…[A-D ]$"` (CGS DSC 5.15) | Repo is *stricter* than spec — fine, just note | 🟡 |
| `driverNumber` | A16, "Not currently supported in API v2.0" | Present in `individual.json`, `maxLength: 16` | Spec marks deprecated; still defined. Out of scope for v1.1? | 🟡 |
| `languageRequirement` | A150, "Not currently supported in API v2.0" | Present in `individual.json`, `maxLength: 150` | Same as above | 🟡 |
| `specificRequirements` | A150, "Not currently supported in API v2.0" | Present in `individual.json`, `maxLength: 150` | Same as above | 🟡 |
| `Alias Array` (entire section) | Whole block struck-through in spec | `individual.json` still defines `aliases: array<name-details>`; `organisation.json` has `aliasOrganisationNames` | Possibly to remove in v1.1 — confirm with spec author | 🟠 |
| `~~organisationTelephoneNumber~~` (defendant) | Struck-through (deprecated → replaced by `companyTelephoneNumber`) | Already absent — only `companyTelephoneNumber` present in `organisation.json` | ✅ already aligned | — |
| `~~defendantOccupationCode~~` | Struck-through | Repo has `occupationCode` (in `individual.json`) — *different name* (no `defendant` prefix) | Naming differs; if removal is required, applies to `occupationCode` | 🟡 |
| `bailConditions` | Not in spec | Present in `individual.json` (`maxLength: 2500`) | Repo extra | 🟡 |
| `numPreviousConvictions` | Not in spec | Present in `defendant-details.json` (`integer`) | Repo extra | 🟡 |

### C3. Parent guardian fields

| Spec field | Spec rule | Repo location | Mismatch | Sev |
|---|---|---|---|---|
| Parent Guardian title | Free text (Mr/Mrs/Ms/Dr/Rev/Sir/Lady/Lord/Dame all valid per CJS guidance) | `parent-guardian-name-details.json`: `pattern: "^(Mr\|Mrs\|Ms\|Miss)$"` | Pattern rejects Dr, Rev, Sir, Lady, Lord, Dame, etc. — also accepts `Miss` which spec doesn't mention | 🔴 / 🟠 (regression vs CJS) |
| Parent Guardian forename | Mandatory (CM, alphanum no double spaces) | `parent-guardian-name-details.json`: `required: ["surname"]` only — forename **not required** | Spec says forename CM, repo only requires surname | 🟠 |
| Parent Guardian address | Lines 1-5 + postcode, all listed in spec | `parent-guardian.json` references whole `address.json` (`address1`-`address5`, `postcode`) | ✅ structurally covered | — |
| Parent Guardian observedEthnicity | Present in spec | Present in `parent-guardian-individual.json` as `number` with `minLength`/`maxLength` (meaningless on number) | Schema-keyword misuse | 🟡 |

### C4. Offence fields

| Spec field | Spec rule | Repo location | Mismatch | Sev |
|---|---|---|---|---|
| `cjsOffenceCode` | M, A8 | `offence-details.json`: M, `maxLength: 8` | ✅ | — |
| `offenceSequenceNo` | M, N3 (≤999) | `offence-details.json`: `integer`, `minimum: 1`, `maxLength: 3` | `maxLength` is meaningless on integers in JSON Schema — should be `maximum: 999` | 🟡 (schema bug) |
| `offenceCommittedDate` | O, D10 | Present, ✓ | ✅ | — |
| `offenceCommittedEndDate` | CM (mandatory if Offence Date Code = 4) | Present, no conditional-required wiring | Conditional rule not encoded | 🟠 |
| `offenceLocation` | CM (mandatory if offence wording includes a location element) | Present, plain optional | Conditional rule not encoded | 🟠 |
| `offenceWording` | M, A2500 | M, `maxLength: 2500`, ✓ | ✅ | — |
| `offenceWordingWelsh` | O, A2500 | Present, ✓ | ✅ | — |
| `statementOfFacts` / `statementOfFactsWelsh` | O, A4000 | Present in `offence.json` (not `offence-details.json`); ✓ on lengths | Defined at wrong nesting level (cosmetic) | 🟡 |
| `prosecutorCompensation` | O, money | Present, ✓ | ✅ | — |
| `laidDate` | M, D10 | M, ✓ | ✅ | — |
| `arrestDate` | O, D10 | Present in `offence.json` (sibling of offence-details, not within) | Position differs from spec table grouping | 🟡 |
| `backDuty` / `backDutyDateFrom` / `backDutyDateTo` | Not in spec field table | Present in `offence-details.json` | Repo extras | 🟡 |
| `vehicleMake` / `vehicleRegistrationMark` | Appear in spec **example** only (not in field tables) | Present in `offence-details.json` | Tolerated by example, undocumented in tables | 🟡 |

### C5. Prosecution-submission-details extras

| Repo field | In spec? | Notes |
|---|---|---|
| `initiationCode` | Implied only (mentioned as internal in `summonsCode` rule) | Internal/legacy — likely fine |
| `writtenChargePostingDate` | Not in spec | Repo extra |

---

## D. Things in spec NOT in any schema (additional to A6/A7)

| Spec field | Where it should land |
|---|---|
| `relatedReferenceNumber` | `prosecution-submission-details.json` (A6 above) |
| `hearingType` | `hearing-details.json` |
| `startDateRangeOfHearing`, `endDateRangeOfHearing` | `hearing-details.json` |
| `OFFENCE_CODE_NOT_SUPPORTED` error | submission-status error enum (Java side, not RAML) |
| Conditional-mandatory rules (`offenceCommittedEndDate` if dateCode=4, `offenceLocation` if wording contains location) | Either JSON Schema `if/then` (draft-7+, but repo uses draft-04) or business-validator code |
| `summonsCode` enum (A/E/M/W), `custodyStatus` enum (B/U/C/R) | `summons-code.json`, `custody-status.json` |

---

## E. Caveats / things I did **not** verify

- I didn't open the resolved `$ref`s into `http://justice.gov.uk/domain/core/common/definitions.json` (`pncId`, `croNumber`, `email`, `ukGovPostCode`, `caseURN`, `vehicleMake`, etc.). Those live in a `coredomain` JAR dependency. The patterns/lengths there could either over- or under-constrain vs the spec.
- I didn't read the query-side RAML or the materials (multipart) endpoint — those are out of scope for v1.1 per `requirements.md`.
- I didn't verify the submission-status response schema (FR-005's error array shape: `code`, `values[]{key,value}`).
- The path mismatch (`/prosecutions` vs `/cases`) might be intentional if APIM rewrites the path before forwarding internally — confirm with the gateway/APIM config in `stagingprosecutorscivil-apim-policy/`.

---

## Summary

**Must-fix for v1.1 release:** A1–A7, C1 missing fields (`hearingType`, `startDateRangeOfHearing`, `endDateRangeOfHearing`), C3 parent-guardian title pattern.

**Decision needed (block on Open Qs):** B1–B3 structural shape, A4 enforcement-merge interpretation, and how aggressively to deprecate the struck-through fields (driverNumber, languageRequirement, specificRequirements, alias arrays).

**Tightening recommended (not blocking):** all 🟠 enum / pattern / required-flag gaps in C1–C4 — these are real spec divergences and any could cause silent acceptance of invalid client data.
