package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.INVALID_VALUE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.REQUIRED_FIELD_MISSING;

import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileConversionResult.ConvertedRow;

import java.util.List;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Runs a real everit schema so the mapping is exercised against the violation tree and JSON
 * pointers everit actually produces, mirroring the shape of {@code stagingcivil.summons}.
 */
class SummonsSchemaViolationMapperTest {

    private static final JSONObject OFFENCE_SCHEMA = objectSchema(
            "offenceDetails", objectSchema("offenceWording", stringWithMaxLength(5)));

    private static final JSONObject INDIVIDUAL_SCHEMA = objectSchema(
            "dateOfBirth", stringWithPattern("^\\d{4}-\\d{2}-\\d{2}$"),
            "nameDetails", objectSchema())
            .put("required", array("nameDetails"));

    private static final JSONObject DEFENDANT_SCHEMA = objectSchema(
            "defendantDetails", objectSchema("asn", stringWithMaxLength(5)),
            "individual", INDIVIDUAL_SCHEMA,
            "offences", arrayOf(OFFENCE_SCHEMA));

    private static final JSONObject PROSECUTION_CASE_SCHEMA = objectSchema(
            "summonsCode", stringWithMaxLength(1),
            "defendants", arrayOf(DEFENDANT_SCHEMA));

    private static final JSONObject SCHEMA = objectSchema(
            "hearingDetails", objectSchema("timeOfHearing", stringWithPattern("^\\d{2}:\\d{2}:\\d{2}$")),
            "prosecutionCases", arrayOf(PROSECUTION_CASE_SCHEMA));

    private static final List<ConvertedRow> CONVERTED_ROWS = List.of(
            new ConvertedRow(1L, "SCIV00001", "DEF00001"),
            new ConvertedRow(3L, "SCIV00003", "DEF00003"));

    @Test
    void mapsEachViolationToItsCsvRowCaseReferenceColumnAndRawValue() {
        final JSONObject summons = object(
                "hearingDetails", object("timeOfHearing", "9am"),
                "prosecutionCases", array(
                        object(
                                "summonsCode", "A",
                                "defendants", array(object("individual", object("nameDetails", object())))),
                        object(
                                "summonsCode", "AB",
                                "defendants", array(object(
                                        "defendantDetails", object("asn", "TOOLONGASN"),
                                        "individual", object("dateOfBirth", "1990"),
                                        "offences", array(object(
                                                "offenceDetails", object("offenceWording", "far too long"))))))));

        final List<ComplaintsFileValidationError> errors =
                SummonsSchemaViolationMapper.map(schemaFailureFor(summons), CONVERTED_ROWS, summons.toString());

        assertThat(errors, containsInAnyOrder(
                error(1L, "SCIV00001", INVALID_VALUE, "hearingDetails.timeOfHearing", "9am"),
                error(3L, "SCIV00003", INVALID_VALUE, "case.summonsCode", "AB"),
                error(3L, "SCIV00003", INVALID_VALUE, "defendant.asn", "TOOLONGASN"),
                error(3L, "SCIV00003", REQUIRED_FIELD_MISSING, "defendant.individual.nameDetails", ""),
                error(3L, "SCIV00003", INVALID_VALUE, "defendant.individual.dateOfBirth", "1990"),
                error(3L, "SCIV00003", INVALID_VALUE, "offence.offenceDetails.offenceWording", "far too long")));
    }

    @Test
    void carriesTheRowsDefendantReferenceForCaseViolationsButNotForSubmissionLevelOnes() {
        final JSONObject summons = object(
                "hearingDetails", object("timeOfHearing", "9am"),
                "prosecutionCases", array(
                        object("summonsCode", "A"),
                        object("summonsCode", "A", "defendants", array(object(
                                "defendantDetails", object("asn", "TOOLONGASN"))))));

        final List<ComplaintsFileValidationError> errors =
                SummonsSchemaViolationMapper.map(schemaFailureFor(summons), CONVERTED_ROWS, summons.toString());

        assertThat(errors, containsInAnyOrder(
                allOf(hasProperty("field", is("hearingDetails.timeOfHearing")), hasProperty("defendantReference", nullValue())),
                allOf(hasProperty("field", is("defendant.asn")), hasProperty("defendantReference", is("DEF00003")))));
    }

    @Test
    void keepsAReadableMessageForLoggingAndStrictConversion() {
        final JSONObject summons = object("prosecutionCases", array(object("summonsCode", "AB")));

        final List<ComplaintsFileValidationError> errors =
                SummonsSchemaViolationMapper.map(schemaFailureFor(summons), CONVERTED_ROWS, summons.toString());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), startsWith("Row 1: 'case.summonsCode' "));
    }

    @Test
    void reportsSubmissionLevelViolationsAgainstRowOneEvenWhenRowOneDidNotConvert() {
        final JSONObject summons = object(
                "hearingDetails", object("timeOfHearing", "9am"),
                "prosecutionCases", array());
        final List<ConvertedRow> rowOneFailedConversion = List.of(new ConvertedRow(2L, "SCIV00002", "DEF00002"));

        final List<ComplaintsFileValidationError> errors =
                SummonsSchemaViolationMapper.map(schemaFailureFor(summons), rowOneFailedConversion, summons.toString());

        assertThat(errors, contains(error(1L, null, INVALID_VALUE, "hearingDetails.timeOfHearing", "9am")));
    }

    @Test
    void reportsASingleGenericErrorWhenTheFailureCarriesNoViolationDetail() {
        final List<ComplaintsFileValidationError> errors = SummonsSchemaViolationMapper.map(
                new JsonSchemaValidationException("schema unavailable"), CONVERTED_ROWS, object().toString());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getRowNumber(), is(1L));
        assertThat(errors.get(0).getCaseReference(), is("SCIV00001"));
        assertThat(errors.get(0).getCode(), is(INVALID_VALUE));
        assertThat(errors.get(0).getField(), is(nullValue()));
        assertThat(errors.get(0).getValue(), is(""));
    }

    private static JsonSchemaValidationException schemaFailureFor(final JSONObject summons) {
        final Schema schema = SchemaLoader.load(SCHEMA);
        final ValidationException violation = assertThrows(ValidationException.class, () -> schema.validate(summons));
        return new JsonSchemaValidationException(violation.getMessage(), violation);
    }

    private static JSONObject objectSchema(final Object... propertyNamesAndSchemas) {
        return object("type", "object", "properties", object(propertyNamesAndSchemas));
    }

    private static JSONObject stringWithPattern(final String pattern) {
        return object("type", "string", "pattern", pattern);
    }

    private static JSONObject stringWithMaxLength(final int maxLength) {
        return object("type", "string", "maxLength", maxLength);
    }

    private static JSONObject arrayOf(final JSONObject itemSchema) {
        return object("type", "array", "items", itemSchema);
    }

    private static JSONObject object(final Object... keysAndValues) {
        final JSONObject object = new JSONObject();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            object.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return object;
    }

    private static JSONArray array(final Object... values) {
        final JSONArray array = new JSONArray();
        for (final Object value : values) {
            array.put(value);
        }
        return array;
    }

    private static Matcher<ComplaintsFileValidationError> error(final long rowNumber, final String caseReference,
                                                                final String code, final String field, final String value) {
        return allOf(
                hasProperty("rowNumber", is(rowNumber)),
                hasProperty("caseReference", is(caseReference)),
                hasProperty("code", is(code)),
                hasProperty("field", is(field)),
                hasProperty("value", is(value)));
    }
}
