package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.URN_PATTERN;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_CHARGE_PROSECUTION_REQUEST;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_INDIVIDUAL_DEFENDANT_REQUEST;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_ORGANISATION_DEFENDANT_REQUEST;

import java.util.List;
import java.util.stream.Stream;

import org.everit.json.schema.Schema;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Common schema validation tests shared by both charge-prosecution and summons-prosecution.
 * Both schemas are structurally identical — same properties, required fields and constraints.
 * Concrete subclasses supply only the schema under test and the valid request fixture.
 */
abstract class AbstractProsecutionSchemaValidationTest extends SchemaValidationTestUtils {

    private static final String PROSECUTION_CASE     = "prosecutionCases[0]";
    private static final String DEFENDANT      = PROSECUTION_CASE    + ".defendants[0]";
    private static final String INDIVIDUAL = DEFENDANT     + ".individual";
    private static final String NAME_DETAILS       = INDIVIDUAL + ".nameDetails";
    private static final String DEFENDANT_DETAILS    = DEFENDANT     + ".defendantDetails";
    private static final String OFFENCE  = DEFENDANT     + ".offences[0]";
    private static final String OFFENCE_DETAILS = OFFENCE + ".offenceDetails";

    protected Schema schema;

    abstract Schema loadSchema();

    @BeforeEach
    void initSchema() {
        schema = loadSchema();
    }

