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

## Architecture Overview

This is an event-sourced, CQRS microservice built on the **HMCTS CPP Framework** (Common Platform Program). It handles civil prosecution submissions — accepting charge and summons requests from prosecutors, tracking their lifecycle, and projecting status into a queryable view.

### Request Flow

1. **Command API** (`stagingprosecutorscivil-command-api`) receives REST requests (charge-prosecution, summons-prosecution, submit-material) and forwards them as commands. Returns a `UrlResponse` with a `submissionId` and status URL.
2. **Command Handler** (`stagingprosecutorscivil-command-handler`) handles the commands, loads or creates a `ProsecutionSubmissionAggregate` via `AggregateService`, and appends events to the event stream.
3. **Domain Aggregate** (`stagingprosecutorscivil-domain-aggregate`) contains `ProsecutionSubmissionAggregate` which produces internal private events (e.g. `charge-prosecution-received`, `summons-prosecution-received`).
4. **Event Listener** (`stagingprosecutorscivil-event-listener`) consumes those internal events and persists/updates `Submission` entities in the viewstore via `SubmissionRepository`.
5. **Event Processor** (`stagingprosecutorscivil-event-processor`) consumes external *public* events from the Prosecution Case File service (`public.prosecutioncasefile.civil.*`) and translates them into internal `update-civil-case` commands, closing the feedback loop.
6. **Query API** (`stagingprosecutorscivil-query-api`) exposes a REST endpoint to look up submission status by UUID, delegating to `CivilProsecutionQueryView` which reads from the viewstore.

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

- Internal commands: `stagingprosecutorscivil.command.<name>`
- Internal events: `stagingprosecutorscivil.event.<name>`
- External public events consumed: `public.prosecutioncasefile.civil.<name>`

The event processor filters by `CIVIL` channel — only messages with that channel tag are processed.

### Domain Message Classes

Java classes for commands and events (e.g. `ChargeProsecution`, `ChargeProsecutionReceived`) are generated from JSON schemas located in `src/raml/json/schema/` within each module. When adding a new command or event, define the schema first, then regenerate.

### ViewStore

The viewstore uses JPA with PostgreSQL. The core entity is `Submission` (table: `submission`) which stores a `submissionId` (UUID PK), `submissionStatus`, `ouCode`, and several `JsonArray` columns (errors, warnings, case/defendant variants). `CaseDetail` is a child entity linked to `Submission` storing case URNs. Schema migrations are managed by Liquibase (`stagingprosecutorscivil-viewstore-liquibase`).

### Testing Patterns

Unit tests use JUnit 5 with `@ExtendWith(MockitoExtension.class)`. Handler tests rely on CPP framework test utilities:

```java
// Verify a handler is wired to the correct message name
assertThat(handler, isHandler(COMMAND_HANDLER)
    .with(method("handleChargeProsecution")
        .thatHandles("stagingprosecutorscivil.command.charge-prosecution")));

// Verify events appended to the event stream
final Stream<JsonEnvelope> stream = verifyAppendAndGetArgumentFrom(eventStream);
assertThat(stream, streamContaining(
    jsonEnvelope(metadata().withName("stagingprosecutorscivil.event.charge-prosecution-received"),
                 payload().isJson(withJsonPath("$.submissionId", notNullValue())))));
```

The `Enveloper` spy in handler tests must be initialised with `createEnveloperWithEvents(...)` listing all event classes the handler can emit.

Integration tests (`stagingprosecutorscivil-integration-test`) run against a live WildFly container via Docker and use WireMock to stub the Prosecution Case File and System ID Mapper services.

### CI/CD

Azure Pipelines runs on `main` and `team/*` branches. The pipeline uses Java 17 on CentOS 8 and includes SonarQube analysis (`uk.gov.moj.cpp.stagingprosecutorscivil:stagingprosecutorscivil-parent`). Secret scanning runs on PRs via GitHub Actions (Gitleaks).
