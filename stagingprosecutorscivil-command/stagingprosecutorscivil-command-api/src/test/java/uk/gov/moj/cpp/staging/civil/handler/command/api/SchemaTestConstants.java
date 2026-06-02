package uk.gov.moj.cpp.staging.civil.handler.command.api;

class SchemaTestConstants {

    private SchemaTestConstants() {}

    // schemas
    static final String CHARGE_PROSECUTION_SCHEMA_FILE  = "/json/schema/stagingprosecutorscivil.charge-prosecution.json";
    static final String SUMMONS_PROSECUTION_SCHEMA_FILE = "/json/schema/stagingprosecutorscivil.summons-prosecution.json";

    // valid requests
    static final String VALID_CHARGE_PROSECUTION_REQUEST   = "/valid-charge-prosecution-request.json";
    static final String VALID_SUMMONS_PROSECUTION_REQUEST  = "/valid-summons-prosecution-request.json";

    // conditional mandatory: forename, surname, gender — all present and valid for individual defendant
    static final String VALID_INDIVIDUAL_DEFENDANT_REQUEST  = "/valid-individual-defendant-request.json";
    // multiple defendants in one prosecution case — schema allows unbounded array (minItems: 1, no maxItems)
    static final String VALID_MULTI_DEFENDANT_REQUEST       = "/valid-multi-defendant-request.json";
    // multiple offences per defendant — schema allows unbounded array (minItems: 1, no maxItems)
    static final String VALID_MULTI_OFFENCE_REQUEST         = "/valid-multi-offence-request.json";
    // multiple prosecution cases — schema allows unbounded array (minItems: 1, no maxItems)
    static final String VALID_MULTI_PROSECUTION_CASE_REQUEST = "/valid-multi-prosecution-case-request.json";

    // conditional mandatory: organisationName — present and valid for organisation defendant
    static final String VALID_ORGANISATION_DEFENDANT_REQUEST = "/valid-organisation-defendant-request.json";

    // URN allowed characters: A-Z, a-z, 0-9, hyphen (-)
    static final String URN_PATTERN = "^[A-Za-z0-9-]+$";
}
