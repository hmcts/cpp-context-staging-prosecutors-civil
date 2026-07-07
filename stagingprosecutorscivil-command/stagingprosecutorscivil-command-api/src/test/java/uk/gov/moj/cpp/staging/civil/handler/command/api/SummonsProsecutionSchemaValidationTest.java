package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.SUMMONS_PROSECUTION_SCHEMA_FILE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_SUMMONS_PROSECUTION_REQUEST;

import java.util.List;
import java.util.stream.Stream;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SummonsProsecutionSchemaValidationTest extends AbstractProsecutionSchemaValidationTest {

    private static final String PROSECUTION_CASE = "prosecutionCases[0]";

    @Override
    Schema loadSchema() {
        return buildSchema(SUMMONS_PROSECUTION_SCHEMA_FILE);
    }

    @Test
    @DisplayName("Valid summons-prosecution request passes schema validation")
    void testValidRequest() {
        assertDoesNotThrow(() -> schema.validate(loadJson(VALID_SUMMONS_PROSECUTION_REQUEST)));
    }

    @Test
    @DisplayName("summonsCode 'E' is a valid value")
    void testSummonsCodeE() {
        assertDoesNotThrow(() -> schema.validate(baseSummons().set("E", PROSECUTION_CASE + ".summonsCode").build()));
    }

    @Test
    @DisplayName("summonsCode 'M' is a valid value")
    void testSummonsCodeM() {
        assertDoesNotThrow(() -> schema.validate(baseSummons().set("M", PROSECUTION_CASE + ".summonsCode").build()));
    }

    @Test
    @DisplayName("summonsCode 'W' is a valid value")
    void testSummonsCodeW() {
        assertDoesNotThrow(() -> schema.validate(baseSummons().set("W", PROSECUTION_CASE + ".summonsCode").build()));
    }

    private static JsonRequestBuilder baseSummons() {
        return JsonRequestBuilder.from(VALID_SUMMONS_PROSECUTION_REQUEST);
    }

    @DisplayName("Summons-Prosecution — summonsCode Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("summonsCodeScenarios")
    void testSummonsCodeViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }

    static Stream<Arguments> summonsCodeScenarios() {
        return Stream.of(
                Arguments.of(
                        "summonsCode absent — required field for summons-prosecution",
                        baseSummons().remove(PROSECUTION_CASE + ".summonsCode").build(),
                        List.of("summonsCode")),
                Arguments.of(
                        "summonsCode 'S' does not match pattern — allowed values are A, E, M, W",
                        baseSummons().set("S", PROSECUTION_CASE + ".summonsCode").build(),
                        List.of("summonsCode", "does not match pattern"))
        );
    }
}
