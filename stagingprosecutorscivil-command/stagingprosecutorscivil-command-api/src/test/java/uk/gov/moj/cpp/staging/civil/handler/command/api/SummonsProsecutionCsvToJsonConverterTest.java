package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.DUPLICATE_CASE_URN;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.INCONSISTENT_VALUE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.INVALID_DATE_FORMAT;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.INVALID_ROW;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.INVALID_VALUE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError.REQUIRED_FIELD_MISSING;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileConversionResult;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError;
import uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvToJsonConverter;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SummonsProsecutionCase;

class SummonsProsecutionCsvToJsonConverterTest {

    private static final String TEMPLATE_CSV = "summons-prosecution-template.csv";
    private static final String FULLY_POPULATED_CSV = "summons-prosecution-fully-populated.csv";

    private final SummonsProsecutionCsvToJsonConverter converter = new SummonsProsecutionCsvToJsonConverter();

    @Test
    void convertsTemplateCsvIntoWireFormatMatchingTheCommandApiRequestShape() throws IOException {
        final JSONObject json = new JSONObject(new JSONTokener(convertTemplateToJson()));

        // dates and times are plain ISO strings, not Jackson's default array representation
        assertEquals("2022-02-04", json.getJSONObject("hearingDetails").getString("dateOfHearing"));
        assertEquals("09:05:00", json.getJSONObject("hearingDetails").getString("timeOfHearing"));

        final JSONObject firstCase = json.getJSONArray("prosecutionCases").getJSONObject(0);
        final JSONObject individualDefendant = firstCase.getJSONArray("defendants").getJSONObject(0);
        // gender is serialised as the schema's integer type, not the Java enum name
        assertEquals(2, individualDefendant.getJSONObject("individual").getInt("gender"));
        assertEquals("1985-03-15", individualDefendant.getJSONObject("individual").getString("dateOfBirth"));
        // absent optional fields (e.g. case.paymentReference, defendant.asn) are omitted, not written as null
        assertEquals(false, firstCase.has("paymentReference"));
        assertEquals(false, individualDefendant.getJSONObject("defendantDetails").has("asn"));
    }

    @Test
    void convertsEachRowIntoADistinctCaseWithASingleDefendant() throws IOException {
        final Summons summonsProsecution = convertTemplateToObject();

        assertEquals("GAAAA01", summonsProsecution.getProsecutingAuthority());
        assertEquals("B01LY01", summonsProsecution.getHearingDetails().getCourtHearingLocation());
        assertEquals(3, summonsProsecution.getProsecutionCases().size());

        final var firstCase = summonsProsecution.getProsecutionCases().get(0);
        assertEquals("SCIV67890", firstCase.getUrn());
        assertEquals(1, firstCase.getDefendants().size());

        final var individualDefendant = firstCase.getDefendants().get(0);
        assertNotNull(individualDefendant.getIndividual());
        assertNull(individualDefendant.getOrganisation());
        assertEquals("Jane", individualDefendant.getIndividual().getNameDetails().getForename());
        assertEquals(1, individualDefendant.getOffences().size());
        assertEquals(1, individualDefendant.getOffences().get(0).getOffenceDetails().getOffenceSequenceNo());

        final var secondCase = summonsProsecution.getProsecutionCases().get(1);
        assertEquals("SCIV67891", secondCase.getUrn());
        assertEquals(1, secondCase.getDefendants().size());
        final var organisationDefendant = secondCase.getDefendants().get(0);
        assertNotNull(organisationDefendant.getOrganisation());
        assertNull(organisationDefendant.getIndividual());
        assertEquals("Acme Retail Ltd", organisationDefendant.getOrganisation().getOrganisationName());

        final var thirdCase = summonsProsecution.getProsecutionCases().get(2);
        assertEquals("SCIV99999", thirdCase.getUrn());
        final var minorDefendant = thirdCase.getDefendants().get(0);
        assertNotNull(minorDefendant.getIndividual().getParentGuardian());
        assertEquals("Jones", minorDefendant.getIndividual().getParentGuardian().getIndividual().getNameDetails().getSurname());
    }

    @Test
    void throwsWhenCaseUrnIsDuplicated() {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n"
                + buildCsvRow(Map.of()) + "\n"
                + buildCsvRow(Map.of(SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID, "DEF00002")) + "\n";

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertTrue(exception.getMessage().contains("duplicate case URN"));
        assertTrue(exception.getMessage().contains(SummonsProsecutionCsvColumns.CASE_URN));
    }

