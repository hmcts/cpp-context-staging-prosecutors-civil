# Requirements: CPCI API v1.1 — Civil Interface External API

> Source: Common Platform Civil Interface External API Specification, v1.1 (Draft, 22/04/2026, Matthew Rich).
> Implementing service: `cpp-context-staging-prosecutors-civil` (legacy WildFly/Maven CQRS context service).
> Related tickets: **CAD-1127**, **CCT-1222**.

## Context
The Common Platform Civil Interface (CPCI) External API enables civil authorities (e.g. TfL) to submit prosecution cases, attach materials, check submission status, and retrieve shared results from the HMCTS Common Platform. The current service implements v1.0. **This requirement covers the v1.1 delta only** — driven by two tickets recorded in the spec's document history.

This is a legacy WildFly/Maven CQRS context service. Apply patterns from `skills/context-service-guide`; **do not** apply Spring Boot/HMCTS-template patterns.

## Actors
| Actor | Description |
|-------|-------------|
| Civil Authority API client | External system (e.g. TfL) submitting cases/materials and polling submission status. Uses mTLS + shared-secret auth. |
| HMCTS Common Platform (CPCI) | This service — receives, validates, persists and exposes results. |
| API Gateway (APIM) | Fronts the external endpoint, handles auth/routing before forwarding internally. |

## Functional requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-001 | Replace external content-type `application/vnd.hmcts.cjs.cpci-charge.v1+json` with `application/vnd.hmcts.cjs.cpci-other-cases.v1+json` for the SubmitCase POST `/cases` endpoint. (CCT-1222) | Must |
| FR-002 | Remove the standalone enforcement content-type — enforcement is now handled under the merged `cpci-other-cases` schema. (CCT-1222) | Must |
| FR-003 | Merge the previous `charge-prosecution` and `enforcement` JSON schemas into a single `other-cases` schema. (CCT-1222) | Must |
| FR-004 | Rename request attribute `relatedURN` → `relatedReferenceNumber` (Civil Authority reference for a linked case/account, optional, A36). (CCT-1222) | Must |
| FR-005 | Add new submission-status error code `OFFENCE_CODE_NOT_SUPPORTED` — returned by the GetSubmissionStatus API when the offence code is not supported by the civil API (i.e. is a Criminal CJS code or blacklisted civils code). (CAD-1127) | Must |
| FR-006 | Update the command-api RAML, JSON schemas, internal command names/handlers, and any client SDK/RAML-jar artefacts to reflect the renamed content-types and merged schema. | Must |
| FR-007 | Maintain backward acceptance of v1.0 content-types and `relatedURN` only if the protocol agreement requires a deprecation window — otherwise hard-cut at release boundary. | Should (TBD — see Open Q1) |

## Non-functional requirements
| ID | Category | Requirement | Threshold |
|----|----------|-------------|-----------|
| NFR-001 | Compatibility | Existing v1.0 clients must have a documented migration path before this version is released externally. | Migration note + protocol amendment |
| NFR-002 | Security | All external traffic continues to use mTLS + shared-secret auth (per Code of Connection). | No change to existing controls |
| NFR-003 | Observability | New `OFFENCE_CODE_NOT_SUPPORTED` error must be emitted with sufficient context (offenceCode, urn) for triage. | Logged + returned in submission-status `errors[]` |
| NFR-004 | Performance | API response SLAs unchanged — 202 on accept, 200 on status query; rate-limiting (429) preserved. | Per existing SLA |
| NFR-005 | Data | Field length/format constraints from the spec must be enforced (e.g. `relatedReferenceNumber` = A36, alphanumeric + hyphens). | Per Data Content table |

## Acceptance criteria

### FR-001 — Rename content-type to `cpci-other-cases.v1+json`
- AC-001: Given a client posts to `/v1/cases` with `Content-Type: application/vnd.hmcts.cjs.cpci-other-cases.v1+json` and a valid body conforming to the merged schema, when the gateway routes the request, then the service responds **202 Accepted** with a `submissionId`.
- AC-002: Given a client posts with the old `cpci-charge.v1+json` content-type, when v1.1 is live (and the deprecation window has expired per Open Q1), then the service responds **415 Unsupported Media Type**.

