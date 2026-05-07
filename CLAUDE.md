# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

`cpp-context-staging-prosecutors-civil` is a **legacy HMCTS CPP context service** (WildFly + Maven multi-module, CQRS). It is **not** a Spring Boot service — do not apply Spring Boot / HMCTS-template patterns here. For deeper guidance on legacy context services use the `context-service-guide` skill (available via the symlinked `.claude/`).

- Maven coordinates: `uk.gov.moj.cpp.staging.prosecutors.civil:stagingprosecutorscivil`
- Parent: `uk.gov.moj.cpp.common:service-parent-pom` (provides framework versions: `framework.version`, `framework-libraries.version`, `event-store.version` — read via `mvn help:evaluate -Dexpression=<name> -q -DforceStdout`)
- Service name (used by deploy / JMX / IT tooling): `stagingprosecutorscivil`

## Module layout (CQRS)

Standard cpp-context-* layout. Each top-level Maven module groups related sub-modules:

- `stagingprosecutorscivil-command/` — `command-api` (RAML) + `command-handler`
- `stagingprosecutorscivil-event/` — `event-listener` + `event-processor`
- `stagingprosecutorscivil-domain/` — `domain-aggregate`, `domain-message`, `domain-common`, `datatypes-common`
- `stagingprosecutorscivil-query/` — `query-api` (RAML) + `query-view`
- `stagingprosecutorscivil-viewstore/` — `viewstore-liquibase` (read-model schema) + `viewstore-persistance`
- `stagingprosecutorscivil-service/` — top-level WAR / wiring
- `stagingprosecutorscivil-event-sources/` — declares which other contexts' events this service subscribes to
- `stagingprosecutorscivil-healthchecks/`
- `stagingprosecutorscivil-integration-test/`

When mapping integrations: `event-sources` reveals upstream contexts, `*-domain/*-domain-event/src/main/resources/json/schema/` reveals events this service publishes, and `*-command-api/src/raml/` / `*-query-api/src/raml/` reveal accepted operations and exposed data.

## Build and test

Standard Maven from the repo root:

```bash
mvn clean install              # full build (all modules)
mvn -pl <module> -am install   # build one module + its dependencies
mvn -pl <module> test          # unit tests for one module
mvn -Dtest=ClassName#method test    # single test method
```

### Integration tests (`runIntegrationTests.sh`)

Requires the `cpp-developers-docker` repo checked out and `CPP_DOCKER_DIR` exported pointing at it. The script:

1. Logs into the docker registry and builds WARs
2. Starts WildFly + supporting containers
3. Runs Liquibase against event-log / event-buffer / view-store / system / event-tracking schemas
4. Deploys WireMock + WARs, runs healthchecks, then the IT suite

Pre-req: `export CPP_DOCKER_DIR=/path/to/hmcts/cpp-developers-docker`

### Liquibase against a local view store (`runLiquibase.sh`)

Runs view-store Liquibase only against `localhost:5432` with creds `stagingprosecutorscivil/stagingprosecutorscivil`. Useful when iterating on view-store changelogs without spinning up the full IT stack.

### Runtime JMX commands (`runSystemCommand.sh`)

Wrapper around `framework-jmx-command-client` for invoking system commands (e.g. `CATCHUP`) against a running deployment.

- `./runSystemCommand.sh` — list available commands
- `./runSystemCommand.sh <COMMAND>` — run one
- `./runSystemCommand.sh --help` — client help

## CI

`azure-pipelines.yaml` delegates to the shared `hmcts/cpp-azure-devops-templates` repo:

- PR builds → `pipelines/context-verify.yaml`
- CI builds (post-merge) → `pipelines/context-validation.yaml`

SonarQube project key: `uk.gov.moj.cpp.stagingprosecutorscivil:stagingprosecutorscivil-parent`. IT folder declared to the pipeline: `stagingprosecutorscivil-integration-test`.

## Branching / release

Parent POM sets `jgitflow.maven.developBranchName=main`. Recent history shows the jgitflow pattern: `dev/release-<version>` branches are merged into `dev/release` then into `main`, with a follow-up commit reverting develop POMs back to SNAPSHOT versions to avoid merge conflicts. Don't manually edit version numbers across module POMs — let the release tooling do it.

## .claude/ symlink

`./.claude` is a symlink to `../claude/.claude` (the shared HMCTS Claude tooling repo). Agents, commands, skills, and hooks defined there apply here. The `context-service-guide` skill is the canonical reference for navigating this kind of repo.