    @Test
    void convertsFullyPopulatedCsvWithEveryFieldSet() throws IOException {
        final Summons summonsProsecution = convertToObject(FULLY_POPULATED_CSV);

        assertEquals("GAAAA01", summonsProsecution.getProsecutingAuthority());
        assertEquals("B01LY01", summonsProsecution.getHearingDetails().getCourtHearingLocation());
        assertEquals(LocalDate.parse("2022-03-10"), summonsProsecution.getHearingDetails().getDateOfHearing());
        assertEquals("10:30:00", summonsProsecution.getHearingDetails().getTimeOfHearing());
        assertEquals(3, summonsProsecution.getProsecutionCases().size());

        final SummonsProsecutionCase prosecutionCase = summonsProsecution.getProsecutionCases().get(0);
        assertEquals("SCIV11111", prosecutionCase.getUrn());
        assertEquals("Alice Johnson", prosecutionCase.getInformant());
        assertEquals("A", prosecutionCase.getSummonsCode());
        assertEquals("ST", prosecutionCase.getCaseMarker());
        assertEquals("REF-2022-001", prosecutionCase.getRelatedReferenceNumber());
        assertEquals("PAY-2022-001", prosecutionCase.getPaymentReference());
        assertEquals(1, prosecutionCase.getDefendants().size());

        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", defendant.getDefendantDetails().getProsecutorDefendantId());
        assertEquals("ASN123456789", defendant.getDefendantDetails().getAsn());
        assertEquals("2022/1234567A", defendant.getDefendantDetails().getPncIdentifier());
        assertEquals("123456/22A", defendant.getDefendantDetails().getCroNumber());
        assertEquals(Language.E, defendant.getDefendantDetails().getDocumentationLanguage());
        assertEquals(Language.W, defendant.getDefendantDetails().getHearingLanguage());
        assertEquals("150.00", defendant.getDefendantDetails().getProsecutorCosts());
        assertEquals("10 Downing Street", defendant.getDefendantDetails().getAddress().getAddress1());
        assertEquals("Westminster", defendant.getDefendantDetails().getAddress().getAddress2());
        assertEquals("City of Westminster", defendant.getDefendantDetails().getAddress().getAddress3());
        assertEquals("Greater London", defendant.getDefendantDetails().getAddress().getAddress4());
        assertEquals("England", defendant.getDefendantDetails().getAddress().getAddress5());
        assertEquals("SW1A 2AA", defendant.getDefendantDetails().getAddress().getPostcode());
        assertNull(defendant.getOrganisation());

        assertNotNull(defendant.getIndividual());
        assertEquals("Mr", defendant.getIndividual().getNameDetails().getTitle());
        assertEquals("Robert", defendant.getIndividual().getNameDetails().getForename());
        assertEquals("James", defendant.getIndividual().getNameDetails().getForename2());
        assertEquals("Michael", defendant.getIndividual().getNameDetails().getForename3());
        assertEquals("Anderson", defendant.getIndividual().getNameDetails().getSurname());
        assertEquals("020 7946 0011", defendant.getIndividual().getContactDetails().getWorkTelephoneNumber());
        assertEquals("020 7946 0022", defendant.getIndividual().getContactDetails().getHomeTelephoneNumber());
        assertEquals("07700 900123", defendant.getIndividual().getContactDetails().getMobileTelephoneNumber());
        assertEquals("robert.anderson@example.com", defendant.getIndividual().getContactDetails().getPrimaryEmail());
        assertEquals("r.anderson.alt@example.com", defendant.getIndividual().getContactDetails().getSecondaryEmail());
        assertEquals("British", defendant.getIndividual().getNationality());
        assertEquals("Irish", defendant.getIndividual().getAdditionalNationality());
        assertEquals(LocalDate.parse("1978-11-23"), defendant.getIndividual().getDateOfBirth());
        assertEquals(Gender.NUMBER_1, defendant.getIndividual().getGender());
        assertEquals("Electrician", defendant.getIndividual().getOccupation());
        assertEquals(new BigDecimal("3"), defendant.getIndividual().getObservedEthnicity());
        assertEquals("White British", defendant.getIndividual().getEthnicity());
        assertEquals("ANDER123456AB9CD", defendant.getIndividual().getDriverNumber());
        assertEquals("Welsh interpreter required", defendant.getIndividual().getLanguageRequirement());
        assertEquals("Wheelchair access needed", defendant.getIndividual().getSpecificRequirements());
        assertEquals("AB123456C", defendant.getIndividual().getNationalInsuranceNumber());
        assertEquals("Bailed", defendant.getIndividual().getCustodyStatus());

        final ParentGuardian parentGuardian = defendant.getIndividual().getParentGuardian();
        assertNotNull(parentGuardian);
        assertNull(parentGuardian.getOrganisation());
        assertEquals("22 Victoria Road", parentGuardian.getAddress().getAddress1());
        assertEquals("Didsbury", parentGuardian.getAddress().getAddress2());
        assertEquals("Manchester", parentGuardian.getAddress().getAddress3());
        assertEquals("Greater Manchester", parentGuardian.getAddress().getAddress4());
        assertEquals("England", parentGuardian.getAddress().getAddress5());
        assertEquals("M20 5AB", parentGuardian.getAddress().getPostcode());

        assertNotNull(parentGuardian.getIndividual());
        assertEquals("Mrs", parentGuardian.getIndividual().getNameDetails().getTitle());
        assertEquals("Margaret", parentGuardian.getIndividual().getNameDetails().getForename());
        assertEquals("Elizabeth", parentGuardian.getIndividual().getNameDetails().getForename2());
        assertEquals("Rose", parentGuardian.getIndividual().getNameDetails().getForename3());
        assertEquals("Anderson", parentGuardian.getIndividual().getNameDetails().getSurname());
        assertEquals("0161 496 0011", parentGuardian.getIndividual().getContactDetails().getWorkTelephoneNumber());
        assertEquals("0161 496 0022", parentGuardian.getIndividual().getContactDetails().getHomeTelephoneNumber());
        assertEquals("07700 900456", parentGuardian.getIndividual().getContactDetails().getMobileTelephoneNumber());
        assertEquals("margaret.anderson@example.com", parentGuardian.getIndividual().getContactDetails().getPrimaryEmail());
        assertEquals("m.anderson.alt@example.com", parentGuardian.getIndividual().getContactDetails().getSecondaryEmail());
        assertEquals(LocalDate.parse("1955-04-02"), parentGuardian.getIndividual().getDateOfBirth());
        assertEquals(Gender.NUMBER_2, parentGuardian.getIndividual().getGender());
        assertEquals(new BigDecimal("3"), parentGuardian.getIndividual().getObservedEthnicity());
        assertEquals("White British", parentGuardian.getIndividual().getSelfDefinedEthnicity());

        assertEquals(1, defendant.getOffences().size());
        final Offence offence = defendant.getOffences().get(0);
        assertEquals("CA03010", offence.getOffenceDetails().getCjsOffenceCode());
        assertEquals(1, offence.getOffenceDetails().getOffenceSequenceNo());
        assertEquals(LocalDate.parse("2022-02-01"), offence.getOffenceDetails().getOffenceCommittedDate());
        assertEquals(LocalDate.parse("2022-02-15"), offence.getOffenceDetails().getLaidDate());
        assertEquals(LocalDate.parse("2022-02-05"), offence.getOffenceDetails().getOffenceCommittedEndDate());
        assertEquals("Manchester", offence.getOffenceDetails().getOffenceLocation());
        assertEquals("Failure to comply with a community protection notice, contrary to section 43 of the "
                + "Anti-social Behaviour, Crime and Policing Act 2014", offence.getOffenceDetails().getOffenceWording());
        assertEquals("Methiant i gydymffurfio a hysbysiad amddiffyn cymunedol",
                offence.getOffenceDetails().getOffenceWordingWelsh());
        assertEquals("250.00", offence.getOffenceDetails().getProsecutorCompensation());
        assertEquals(LocalDate.parse("2022-01-28"), offence.getArrestDate());
        assertEquals("The defendant was observed on multiple occasions failing to comply with the terms of the "
                + "community protection notice served on 2022-01-10.", offence.getStatementOfFacts());
        assertEquals("Gwelwyd y diffynnydd ar sawl achlysur yn methu a chydymffurfio a thelerau'r hysbysiad",
                offence.getStatementOfFactsWelsh());

        final SummonsProsecutionCase secondCase = summonsProsecution.getProsecutionCases().get(1);
        assertEquals("SCIV22222", secondCase.getUrn());
        assertEquals("Carol White", secondCase.getInformant());
        assertEquals("C", secondCase.getSummonsCode());
        assertEquals("ST3", secondCase.getCaseMarker());
        assertEquals("REF-2022-003", secondCase.getRelatedReferenceNumber());
        assertEquals("PAY-2022-003", secondCase.getPaymentReference());
        assertEquals(1, secondCase.getDefendants().size());

        final Defendant minorDefendant = secondCase.getDefendants().get(0);
        assertEquals("223e4567-e89b-12d3-a456-426614174001", minorDefendant.getDefendantDetails().getProsecutorDefendantId());
        assertNull(minorDefendant.getOrganisation());
        assertNotNull(minorDefendant.getIndividual());
        assertEquals("David", minorDefendant.getIndividual().getNameDetails().getForename());
        assertEquals("Wilson", minorDefendant.getIndividual().getNameDetails().getSurname());
        assertEquals(LocalDate.parse("2012-08-14"), minorDefendant.getIndividual().getDateOfBirth());

        final ParentGuardian minorParentGuardian = minorDefendant.getIndividual().getParentGuardian();
        assertNotNull(minorParentGuardian);
        assertNull(minorParentGuardian.getIndividual());
        assertNotNull(minorParentGuardian.getOrganisation());
        assertEquals("Wilson Family Trust", minorParentGuardian.getOrganisation().getOrganisationName());
        assertEquals("0161 555 0099", minorParentGuardian.getOrganisation().getCompanyTelephoneNumber());
        assertEquals("5 Trust House Lane", minorParentGuardian.getAddress().getAddress1());

        assertEquals(1, minorDefendant.getOffences().size());
        final Offence minorOffence = minorDefendant.getOffences().get(0);
        assertEquals("CA03020", minorOffence.getOffenceDetails().getCjsOffenceCode());
        assertEquals(1, minorOffence.getOffenceDetails().getOffenceSequenceNo());
        assertEquals("Manchester", minorOffence.getOffenceDetails().getOffenceLocation());
        assertEquals("50.00", minorOffence.getOffenceDetails().getProsecutorCompensation());

        final SummonsProsecutionCase thirdCase = summonsProsecution.getProsecutionCases().get(2);
        assertEquals("SCIV33333", thirdCase.getUrn());
        assertEquals("Beta Enterprises Ltd", thirdCase.getInformant());
        assertEquals("B", thirdCase.getSummonsCode());
        assertEquals("ST2", thirdCase.getCaseMarker());
        assertEquals("REF-2022-002", thirdCase.getRelatedReferenceNumber());
        assertEquals("PAY-2022-002", thirdCase.getPaymentReference());
        assertEquals(1, thirdCase.getDefendants().size());

        final Defendant organisationDefendant = thirdCase.getDefendants().get(0);
        assertEquals("789e4567-e89b-12d3-a456-426614174111", organisationDefendant.getDefendantDetails().getProsecutorDefendantId());
        assertEquals(Language.W, organisationDefendant.getDefendantDetails().getDocumentationLanguage());
        assertEquals(Language.E, organisationDefendant.getDefendantDetails().getHearingLanguage());
        assertNull(organisationDefendant.getIndividual());
        assertNotNull(organisationDefendant.getOrganisation());
        assertEquals("Beta Enterprises Ltd", organisationDefendant.getOrganisation().getOrganisationName());
        assertEquals("0113 555 0100", organisationDefendant.getOrganisation().getCompanyTelephoneNumber());

        assertEquals(1, organisationDefendant.getOffences().size());
        final Offence organisationOffence = organisationDefendant.getOffences().get(0);
        assertEquals("CA03030", organisationOffence.getOffenceDetails().getCjsOffenceCode());
        assertEquals("Leeds", organisationOffence.getOffenceDetails().getOffenceLocation());
        assertEquals("500.00", organisationOffence.getOffenceDetails().getProsecutorCompensation());
    }

