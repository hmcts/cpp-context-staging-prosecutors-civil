package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.SUMMONS_PROSECUTION_SCHEMA_FILE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_SUMMONS_PROSECUTION_REQUEST;

import org.everit.json.schema.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SummonsProsecutionSchemaValidationTest extends AbstractProsecutionSchemaValidationTest {

    @Override
    Schema loadSchema() {
        return buildSchema(SUMMONS_PROSECUTION_SCHEMA_FILE);
    }

    @Test
    @DisplayName("Valid summons-prosecution request passes schema validation")
    void testValidRequest() {
        assertDoesNotThrow(() -> schema.validate(loadJson(VALID_SUMMONS_PROSECUTION_REQUEST)));
    }
}