    private static JsonRequestBuilder base() {
        return JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST);
    }

    private static JsonRequestBuilder baseOrg() {
        return JsonRequestBuilder.from(VALID_ORGANISATION_DEFENDANT_REQUEST);
    }


    @Test
    @DisplayName("Valid individual defendant — conditional mandatory fields forename, surname and gender present and valid")
    void testValidIndividualDefendantRequest() {
        assertDoesNotThrow(() -> schema.validate(loadJson(VALID_INDIVIDUAL_DEFENDANT_REQUEST)));
    }

    @Test
    @DisplayName("Valid organisation defendant — conditional mandatory field organisationName present and valid")
    void testValidOrganisationDefendantRequest() {
        assertDoesNotThrow(() -> schema.validate(loadJson(VALID_ORGANISATION_DEFENDANT_REQUEST)));
    }


    @DisplayName("Prosecution Request Field — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("prosecutionRequestFieldScenarios")
    void testProsecutionRequestFieldViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }

    @DisplayName("Prosecution Request Field — URN Format Validation")
    @ParameterizedTest(name = "URN ''{0}'' — valid={1}")
    @MethodSource("urnFormatScenarios")
    void testUrnFormat(String urn, boolean expectedValid) {
        assertEquals(expectedValid, urn.matches(URN_PATTERN),
                "URN '" + urn + "' expected to be " + (expectedValid ? "valid" : "invalid"));
    }


    @DisplayName("Prosecution Case Level — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("prosecutionCaseLevelScenarios")
    void testProsecutionCaseLevelViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }


    @DisplayName("Hearing Details Level — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("hearingDetailsScenarios")
    void testHearingDetailsViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }


    @DisplayName("Defendant Structure — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("defendantStructureScenarios")
    void testDefendantStructureViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }


    @DisplayName("Individual Defendant — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("individualDefendantScenarios")
    void testIndividualDefendantViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }


    @DisplayName("Organisation Defendant — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("organisationDefendantScenarios")
    void testOrganisationDefendantViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }


    @DisplayName("Defendant Details — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("defendantDetailsScenarios")
    void testDefendantDetailsViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }


    @DisplayName("Offence Level — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("offenceLevelScenarios")
    void testOffenceLevelViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }


    @DisplayName("Offence Details Level — Negative Scenarios")
    @ParameterizedTest(name = "{0}")
    @MethodSource("offenceDetailsScenarios")
    void testOffenceDetailsViolations(String description, JSONObject request, List<String> expectedFragments) {
        assertViolations(schema, description, request, expectedFragments);
    }

    static Stream<Arguments> prosecutionRequestFieldScenarios() {
        return Stream.of(
                Arguments.of(
                        "missing top-level mandatory field: prosecutingAuthority absent",
                        base().remove("prosecutingAuthority").build(),
                        List.of("prosecutingAuthority")),
                Arguments.of(
                        "missing top-level mandatory field: hearingDetails absent",
                        base().remove("hearingDetails").build(),
                        List.of("hearingDetails")),
                Arguments.of(
                        "prosecutingAuthority too short: 'GAEAA0' has 6 characters (must be exactly 7)",
                        base().set("GAEAA0", "prosecutingAuthority").build(),
                        List.of("prosecutingAuthority", "minLength")),
                Arguments.of(
                        "prosecutingAuthority too long: 'GAEAA012' has 8 characters (must be exactly 7)",
                        base().set("GAEAA012", "prosecutingAuthority").build(),
                        List.of("prosecutingAuthority", "maxLength")),
                Arguments.of(
                        "prosecutingAuthority invalid characters: 'GAEAA!1' contains '!' — only [a-zA-Z0-9-] allowed",
                        base().set("GAEAA!1", "prosecutingAuthority").build(),
                        List.of("prosecutingAuthority", "does not match pattern"))
        );
    }

    static Stream<Arguments> urnFormatScenarios() {
        return Stream.of(
                Arguments.of("SCIV12345",   true),
                Arguments.of("SCIV-12345",  true),
                Arguments.of("abc-DEF-789", true),
                Arguments.of("A",           true),
                Arguments.of("123",         true),
                Arguments.of("SCIV 12345",  false),   // space
                Arguments.of("SCIV_12345",  false),   // underscore
                Arguments.of("SCIV!12345",  false),   // exclamation mark
                Arguments.of("SCIV@12345",  false),   // at sign
                Arguments.of("SCIV.12345",  false),   // full stop
                Arguments.of("SCIV/12345",  false),   // forward slash
                Arguments.of("SCIV#12345",  false),   // hash
                Arguments.of("SCIV 123!45", false)    // space and exclamation mark
        );
    }

    static Stream<Arguments> prosecutionCaseLevelScenarios() {
        return Stream.of(
                Arguments.of(
                        "prosecutionCases is empty array — minItems is 1",
                        base().set(new JSONArray(), "prosecutionCases").build(),
                        List.of("prosecutionCases", "minimum")),
                Arguments.of(
                        "missing mandatory field: prosecution case urn absent",
                        base().remove(PROSECUTION_CASE + ".urn").build(),
                        List.of("urn")),
                Arguments.of(
                        "missing mandatory field: prosecution case informant absent",
                        base().remove(PROSECUTION_CASE + ".informant").build(),
                        List.of("informant")),
                Arguments.of(
                        "missing mandatory field: prosecution case defendants absent",
                        base().remove(PROSECUTION_CASE + ".defendants").build(),
                        List.of("defendants")),
                Arguments.of(
                        "defendants is empty array — minItems is 1",
                        base().set(new JSONArray(), PROSECUTION_CASE + ".defendants").build(),
                        List.of("defendants", "minimum")),
                Arguments.of(
                        "urn too long: 37 characters exceeds maxLength of 36",
                        base().set("SCIV12345678901234567890123456789012X", PROSECUTION_CASE + ".urn").build(),
                        List.of("urn", "maxLength")),
                Arguments.of(
                        "informant too long: 95 characters exceeds maxLength of 92",
                        base().set("John Edward Alexander Thompson Robertson Fitzgerald MacAllister Williams Patterson Smith Junior", PROSECUTION_CASE + ".informant").build(),
                        List.of("informant", "maxLength")),
                Arguments.of(
                        "caseMarker too long: 'ABCDEFGHIJK' has 11 characters — maxLength is 10",
                        base().set("ABCDEFGHIJK", PROSECUTION_CASE + ".caseMarker").build(),
                        List.of("caseMarker", "maxLength")),
                Arguments.of(
                        "paymentReference too long: 37 characters exceeds maxLength of 36",
                        base().set("PAY12345678901234567890123456789012XX", PROSECUTION_CASE + ".paymentReference").build(),
                        List.of("paymentReference", "maxLength")),
                Arguments.of(
                        "summonsCode too long: 'AB' has 2 characters — maxLength is 1",
                        base().set("AB", PROSECUTION_CASE + ".summonsCode").build(),
                        List.of("summonsCode", "maxLength"))
        );
    }

    static Stream<Arguments> hearingDetailsScenarios() {
        return Stream.of(
                Arguments.of(
                        "missing mandatory field: hearingDetails.timeOfHearing absent",
                        base().remove("hearingDetails.timeOfHearing").build(),
                        List.of("timeOfHearing")),
                Arguments.of(
                        "timeOfHearing invalid format: '10-30-00' uses dashes — pattern requires colons (HH:MM:SS)",
                        base().set("10-30-00", "hearingDetails.timeOfHearing").build(),
                        List.of("timeOfHearing", "does not match pattern")),
                Arguments.of(
                        "missing mandatory field: hearingDetails.courtHearingLocation absent",
                        base().remove("hearingDetails.courtHearingLocation").build(),
                        List.of("courtHearingLocation")),
                Arguments.of(
                        "courtHearingLocation too short: '' is empty — minLength is 1",
                        base().set("", "hearingDetails.courtHearingLocation").build(),
                        List.of("courtHearingLocation", "minLength")),
                Arguments.of(
                        "courtHearingLocation too long: 'B01LY012' has 8 characters — maxLength is 7",
                        base().set("B01LY012", "hearingDetails.courtHearingLocation").build(),
                        List.of("courtHearingLocation", "maxLength")),
                Arguments.of(
                        "missing mandatory field: hearingDetails.dateOfHearing absent",
                        base().remove("hearingDetails.dateOfHearing").build(),
                        List.of("dateOfHearing")),
                Arguments.of(
                        "dateOfHearing wrong separator: '2011/11/17' uses slashes — ISO-8601 requires dashes (YYYY-MM-DD)",
                        base().set("2011/11/17", "hearingDetails.dateOfHearing").build(),
                        List.of("dateOfHearing", "does not match pattern")),
                Arguments.of(
                        "dateOfHearing invalid month: '2011-13-17' — month 13 does not exist",
                        base().set("2011-13-17", "hearingDetails.dateOfHearing").build(),
                        List.of("dateOfHearing", "does not match pattern")),
                Arguments.of(
                        "dateOfHearing invalid day: '2011-11-31' — November has 30 days",
                        base().set("2011-11-31", "hearingDetails.dateOfHearing").build(),
                        List.of("dateOfHearing", "does not match pattern"))
        );
    }

    static Stream<Arguments> defendantStructureScenarios() {
        return Stream.of(
                Arguments.of(
                        "defendant oneOf: neither individual nor organisation present — both options fail",
                        base().remove(DEFENDANT + ".individual").build(),
                        List.of("individual")),
                Arguments.of(
                        "defendant oneOf: both individual and organisation present — only one allowed",
                        base().set(new JSONObject("{\"organisationName\":\"Test Organisation Ltd\"}"), DEFENDANT + ".organisation").build(),
                        List.of("defendants")),
                Arguments.of(
                        "defendant oneOf: defendantDetails absent — required by both options",
                        base().remove(DEFENDANT + ".defendantDetails").build(),
                        List.of("defendantDetails")),
                Arguments.of(
                        "defendant offences: empty array — minItems is 1",
                        base().set(new JSONArray(), DEFENDANT + ".offences").build(),
                        List.of("offences", "minimum"))
        );
    }

    static Stream<Arguments> individualDefendantScenarios() {
        return Stream.of(
                Arguments.of(
                        "individual: nameDetails absent — required field",
                        base().remove(INDIVIDUAL + ".nameDetails").build(),
                        List.of("nameDetails")),
                Arguments.of(
                        "individual: gender absent — required field",
                        base().remove(INDIVIDUAL + ".gender").build(),
                        List.of("gender")),
                Arguments.of(
                        "individual: gender '5' is not a valid enum value — allowed: 0 (Not Known), 1 (Male), 2 (Female), 9 (Not Applicable)",
                        base().set(5, INDIVIDUAL + ".gender").build(),
                        List.of("gender", "is not a valid enum value")),
                Arguments.of(
                        "nameDetails: forename absent — required field",
                        base().remove(NAME_DETAILS + ".forename").build(),
                        List.of("forename")),
                Arguments.of(
                        "nameDetails: surname absent — required field",
                        base().remove(NAME_DETAILS + ".surname").build(),
                        List.of("surname")),
                Arguments.of(
                        "nameDetails: forename 'John  Lee' contains consecutive spaces — pattern prohibits adjacent spaces",
                        base().set("John  Lee", NAME_DETAILS + ".forename").build(),
                        List.of("forename", "does not match pattern")),
                Arguments.of(
                        "nameDetails: surname 'Van  Doe' contains consecutive spaces — pattern prohibits adjacent spaces",
                        base().set("Van  Doe", NAME_DETAILS + ".surname").build(),
                        List.of("surname", "does not match pattern")),
                Arguments.of(
                        "nameDetails: forename 37 characters exceeds maxLength of 35",
                        base().set("Alexandros Sebastian Montgomery-Jones", NAME_DETAILS + ".forename").build(),
                        List.of("forename", "maxLength")),
                Arguments.of(
                        "nameDetails: surname 37 characters exceeds maxLength of 35",
                        base().set("Alexandros Sebastian Montgomery-Jones", NAME_DETAILS + ".surname").build(),
                        List.of("surname", "maxLength")),
                Arguments.of(
                        "individual: dateOfBirth '2011/11/07' uses slashes — ISO-8601 requires dashes (YYYY-MM-DD)",
                        base().set("2011/11/07", INDIVIDUAL + ".dateOfBirth").build(),
                        List.of("dateOfBirth", "does not match pattern"))
        );
    }

    static Stream<Arguments> organisationDefendantScenarios() {
        return Stream.of(
                Arguments.of(
                        "organisation: organisationName absent — required field",
                        baseOrg().remove(DEFENDANT + ".organisation.organisationName").build(),
                        List.of("organisationName")),
                Arguments.of(
                        "organisation: organisationName 259 characters exceeds maxLength of 255",
                        baseOrg().set("International Law Enforcement Agency for Civil Prosecution Services United Kingdom Division Regional Office for South East England Metropolitan Area Compliance and Legal Services Department Regulatory Affairs Unit Alpha Beta Gamma Delta Epsilon Zeta Eta Theta",
                                DEFENDANT + ".organisation.organisationName").build(),
                        List.of("organisationName", "maxLength"))
        );
    }

    static Stream<Arguments> defendantDetailsScenarios() {
        return Stream.of(
                Arguments.of(
                        "defendantDetails: prosecutorDefendantId absent — required field",
                        base().remove(DEFENDANT_DETAILS + ".prosecutorDefendantId").build(),
                        List.of("prosecutorDefendantId")),
                Arguments.of(
                        "defendantDetails: documentationLanguage absent — required field",
                        base().remove(DEFENDANT_DETAILS + ".documentationLanguage").build(),
                        List.of("documentationLanguage")),
                Arguments.of(
                        "defendantDetails: hearingLanguage absent — required field",
                        base().remove(DEFENDANT_DETAILS + ".hearingLanguage").build(),
                        List.of("hearingLanguage")),
                Arguments.of(
                        "defendantDetails: address absent — required field",
                        base().remove(DEFENDANT_DETAILS + ".address").build(),
                        List.of("address")),
                Arguments.of(
                        "defendantDetails: prosecutorCosts '1000' missing decimal places — money pattern requires '1000.00'",
                        base().set("1000", DEFENDANT_DETAILS + ".prosecutorCosts").build(),
                        List.of("prosecutorCosts", "does not match pattern"))
        );
    }

    static Stream<Arguments> offenceLevelScenarios() {
        return Stream.of(
                Arguments.of(
                        "offence: offenceDetails absent — required field",
                        base().remove(OFFENCE + ".offenceDetails").build(),
                        List.of("offenceDetails")),
                Arguments.of(
                        "offence: arrestDate '2011/11/02' uses slashes — ISO-8601 requires dashes (YYYY-MM-DD)",
                        base().set("2011/11/02", OFFENCE + ".arrestDate").build(),
                        List.of("arrestDate", "does not match pattern")),
                Arguments.of(
                        "offence: statementOfFacts 4001 characters exceeds maxLength of 4000",
                        base().set("A".repeat(4001), OFFENCE + ".statementOfFacts").build(),
                        List.of("statementOfFacts", "maxLength"))
        );
    }

    static Stream<Arguments> offenceDetailsScenarios() {
        return Stream.of(
                Arguments.of(
                        "offenceDetails: cjsOffenceCode absent — required field",
                        base().remove(OFFENCE_DETAILS + ".cjsOffenceCode").build(),
                        List.of("cjsOffenceCode")),
                Arguments.of(
                        "offenceDetails: offenceSequenceNo absent — required field",
                        base().remove(OFFENCE_DETAILS + ".offenceSequenceNo").build(),
                        List.of("offenceSequenceNo")),
                Arguments.of(
                        "offenceDetails: offenceWording absent — required field",
                        base().remove(OFFENCE_DETAILS + ".offenceWording").build(),
                        List.of("offenceWording")),
                Arguments.of(
                        "offenceDetails: cjsOffenceCode 'MS155030X' has 9 characters — maxLength is 8",
                        base().set("MS155030X", OFFENCE_DETAILS + ".cjsOffenceCode").build(),
                        List.of("cjsOffenceCode", "maxLength")),
                Arguments.of(
                        "offenceDetails: offenceSequenceNo 0 is below minimum of 1",
                        base().set(0, OFFENCE_DETAILS + ".offenceSequenceNo").build(),
                        List.of("offenceSequenceNo", "is not greater or equal to")),
                Arguments.of(
                        "offenceDetails: offenceWording 2501 characters exceeds maxLength of 2500",
                        base().set("A".repeat(2501), OFFENCE_DETAILS + ".offenceWording").build(),
                        List.of("offenceWording", "maxLength")),
                Arguments.of(
                        "offenceDetails: offenceLocation 81 characters exceeds maxLength of 80",
                        base().set("A".repeat(81), OFFENCE_DETAILS + ".offenceLocation").build(),
                        List.of("offenceLocation", "maxLength")),
                Arguments.of(
                        "offenceDetails: laidDate absent — required field",
                        base().remove(OFFENCE_DETAILS + ".laidDate").build(),
                        List.of("laidDate")),
                Arguments.of(
                        "offenceDetails: laidDate '2011/11/02' uses slashes — ISO-8601 requires dashes (YYYY-MM-DD)",
                        base().set("2011/11/02", OFFENCE_DETAILS + ".laidDate").build(),
                        List.of("laidDate", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: laidDate '2011-13-02' — month 13 does not exist",
                        base().set("2011-13-02", OFFENCE_DETAILS + ".laidDate").build(),
                        List.of("laidDate", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: laidDate '2011-11-31' — November has 30 days",
                        base().set("2011-11-31", OFFENCE_DETAILS + ".laidDate").build(),
                        List.of("laidDate", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: offenceDateCode 7 is not a valid enum value — allowed: 1 (on/in), 2 (before), 3 (after), 4 (between), 5 (on/about), 6 (on/before)",
                        base().set(7, OFFENCE_DETAILS + ".offenceDateCode").build(),
                        List.of("offenceDateCode", "is not a valid enum value")),
                Arguments.of(
                        "offenceDetails: backDuty '500' missing decimal places — money pattern requires '500.00'",
                        base().set("500", OFFENCE_DETAILS + ".backDuty").build(),
                        List.of("backDuty", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: prosecutorCompensation '1500' missing decimal places (expected '1500.00')",
                        base().set("1500", OFFENCE_DETAILS + ".prosecutorCompensation").build(),
                        List.of("prosecutorCompensation", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: offenceCommittedDate '2011/11/02' uses slashes — ISO-8601 requires dashes",
                        base().set("2011/11/02", OFFENCE_DETAILS + ".offenceCommittedDate").build(),
                        List.of("offenceCommittedDate", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: offenceCommittedEndDate '2011/11/02' uses slashes — ISO-8601 requires dashes",
                        base().set("2011/11/02", OFFENCE_DETAILS + ".offenceCommittedEndDate").build(),
                        List.of("offenceCommittedEndDate", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: backDutyDateFrom '2011/11/02' uses slashes — ISO-8601 requires dashes",
                        base().set("2011/11/02", OFFENCE_DETAILS + ".backDutyDateFrom").build(),
                        List.of("backDutyDateFrom", "does not match pattern")),
                Arguments.of(
                        "offenceDetails: backDutyDateTo '2011/11/02' uses slashes — ISO-8601 requires dashes",
                        base().set("2011/11/02", OFFENCE_DETAILS + ".backDutyDateTo").build(),
                        List.of("backDutyDateTo", "does not match pattern"))
        );
    }
}