    @Test
    void throwsWhenCsvFileContainsNoDataRows() {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n";

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertEquals("CSV file contains no data rows", exception.getMessage());
    }

    @Test
    void acceptsExactlyTheConfiguredMaxRowsLimit() throws IOException {
        converter.maxRows = "3";
        final String csv = buildCsvWithRows(3);

        final Summons summonsProsecution = converter.convertToObject(new StringReader(csv));

        assertEquals(3, summonsProsecution.getProsecutionCases().size());
    }

    @Test
    void throwsWhenRowCountExceedsConfiguredMaxRowsLimit() {
        converter.maxRows = "2";
        final String csv = buildCsvWithRows(3);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertTrue(exception.getMessage().contains("exceeds the maximum allowed number of rows"));
        assertTrue(exception.getMessage().contains("2"));
    }

    @Test
    void defaultMaxRowsLimitIs1000() {
        assertEquals("1000", new SummonsProsecutionCsvToJsonConverter().maxRows);
    }

    @Test
    void throwsWhenDefendantTypeIsInvalid() {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.DEFENDANT_TYPE, "UNKNOWN"));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertTrue(exception.getMessage().contains(SummonsProsecutionCsvColumns.DEFENDANT_TYPE));
        assertTrue(exception.getMessage().contains("UNKNOWN"));
    }

    @Test
    void throwsWhenParentGuardianTypeIsInvalid() {
        final String csv = buildCsv(Map.of(
                SummonsProsecutionCsvColumns.PARENT_GUARDIAN_TYPE, "UNKNOWN",
                SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_1, "1 Guardian Street"));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertTrue(exception.getMessage().contains(SummonsProsecutionCsvColumns.PARENT_GUARDIAN_TYPE));
        assertTrue(exception.getMessage().contains("UNKNOWN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"04/02/2022", "2022/02/04", "04-02-2022", "2022-2-4", "22-02-04", "20220204",
            "2022-02-30", "2022-13-01", "+12022-01-01", "tomorrow"})
    void throwsWhenRequiredDateIsNotIsoFormat(final String invalidDate) {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE, invalidDate));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertEquals("Row 1: '" + SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE + "' has invalid date '"
                + invalidDate + "' - expected ISO date format YYYY-MM-DD", exception.getMessage());
    }

    @Test
    void throwsWhenOptionalDateIsNotIsoFormat() {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.OFFENCE_ARREST_DATE, "28/01/2022"));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertEquals("Row 1: '" + SummonsProsecutionCsvColumns.OFFENCE_ARREST_DATE + "' has invalid date "
                + "'28/01/2022' - expected ISO date format YYYY-MM-DD", exception.getMessage());
    }

    @Test
    void acceptsLeapDayInIsoFormat() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE, "2024-02-29"));

        final Summons summonsProsecution = converter.convertToObject(new StringReader(csv));

        assertEquals(LocalDate.of(2024, 2, 29), summonsProsecution.getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getOffenceDetails().getLaidDate());
    }

    @Test
    void throwsWhenRequiredColumnIsBlank() {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.CASE_URN, ""));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertTrue(exception.getMessage().contains(SummonsProsecutionCsvColumns.CASE_URN));
        assertTrue(exception.getMessage().contains("is blank"));
    }

    @Test
    void convertsCsvWithBlankInformantToCaseWithoutInformant() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.CASE_INFORMANT, ""));

        final Summons summonsProsecution = converter.convertToObject(new StringReader(csv));

        assertNull(summonsProsecution.getProsecutionCases().get(0).getInformant());
    }

    @Test
    void throwsWhenGenderValueIsInvalid() {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER, "5"));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertTrue(exception.getMessage().contains(SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER));
        assertTrue(exception.getMessage().contains("invalid gender value"));
    }

    @Test
    void throwsWhenLanguageValueIsInvalid() {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.DEFENDANT_DOCUMENTATION_LANGUAGE, "X"));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertTrue(exception.getMessage().contains(SummonsProsecutionCsvColumns.DEFENDANT_DOCUMENTATION_LANGUAGE));
        assertTrue(exception.getMessage().contains("invalid language value"));
    }

    @Test
    void contactDetailsIsNullWhenWorkHomeMobilePhoneAndBothEmailsAreAllBlank() throws IOException {
        final String csv = buildCsv(Map.of());

        final Defendant defendant = firstDefendantFrom(csv);

        assertNull(defendant.getIndividual().getContactDetails());
    }

    @Test
    void contactDetailsIsPresentWhenOnlyWorkPhoneIsSet() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_WORK_PHONE, "0111 111 1111"));

        final Defendant defendant = firstDefendantFrom(csv);

        assertEquals("0111 111 1111", defendant.getIndividual().getContactDetails().getWorkTelephoneNumber());
        assertNull(defendant.getIndividual().getContactDetails().getHomeTelephoneNumber());
    }

    @Test
    void contactDetailsIsPresentWhenOnlyHomePhoneIsSet() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_HOME_PHONE, "0222 222 2222"));

        final Defendant defendant = firstDefendantFrom(csv);

        assertEquals("0222 222 2222", defendant.getIndividual().getContactDetails().getHomeTelephoneNumber());
        assertNull(defendant.getIndividual().getContactDetails().getWorkTelephoneNumber());
    }

    @Test
    void contactDetailsIsPresentWhenOnlyMobilePhoneIsSet() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_MOBILE_PHONE, "07700 900000"));

        final Defendant defendant = firstDefendantFrom(csv);

        assertEquals("07700 900000", defendant.getIndividual().getContactDetails().getMobileTelephoneNumber());
        assertNull(defendant.getIndividual().getContactDetails().getWorkTelephoneNumber());
    }

    @Test
    void contactDetailsIsPresentWhenOnlyPrimaryEmailIsSet() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_PRIMARY_EMAIL, "primary@example.com"));

        final Defendant defendant = firstDefendantFrom(csv);

        assertEquals("primary@example.com", defendant.getIndividual().getContactDetails().getPrimaryEmail());
        assertNull(defendant.getIndividual().getContactDetails().getSecondaryEmail());
    }

    @Test
    void contactDetailsIsPresentWhenOnlySecondaryEmailIsSet() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_SECONDARY_EMAIL, "secondary@example.com"));

        final Defendant defendant = firstDefendantFrom(csv);

        assertEquals("secondary@example.com", defendant.getIndividual().getContactDetails().getSecondaryEmail());
        assertNull(defendant.getIndividual().getContactDetails().getPrimaryEmail());
    }

    // ---- convert(): collects every problem so they can be reported in the error report ----

    @Test
    void convertCollectsEveryProblemInARowInsteadOfStoppingAtTheFirst() throws IOException {
        final String csv = buildCsv(Map.of(
                SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, "",
                SummonsProsecutionCsvColumns.INDIVIDUAL_DATE_OF_BIRTH, "31/02/1990",
                SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER, "7"));

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors(), contains(
                error(1, "SCIV00001", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, ""),
                error(1, "SCIV00001", INVALID_DATE_FORMAT, SummonsProsecutionCsvColumns.INDIVIDUAL_DATE_OF_BIRTH, "31/02/1990"),
                error(1, "SCIV00001", INVALID_VALUE, SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER, "7")));
        assertTrue(result.getSummons().isEmpty());
        assertTrue(result.getConvertedRows().isEmpty());
        assertEquals("GAAAA01", result.getProsecutingAuthority());
    }

    @Test
    void convertKeepsValidRowsAndReportsProblemsAgainstTheFailingRowsCaseUrn() throws IOException {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n"
                + buildCsvRow(Map.of()) + "\n"
                + buildCsvRow(Map.of(
                        SummonsProsecutionCsvColumns.CASE_URN, "SCIV00002",
                        SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE, "")) + "\n"
                + buildCsvRow(Map.of(SummonsProsecutionCsvColumns.CASE_URN, "SCIV00003")) + "\n";

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors(), contains(
                error(2, "SCIV00002", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE, "")));
        assertTrue(result.getSummons().isPresent());
        assertThat(result.getSummons().get().getProsecutionCases().stream().map(SummonsProsecutionCase::getUrn).collect(toList()),
                contains("SCIV00001", "SCIV00003"));
        assertThat(result.getConvertedRows().stream().map(ComplaintsFileConversionResult.ConvertedRow::getRowNumber).collect(toList()),
                contains(1L, 3L));
        assertThat(result.getConvertedRows().stream().map(ComplaintsFileConversionResult.ConvertedRow::getCaseReference).collect(toList()),
                contains("SCIV00001", "SCIV00003"));
    }

    @Test
    void convertLeavesTheCaseReferenceNullWhenTheCaseUrnIsBlank() throws IOException {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.CASE_URN, ""));

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors(), contains(
                error(1, null, REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.CASE_URN, "")));
    }

    @Test
    void convertRecordsTheRowsProsecutorDefendantIdAsTheDefendantReference() throws IOException {
        final String csv = buildCsv(Map.of(
                SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID, "DEF00001",
                SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, ""));

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors().size(), is(1));
        assertThat(result.getErrors().get(0).getCaseReference(), is("SCIV00001"));
        assertThat(result.getErrors().get(0).getDefendantReference(), is("DEF00001"));
        assertTrue(result.getErrors().get(0).isDefendantError());
    }

    @Test
    void convertLeavesTheDefendantReferenceNullWhenTheProsecutorDefendantIdIsBlank() throws IOException {
        final String csv = buildCsv(Map.of(
                SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID, "",
                SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, ""));

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors().get(0).getDefendantReference(), is(nullValue()));
    }

    @Test
    void convertReportsADuplicateCaseUrnAgainstTheRepeatedRowOnly() throws IOException {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n"
                + buildCsvRow(Map.of()) + "\n"
                + buildCsvRow(Map.of(SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID, "DEF00002")) + "\n";

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors(), contains(
                error(2, "SCIV00001", DUPLICATE_CASE_URN, SummonsProsecutionCsvColumns.CASE_URN, "SCIV00001")));
        assertThat(result.getConvertedRows().size(), is(1));
    }

    @Test
    void convertReportsHearingDetailsProblemsAgainstTheFirstRowWithoutExcludingItsCase() throws IOException {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n"
                + buildCsvRow(Map.of(SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "04/02/2022")) + "\n"
                + buildCsvRow(Map.of(SummonsProsecutionCsvColumns.CASE_URN, "SCIV00002")) + "\n";

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        // row 2's valid date is not compared with row 1's malformed one - fixing row 1 is what's needed
        assertThat(result.getErrors(), contains(
                error(1, "SCIV00001", INVALID_DATE_FORMAT, SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "04/02/2022")));
        // both cases are still converted so they can be schema-validated in the same upload
        assertThat(result.getConvertedRows().stream().map(ComplaintsFileConversionResult.ConvertedRow::getRowNumber).collect(toList()),
                contains(1L, 2L));
        final Summons summons = result.getSummons().get();
        assertThat(summons.getProsecutionCases().size(), is(2));
        assertNull(summons.getHearingDetails().getDateOfHearing());
        assertEquals("B01LY01", summons.getHearingDetails().getCourtHearingLocation());
    }

    @Test
    void convertReportsHearingDetailsAndFirstRowCaseProblemsTogether() throws IOException {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n"
                + buildCsvRow(Map.of(
                        SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, "",
                        SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "04/02/2022",
                        SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, "")) + "\n"
                + buildCsvRow(Map.of(
                        SummonsProsecutionCsvColumns.CASE_URN, "SCIV00002",
                        SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE, "10/01/2022")) + "\n";

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors(), contains(
                error(1, "SCIV00001", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, ""),
                error(1, "SCIV00001", INVALID_DATE_FORMAT, SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "04/02/2022"),
                error(1, "SCIV00001", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, ""),
                error(2, "SCIV00002", INVALID_DATE_FORMAT, SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE, "10/01/2022")));
        assertTrue(result.getSummons().isEmpty());
    }

    @Test
    void convertReportsBlankHearingColumnsAgainstEveryRowWithoutExcludingTheCases() throws IOException {
        final ComplaintsFileConversionResult result = convert("complaints-files/submission-level-columns-blank-in-every-row.csv");

        assertThat(result.getErrors(), contains(
                error(1, "SCIV00001", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, ""),
                error(1, "SCIV00001", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, ""),
                error(2, "SCIV00002", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, ""),
                error(2, "SCIV00002", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, ""),
                error(3, "SCIV00003", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, ""),
                error(3, "SCIV00003", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, "")));
        assertThat(result.getConvertedRows().stream().map(ComplaintsFileConversionResult.ConvertedRow::getRowNumber).collect(toList()),
                contains(1L, 2L, 3L));
    }

    @Test
    void convertReportsSubmissionLevelValuesThatDifferFromRowOneAgainstTheirOwnRow() throws IOException {
        final ComplaintsFileConversionResult result = convert("complaints-files/submission-level-columns-inconsistent.csv");

        assertThat(result.getErrors(), contains(
                error(2, "SCIV00002", INCONSISTENT_VALUE, SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, "10:00:00"),
                error(3, "SCIV00003", INCONSISTENT_VALUE, SummonsProsecutionCsvColumns.PROSECUTING_AUTHORITY, "GBBBB02"),
                error(3, "SCIV00003", INCONSISTENT_VALUE, SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, "C01LY01"),
                error(3, "SCIV00003", INCONSISTENT_VALUE, SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "2022-02-05")));
        assertEquals("Row 2: column '" + SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING
                        + "' is '10:00:00' but row 1 has '09:05:00' - it must be the same in every row",
                result.getErrors().get(0).getMessage());
        // only row 1's values are sent forward
        final Summons summons = result.getSummons().get();
        assertThat(summons.getProsecutionCases().size(), is(3));
        assertEquals("GAAAA01", summons.getProsecutingAuthority());
        assertEquals("B01LY01", summons.getHearingDetails().getCourtHearingLocation());
        assertEquals(LocalDate.parse("2022-02-04"), summons.getHearingDetails().getDateOfHearing());
        assertEquals("09:05:00", summons.getHearingDetails().getTimeOfHearing());
    }

    @Test
    void convertReportsMalformedHearingValuesInALaterRowOnceEach() throws IOException {
        final ComplaintsFileConversionResult result = convert("complaints-files/submission-level-columns-invalid-in-later-row.csv");

        // malformed, so not also reported as inconsistent with row 1
        assertThat(result.getErrors(), contains(
                error(3, "SCIV00003", INVALID_VALUE, SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, "B01LY01X"),
                error(3, "SCIV00003", INVALID_DATE_FORMAT, SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "04/02/2022"),
                error(3, "SCIV00003", INVALID_VALUE, SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, "9am")));
        assertThat(result.getConvertedRows().size(), is(3));
    }

    @Test
    void convertReportsABlankProsecutingAuthorityOrHearingDateInALaterRow() throws IOException {
        final ComplaintsFileConversionResult result = convert("complaints-files/submission-level-columns-blank-in-later-row.csv");

        assertThat(result.getErrors(), contains(
                error(2, "SCIV00002", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.PROSECUTING_AUTHORITY, ""),
                error(2, "SCIV00002", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "")));
        assertEquals("GAAAA01", result.getProsecutingAuthority());
    }

    @Test
    void convertComparesHearingTimesByValueNotByLeadingZeros() throws IOException {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n"
                + buildCsvRow(Map.of(SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, "9:05:00")) + "\n"
                + buildCsvRow(Map.of(SummonsProsecutionCsvColumns.CASE_URN, "SCIV00002")) + "\n";

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertTrue(result.getErrors().isEmpty());
        assertEquals("9:05:00", result.getSummons().get().getHearingDetails().getTimeOfHearing());
    }

    @Test
    void convertDoesNotCompareLaterRowsWithABlankRowOneValue() throws IOException {
        final ComplaintsFileConversionResult result = convert("complaints-files/submission-level-columns-blank-in-first-row-only.csv");

        assertThat(result.getErrors(), contains(
                error(1, "SCIV00001", REQUIRED_FIELD_MISSING, SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, "")));
        assertNull(result.getSummons().get().getHearingDetails().getTimeOfHearing());
    }


    @Test
    void convertReportsInvalidNumbersAndEnumsWithoutThrowing() throws IOException {
        final String csv = buildCsv(Map.of(
                SummonsProsecutionCsvColumns.OFFENCE_SEQUENCE_NO, "one",
                SummonsProsecutionCsvColumns.INDIVIDUAL_OBSERVED_ETHNICITY, "abc",
                SummonsProsecutionCsvColumns.DEFENDANT_DOCUMENTATION_LANGUAGE, "X"));

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors(), containsInAnyOrder(
                error(1, "SCIV00001", INVALID_VALUE, SummonsProsecutionCsvColumns.DEFENDANT_DOCUMENTATION_LANGUAGE, "X"),
                error(1, "SCIV00001", INVALID_VALUE, SummonsProsecutionCsvColumns.INDIVIDUAL_OBSERVED_ETHNICITY, "abc"),
                error(1, "SCIV00001", INVALID_VALUE, SummonsProsecutionCsvColumns.OFFENCE_SEQUENCE_NO, "one")));
    }

    @Test
    void convertReportsAnInvalidDefendantTypeWithoutReadingTheIndividualOrOrganisationColumns() throws IOException {
        final String csv = buildCsv(Map.of(
                SummonsProsecutionCsvColumns.DEFENDANT_TYPE, "PERSON",
                SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, ""));

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors(), contains(
                error(1, "SCIV00001", INVALID_VALUE, SummonsProsecutionCsvColumns.DEFENDANT_TYPE, "PERSON")));
    }

    @Test
    void convertReportsALaterRowWithMissingColumnsAsAnInvalidRow() throws IOException {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\n"
                + buildCsvRow(Map.of()) + "\n"
                + "GAAAA01,B01LY01\n";

        final ComplaintsFileConversionResult result = converter.convert(new StringReader(csv));

        assertThat(result.getErrors().size(), is(1));
        assertThat(result.getErrors().get(0).getCode(), is(INVALID_ROW));
        assertThat(result.getErrors().get(0).getRowNumber(), is(2L));
        assertThat(result.getErrors().get(0).getCaseReference(), is(nullValue()));
        assertThat(result.getConvertedRows().size(), is(1));
    }

    @Test
    void convertThrowsWhenTheFirstRowDoesNotMatchTheTemplateColumns() {
        final String csv = String.join(",", SummonsProsecutionCsvColumns.HEADERS) + "\nGAAAA01,B01LY01\n";

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(new StringReader(csv)));

        assertTrue(exception.getMessage().contains("columns but the complaints file template has"));
    }

    @Test
    void convertThrowsWhenProsecutingAuthorityIsBlankBecauseTheFileCannotBeAttributedToAProsecutor() {
        final String csv = buildCsv(Map.of(SummonsProsecutionCsvColumns.PROSECUTING_AUTHORITY, ""));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(new StringReader(csv)));

        assertTrue(exception.getMessage().contains(SummonsProsecutionCsvColumns.PROSECUTING_AUTHORITY));
    }

    @Test
    void convertReturnsEveryRowWhenTheFileIsValid() throws IOException {
        final ComplaintsFileConversionResult result = converter.convert(resourceReader(FULLY_POPULATED_CSV));

        assertTrue(result.getErrors().isEmpty());
        assertThat(result.getSummons().get().getProsecutionCases().size(), is(3));
        assertThat(result.getConvertedRows().stream().map(ComplaintsFileConversionResult.ConvertedRow::getCaseReference).collect(toList()),
                contains("SCIV11111", "SCIV22222", "SCIV33333"));
    }

    @Test
    void convertToObjectStillFailsFastWithTheFirstProblemsMessage() {
        final String csv = buildCsv(Map.of(
                SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, "",
                SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER, "7"));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> converter.convertToObject(new StringReader(csv)));

        assertEquals("Row 1: required column '" + SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME + "' is blank",
                exception.getMessage());
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

    private ComplaintsFileConversionResult convert(final String resourcePath) throws IOException {
        try (Reader reader = resourceReader(resourcePath)) {
            return converter.convert(reader);
        }
    }

    private Defendant firstDefendantFrom(final String csv) throws IOException {
        return converter.convertToObject(new StringReader(csv))
                .getProsecutionCases().get(0).getDefendants().get(0);
    }

    private String convertTemplateToJson() throws IOException {
        return convertToJson(TEMPLATE_CSV);
    }

    private Summons convertTemplateToObject() throws IOException {
        return convertToObject(TEMPLATE_CSV);
    }

    private String convertToJson(final String resourcePath) throws IOException {
        try (Reader reader = resourceReader(resourcePath)) {
            return converter.convertToJson(reader);
        }
    }

    private Summons convertToObject(final String resourcePath) throws IOException {
        try (Reader reader = resourceReader(resourcePath)) {
            return converter.convertToObject(reader);
        }
    }

    private Reader resourceReader(final String resourcePath) {
        return new InputStreamReader(getClass().getResourceAsStream("/" + resourcePath), StandardCharsets.UTF_8);
    }

    /**
     * Builds a single-row CSV (header + one data row) that satisfies every {@code requireXxx}
     * check in the converter for an INDIVIDUAL defendant with no parent/guardian, then applies
     * the given overrides on top - letting each edge-case test blank out or corrupt exactly the
     * one column it wants to exercise.
     */
    private static String buildCsv(final Map<String, String> overrides) {
        final String header = String.join(",", SummonsProsecutionCsvColumns.HEADERS);
        return header + "\n" + buildCsvRow(overrides) + "\n";
    }

    private static String buildCsvWithRows(final int rowCount) {
        final StringBuilder csv = new StringBuilder(String.join(",", SummonsProsecutionCsvColumns.HEADERS)).append('\n');
        for (int i = 1; i <= rowCount; i++) {
            csv.append(buildCsvRow(Map.of(
                    SummonsProsecutionCsvColumns.CASE_URN, "SCIV" + i,
                    SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID, "DEF" + i)))
                    .append('\n');
        }
        return csv.toString();
    }

    private static String buildCsvRow(final Map<String, String> overrides) {
        final Map<String, String> values = new LinkedHashMap<>(defaultValidValues());
        values.putAll(overrides);

        final StringBuilder row = new StringBuilder();
        for (int i = 0; i < SummonsProsecutionCsvColumns.HEADERS.length; i++) {
            if (i > 0) {
                row.append(',');
            }
            row.append(values.getOrDefault(SummonsProsecutionCsvColumns.HEADERS[i], ""));
        }
        return row.toString();
    }

    private static Map<String, String> defaultValidValues() {
        final Map<String, String> values = new LinkedHashMap<>();
        values.put(SummonsProsecutionCsvColumns.PROSECUTING_AUTHORITY, "GAAAA01");
        values.put(SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION, "B01LY01");
        values.put(SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING, "2022-02-04");
        values.put(SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING, "09:05:00");
        values.put(SummonsProsecutionCsvColumns.CASE_URN, "SCIV00001");
        values.put(SummonsProsecutionCsvColumns.CASE_INFORMANT, "Jane Smith");
        values.put(SummonsProsecutionCsvColumns.CASE_SUMMONS_CODE, "A");
        values.put(SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID, "DEF00001");
        values.put(SummonsProsecutionCsvColumns.DEFENDANT_DOCUMENTATION_LANGUAGE, "E");
        values.put(SummonsProsecutionCsvColumns.DEFENDANT_HEARING_LANGUAGE, "E");
        values.put(SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_1, "1 High Street");
        values.put(SummonsProsecutionCsvColumns.DEFENDANT_TYPE, SummonsProsecutionCsvColumns.DEFENDANT_TYPE_INDIVIDUAL);
        values.put(SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_FORENAME, "Jane");
        values.put(SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME, "Smith");
        values.put(SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER, "2");
        values.put(SummonsProsecutionCsvColumns.OFFENCE_CJS_OFFENCE_CODE, "CA03010");
        values.put(SummonsProsecutionCsvColumns.OFFENCE_SEQUENCE_NO, "1");
        values.put(SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE, "2022-01-10");
        values.put(SummonsProsecutionCsvColumns.OFFENCE_WORDING, "Failure to comply");
        return values;
    }
}
