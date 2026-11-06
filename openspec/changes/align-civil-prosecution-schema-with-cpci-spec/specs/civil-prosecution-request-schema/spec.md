## REMOVED Requirements

### Requirement: Defendant alias names
**Reason**: The CPCI External API Specification v1.4's Alias Array field table (Alias-Forename, Alias-Forename2, Alias-Forename3, Alias-Surname) is struck through in its entirety, indicating this attribute set is deprecated and no longer part of the agreed contract.
**Migration**: Callers currently sending `individual.aliases` must stop sending it. There is no replacement field.

#### Scenario: Request with aliases is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `individual.aliases`
- **THEN** the request is rejected because `aliases` is no longer a recognised property of the individual schema

### Requirement: Defendant bail conditions free text
**Reason**: `bailConditions` does not appear anywhere in the CPCI specification's Defendant Related Fields table.
**Migration**: Callers currently sending `individual.bailConditions` must stop sending it. There is no replacement field; `custodyStatus` remains the supported way to convey bail/custody state.

#### Scenario: Request with bailConditions is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `individual.bailConditions`
- **THEN** the request is rejected because `bailConditions` is no longer a recognised property of the individual schema

### Requirement: Defendant number of previous convictions
**Reason**: `numPreviousConvictions` does not appear anywhere in the CPCI specification's Defendant Related Fields table.
**Migration**: Callers currently sending `defendantDetails.numPreviousConvictions` must stop sending it. There is no replacement field.

#### Scenario: Request with numPreviousConvictions is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `defendantDetails.numPreviousConvictions`
- **THEN** the request is rejected because `numPreviousConvictions` is no longer a recognised property of the defendant-details schema

### Requirement: Organisation alias names
**Reason**: `aliasOrganisationNames` does not appear anywhere in the CPCI specification's Case/Prosecution or Defendant Related Fields tables.
**Migration**: Callers currently sending `organisation.aliasOrganisationNames` must stop sending it. There is no replacement field.

#### Scenario: Request with aliasOrganisationNames is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `organisation.aliasOrganisationNames`
- **THEN** the request is rejected because `aliasOrganisationNames` is no longer a recognised property of the organisation schema

### Requirement: Offence back duty fields
**Reason**: `backDuty`, `backDutyDateFrom`, and `backDutyDateTo` do not appear anywhere in the CPCI specification's Offence Related Fields table or its worked example.
**Migration**: Callers currently sending these three fields on an offence must stop sending them. There is no replacement field.

#### Scenario: Request with back duty fields is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `offenceDetails.backDuty`, `offenceDetails.backDutyDateFrom`, or `offenceDetails.backDutyDateTo`
- **THEN** the request is rejected because these fields are no longer recognised properties of the offence-details schema

### Requirement: Offence vehicle fields
**Reason**: `vehicleMake` and `vehicleRegistrationMark` are not listed in the CPCI specification's Offence Related Fields table. Although they appear in the specification's worked example payload, the field-definition table is the authoritative source for this schema's contract.
**Migration**: Callers currently sending `offenceDetails.vehicleMake` or `offenceDetails.vehicleRegistrationMark` must stop sending them. There is no replacement field.

#### Scenario: Request with vehicle fields is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `offenceDetails.vehicleMake` or `offenceDetails.vehicleRegistrationMark`
- **THEN** the request is rejected because these fields are no longer recognised properties of the offence-details schema

### Requirement: Offence date code
**Reason**: `offenceDateCode` is not listed as its own row in the CPCI specification's Offence Related Fields table; it is only referenced within another field's business-rule text, which is insufficient to establish it as a supported field in isolation.
**Migration**: Callers currently sending `offenceDetails.offenceDateCode` must stop sending it. There is no replacement field.

#### Scenario: Request with offenceDateCode is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `offenceDetails.offenceDateCode`
- **THEN** the request is rejected because `offenceDateCode` is no longer a recognised property of the offence-details schema

### Requirement: Defendant occupation code
**Reason**: The CPCI specification's Defendant Related Fields table lists this field as `defendantOccupationCode` with the entire row struck through, indicating it is deprecated. The wire field (`occupationCode`, as shown in the schema and the specification's own example payload) is the same field the struck-through row describes.
**Migration**: Callers currently sending `individual.occupationCode` must stop sending it. There is no replacement field; `occupation` (free-text description) remains supported.

#### Scenario: Request with occupationCode is rejected
- **WHEN** a charge-prosecution or summons-prosecution request includes `individual.occupationCode`
- **THEN** the request is rejected because `occupationCode` is no longer a recognised property of the individual schema
