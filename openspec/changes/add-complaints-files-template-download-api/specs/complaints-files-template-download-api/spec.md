## ADDED Requirements

### Requirement: Download complaints files CSV template
The system SHALL provide a `GET /complaints-files-template` query-api endpoint that returns the complaints-files CSV template as a downloadable attachment.

#### Scenario: Authorised caller downloads the template
- **WHEN** a caller holding the `BULK_CASE`/`GrantAccess` permission sends `GET /complaints-files-template`
- **THEN** the response has status `200 OK`, header `Content-Type: text/csv`, header `Content-Disposition: attachment; filename="complaints-files-template.csv"`, and a body containing the CSV template content

#### Scenario: Missing template resource
- **WHEN** the packaged `complaints-files-template.csv` classpath resource is not present
- **THEN** the response has status `404 Not Found` with a body indicating the template was not found, rather than an unhandled server error

### Requirement: Access control for complaints files template download
The system SHALL restrict `GET /complaints-files-template` to callers holding the `BULK_CASE`/`GrantAccess` permission, following the same `hasPermission` rule pattern as the existing `submission-details` endpoint's `CIVIL_CASE`/`GrantAccess` check.

#### Scenario: Caller without the required permission is denied
- **WHEN** a caller who does not hold the `BULK_CASE`/`GrantAccess` permission sends `GET /complaints-files-template`
- **THEN** the request is rejected by the access-control rule for `stagingprosecutorscivil.complaints-files-template`
