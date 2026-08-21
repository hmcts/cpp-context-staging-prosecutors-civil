# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Full build
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Run all unit tests for a specific module
mvn test -pl stagingprosecutorscivil-command/stagingprosecutorscivil-command-handler

# Run a single test class
mvn test -pl stagingprosecutorscivil-command/stagingprosecutorscivil-command-handler -Dtest=CivilProsecutionHandlerTest

# Run integration tests (requires Docker and CPP_DOCKER_DIR env var pointing to cpp-developers-docker repo)
./runIntegrationTests.sh
```

`runIntegrationTests.sh` builds WARs, runs all Liquibase changesets (event log, aggregate snapshot, event buffer, viewstore, system, event tracking, file service), deploys WireMock stubs plus the WARs into the shared WildFly container, runs healthchecks, then executes `stagingprosecutorscivil-integration-test`.

## Architecture Overview

This is an event-sourced, CQRS microservice built on the **HMCTS CPP Framework**. It handles civil prosecution submissions — accepting "other case" (charge) and summons requests from prosecutors, plus post-hoc material (document) submissions against an existing case, tracks their lifecycle, and projects status into a queryable view.

There are two independent submission flows sharing the same `submission` viewstore table and status-lookup endpoint, but backed by two separate aggregates:

- **Case submission flow** — `other-case` / `summons` → `ProsecutionSubmissionAggregate`
- **Material submission flow** — `submit-material` → `MaterialSubmission` aggregate

### Case Submission Flow

1. **Command API** (`stagingprosecutorscivil-command-api`, class `CivilProsecutionApi`) handles REST requests `stagingcivil.other-case` and `stagingcivil.summons` at `POST /cases` (content negotiated via the `application/vnd.stagingcivil.other-case+json` / `...summons+json` media types), generates a `submissionId`, and forwards `stagingcivil.command.other-case` / `stagingcivil.command.summons` commands. Returns a `UrlResponse` (202) with a status URL built from `submissionId`.
2. **Command Handler** (`stagingprosecutorscivil-command-handler`, class `CivilProsecutionHandler`) loads/creates a `ProsecutionSubmissionAggregate` via `AggregateService` and appends the resulting events to the event stream. Also handles `stagingcivil.command.update-civil-case`, which comes back from the event processor (step 5 below), not from the REST layer.
3. **Domain Aggregate** (`stagingprosecutorscivil-domain-aggregate`, class `ProsecutionSubmissionAggregate`) raises private events: `stagingprosecutorscivil.event.other-case-received`, `stagingprosecutorscivil.event.summons-received`, `stagingprosecutorscivil.event.update-civil-case-received`.
4. **Event Listener** (`stagingprosecutorscivil-event-listener`, class `SubmissionEventListener`) consumes those private events and upserts `Submission`/`CaseDetail` entities in the viewstore via `SubmissionRepository`. On `update-civil-case-received` it sets `errors`/`caseErrors`/`defendantErrors` for `REJECTED`, or `warnings`/`caseWarnings`/`defendantWarnings` for `SUCCESS_WITH_WARNINGS`.
5. **Event Processor** (`stagingprosecutorscivil-event-processor`, class `ProsecutionEventProcessor`) consumes its own `other-case-received`/`summons-received` events, converts them to Prosecution Case File `InitiateProsecution`/`InitiateGroupProsecution` commands (via `ProsecutionCaseToGroupProsecutionConverterForOthers`/`...ForSummons`), and separately consumes external *public* events from Prosecution Case File (`public.prosecutioncasefile.civil-prosecution-rejected`, `public.prosecutioncasefile.group-prosecution-rejected`, and — in `ProsecutionSubmissionSucceededPublicEventProcessor`/`GroupSubmissionSucceededPublicEventProcessor` — the `*-submission-succeeded[-with-warnings]` events), translating them into `stagingcivil.command.update-civil-case` commands to close the feedback loop. Public events are filtered to the `CIVIL` channel (`uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL`).
6. **Query API/View** (`stagingprosecutorscivil-query-api` / `-query-view`) exposes `CivilProsecutionQueryApi` → `CivilProsecutionQueryView.querySubmission`, which looks up `Submission` by `submissionId` and returns id/status/warnings/errors (+ caseErrors/defendantErrors when present) as `stagingcivil.query.submission-details`.

### Material Submission Flow

1. **Command API** `CivilProsecutionApi.submitMaterial` handles `stagingcivil.submit-material` at `POST /v1/prosecutions/{caseUrn}/materials` (multipart form upload), validates the payload against its JSON schema explicitly (`jsonSchemaValidator.validate(...)`), and sends `stagingcivil.command.submit-material`.
2. **Command Handler** `MaterialHandler` handles `submit-material`, `reject-material`, and `receive-material-submission-successful` commands against the `MaterialSubmission` aggregate.
3. **Domain Aggregate** `MaterialSubmission` raises `MaterialSubmitted`, `MaterialSubmissionRejected`, `MaterialSubmissionSuccessful` private events.
4. **Event Listener** `SubmissionEventListener` handles `stagingprosecutorscivil.event.material-submitted` (creates a `Submission` with `type=MATERIAL`), `...material-submission-rejected`, and `...material-submission-successful` (sets `completedAt` + status `SUCCESS`).
5. **Event Processor** classes `MaterialSubmittedProcessor` / `SystemIdMapperService` push material to the downstream System ID Mapper / Prosecution Case File services and the loop closes the same way via `receive-material-submission-successful` / `reject-material` commands.

### CPP Framework Component Types

Classes are annotated with `@ServiceComponent(<type>)` which determines how the framework routes messages to them:

| Annotation | Module pattern | Role |
|---|---|---|
| `COMMAND_API` | `*-command-api` | REST → command dispatch |
| `COMMAND_HANDLER` | `*-command-handler` | Command → aggregate → event store |
| `EVENT_LISTENER` | `*-event-listener` | Internal event → viewstore write |
| `EVENT_PROCESSOR` | `*-event-processor` | External public event → new command |
| `QUERY_API` | `*-query-api` | REST → viewstore read |

### Message Naming Conventions

REST APIs and internal commands consistently use the `stagingcivil.*` prefix. Internal *events*, however, still use the longer `stagingprosecutorscivil.event.*` prefix — this is a deliberate, narrower scope than the REST/command rename (see below):

- REST APIs: `stagingcivil.other-case`, `stagingcivil.summons`, `stagingcivil.submit-material`
- Internal commands: `stagingcivil.command.other-case`, `stagingcivil.command.summons`, `stagingcivil.command.update-civil-case`, `stagingcivil.command.submit-material`, `stagingcivil.command.reject-material`, `stagingcivil.command.receive-material-submission-successful`
- Internal events: `stagingprosecutorscivil.event.<name>` (e.g. `other-case-received`, `summons-received`, `update-civil-case-received`, `material-submitted`, `material-submission-rejected`, `material-submission-successful`)
- External public events consumed: `public.prosecutioncasefile.civil-prosecution-rejected`, `public.prosecutioncasefile.group-prosecution-rejected`, `public.prosecutioncasefile.prosecution-submission-succeeded[-with-warnings]`, `public.prosecutioncasefile.group-submission-succeeded`, `public.prosecutioncasefile.group-submission-failed`
- Query: `stagingcivil.query.submission-details`

When adding a new message, check which prefix convention the *adjacent* messages in that flow use rather than assuming one global prefix.

Access control rules live in Drools files (`*/accesscontrol/*.drl`), keyed by the exact message name (e.g. `command-api.drl` matches `Action(name == "stagingcivil.other-case")`) against permissions in `RuleConstants`.

### Domain Message Classes

Java command/event/schema classes (`OtherCase`, `Summons`, `OtherCaseReceived`, `SubmitMaterialCommand`, etc.) are generated at build time from JSON schemas under `src/raml/json/schema/` in each module (command-api, command-handler, event-processor). Each module generates its own copy of shared types under module-specific packages (e.g. `...command.api.OtherCase` vs `...command.handler.OtherCase`) — when adding a field, the schema must be updated in every module that has its own copy. When adding a new command or event, define/update the schema first, then rebuild to regenerate the Java classes; never hand-edit generated sources.

### ViewStore

JPA + PostgreSQL, module `stagingprosecutorscivil-viewstore-persistance`. The core entity is `Submission` (table `submission`): `submissionId` (UUID PK), `submissionStatus`, `ouCode`, `type` (`SubmissionType`: `PROSECUTION` / `MATERIAL`), `receivedAt`/`completedAt`, and `JsonArray` columns for `errors`/`warnings`/`caseErrors`(`groupCaseErrors`)/`defendantErrors`/`caseWarnings`/`defendantWarnings` (via `JsonArrayConverter`). `CaseDetail` is a child entity (one-to-many, cascade all, orphan removal) storing case URNs. Access is via `SubmissionRepository`. Schema migrations are Liquibase changesets in `stagingprosecutorscivil-viewstore-liquibase`.

### Testing Patterns

Unit tests use JUnit 5 with `@ExtendWith(MockitoExtension.class)`. Handler tests rely on CPP framework test utilities:

```java
// Verify a handler is wired to the correct message name
assertThat(handler, isHandler(COMMAND_HANDLER)
    .with(method("handleOtherCase")
        .thatHandles("stagingcivil.command.other-case")));

// Verify events appended to the event stream
final Stream<JsonEnvelope> stream = verifyAppendAndGetArgumentFrom(eventStream);
assertThat(stream, streamContaining(
    jsonEnvelope(metadata().withName("stagingprosecutorscivil.event.other-case-received"),
                 payload().isJson(withJsonPath("$.submissionId", notNullValue())))));
```

The `Enveloper` spy in handler tests must be initialised with `createEnveloperWithEvents(...)` listing every event class the handler under test can emit (e.g. `CivilProsecutionHandlerTest` lists `OtherCaseReceived`, `SummonsReceived`, `UpdateCivilCaseReceived`, `MaterialSubmitted`).

Schema-validation tests (`*SchemaValidationTest`, extending `AbstractProsecutionSchemaValidationTest`) assert request payloads validate against the RAML JSON schemas independent of the handler logic.

Integration tests (`stagingprosecutorscivil-integration-test`) run against a live WildFly container via Docker and use WireMock to stub the Prosecution Case File and System ID Mapper services.

### Other Modules

- `stagingprosecutorscivil-apim-policy` — Azure API Management policy XML/OpenAPI config for exposing the command/query endpoints externally.
- `stagingprosecutorscivil-event-sources` — declares the JMS event sources (`stagingprosecutorscivil.event` topic, plus the `public.event` topic this service subscribes to) in `src/yaml/event-sources.yaml`.
- `stagingprosecutorscivil-healthchecks` — custom healthcheck provider (`CivilIgnoredHealthcheckNamesProvider`).

### CI/CD

Azure Pipelines runs on `main` and `team/*` branches. The pipeline uses Java 17 on CentOS 8 and includes SonarQube analysis (`uk.gov.moj.cpp.stagingprosecutorscivil:stagingprosecutorscivil-parent`). Secret scanning runs on PRs via GitHub Actions (Gitleaks).
