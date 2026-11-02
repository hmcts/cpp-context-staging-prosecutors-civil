package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.CHARGE_PROSECUTION_SCHEMA_FILE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_CHARGE_PROSECUTION_REQUEST;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_MULTI_PROSECUTION_CASE_REQUEST;

import java.util.List;
import java.util.stream.Stream;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChargeProsecutionSchemaValidationTest extends AbstractProsecutionSchemaValidationTest {

    @Override
    Schema loadSchema() {
        return buildSchema(CHARGE_PROSECUTION_SCHEMA_FILE);
    }

    @Test
    @DisplayName("Valid charge-prosecution request passes schema validation")
    void testValidRequest() {
        assertDoesNotThrow(() -> schema.validate(loadJson(VALID_CHARGE_PROSECUTION_REQUEST)));
    }

    @Test
    @DisplayName("summonsCode absent — charge-prosecution schema does not require summonsCode")
    void testSummonsCodeNotRequiredForCharge() {
        assertDoesNotThrow(() -> schema.validate(loadJson(VALID_CHARGE_PROSECUTION_REQUEST)));
    }

    @DisplayName("Charge-Prosecution — informant is mandatory (optional for summons-prosecution)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("informantScenarios")
    void testInformantViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }

    static Stream<Arguments> informantScenarios() {
        return Stream.of(
                Arguments.of(
                        "missing mandatory field: prosecution case informant absent",
                        JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST).remove("prosecutionCases[0].informant").build(),
                        List.of("informant")),
                Arguments.of(
                        "second prosecution case: informant absent — required field validated on every prosecution case",
                        JsonRequestBuilder.from(VALID_MULTI_PROSECUTION_CASE_REQUEST).remove("prosecutionCases[1].informant").build(),
                        List.of("informant"))
        );
    }
}