### FR-002 / FR-003 — Merge enforcement into `other-cases`
- AC-003: The merged schema accepts both former charge-style payloads and former enforcement-style payloads (including `startDateRangeOfHearing`/`endDateRangeOfHearing` for enforcement auto-listing).
- AC-004: A previously-valid v1.0 enforcement payload (re-cast under the new content-type) round-trips through the API and produces an equivalent prosecution aggregate to v1.0.

### FR-004 — Rename `relatedURN` → `relatedReferenceNumber`
- AC-005: Given a request body containing `"relatedReferenceNumber": "<value>"`, when accepted, then the value is persisted on the prosecution aggregate and surfaced under the new name in any downstream views/events.
- AC-006: The JSON schema and RAML reject requests using the legacy `relatedURN` key (after deprecation cutover).

### FR-005 — `OFFENCE_CODE_NOT_SUPPORTED` error
- AC-007: Given a SubmitCase request with an `cjsOffenceCode` that exists in the CJS catalogue but is classified as Criminal (not Civil) or appears on the civils blacklist, when the submission is processed, then GetSubmissionStatus returns `status: REJECTED` with `errors[].code == "OFFENCE_CODE_NOT_SUPPORTED"` and `values[].key == "cjsOffenceCode"` carrying the rejected code.
- AC-008: `OFFENCE_CODE_NOT_SUPPORTED` is returned **distinctly** from `OFFENCE_CODE_IS_INVALID` (which fires only when the code is not a valid CJS code at all).

### FR-006 — Artefact propagation
- AC-009: `staging_prosecutors_civil_command_api.raml` and JSON schemas under `stagingprosecutorscivil-command-api/src/raml/json/{,schema/}` are updated; `mvn clean install` succeeds and the published `raml-jar` reflects the new names.
- AC-010: Integration tests under `stagingprosecutorscivil-integration-test` exercising both content-types pass via `runIntegrationTests.sh`.

## Constraints
- **Protocol Agreement / Code of Connection / SLA** ([3][4][5] in the spec) gate any external-facing change. Changes to the Interface Specification are only valid if they remain consistent with these protocols — confirm with HMCTS↔civil-authority protocol owners before publishing v1.1.
- **CJS Data Standards Catalogue** [2] is the authoritative source for offence codes, OU codes, ethnicity codes, etc. — do not redefine.
- **Out of scope per the spec itself**: Non-Police Prosecutor API (covered by CPPI) and Police Prosecutor API (covered by SPI/CJSE).
- **Legacy WildFly architecture** — must not introduce Spring Boot patterns. Use the existing CQRS module layout.

## Out of scope
- Any change to authentication (mTLS / shared secret model unchanged).
- Changes to the ResultsAPI shape (no v1.1 changes documented).
- Changes to SubmitDocument materials handling (no v1.1 changes documented).
- New material types beyond those listed.
- Net-new functional behaviour beyond CAD-1127 and CCT-1222.

## Open questions
1. **Deprecation window for v1.0 content-types and `relatedURN`** — Owner: Matthew Rich (spec author) / Protocol owner — Due: TBD. Hard-cut at v1.1 release, or accept both for a transition period? The spec doesn't state this; FR-007 hinges on the answer.
2. **Source of truth for the "blacklisted civils code" list** powering `OFFENCE_CODE_NOT_SUPPORTED` (CAD-1127) — is this a config table in this service, a reference-data lookup, or hard-coded? Owner: TBD — Due: TBD.
3. **Schema-merge semantics** — CCT-1222 says "merging the 2 content type schemas" (charge + enforcement). Should the merged `other-cases` schema use a discriminator field (e.g. `hearingType`/case-marker) or is it a strict union with conditional-mandatory fields per `Civil Charge` vs `Civil Enforcement` columns in the Field Definitions tables? Owner: TBD.
4. **Internal command/handler renames** — should the existing internal command names (`charge-prosecution`, file `stagingprosecutorscivil.charge-prosecution.json`) be renamed in lockstep with the external content-type, or kept stable to avoid event-store/aggregate impact? Owner: TBD (architecture).
5. **CCT-1222 ticket scope** — does it cover only the externally-facing rename, or also the internal RAML, command names, view-store columns and downstream domain events? Pull the Jira ticket to confirm.
6. **Confluence page revision** — the spec is `Status: Draft` v1.1 dated 22/04/2026. Confirm sign-off before implementation begins; v1.1 may still change.
7. **Test data for OFFENCE_CODE_NOT_SUPPORTED** — what known Criminal-CJS / blacklisted codes should integration tests use to exercise the new path? Owner: TBD.
