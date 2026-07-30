package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.CASE_CASE_MARKER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.CASE_INFORMANT;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.CASE_PAYMENT_REFERENCE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.CASE_RELATED_REFERENCE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.CASE_SUMMONS_CODE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.CASE_URN;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_1;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_2;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_3;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_4;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_5;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_POSTCODE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_ASN;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_CRO_NUMBER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_DOCUMENTATION_LANGUAGE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_HEARING_LANGUAGE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_PNC_IDENTIFIER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_COSTS;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_TYPE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_TYPE_INDIVIDUAL;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DEFENDANT_TYPE_ORGANISATION;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.DELIMITER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_ADDITIONAL_NATIONALITY;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_HOME_PHONE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_MOBILE_PHONE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_PRIMARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_SECONDARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_WORK_PHONE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CUSTODY_STATUS;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_DRIVER_NUMBER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_LANGUAGE_REQUIREMENT;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_FORENAME;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_FORENAME_2;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_FORENAME_3;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_TITLE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NATIONALITY;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NATIONAL_INSURANCE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_OCCUPATION;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_SPECIFIC_REQUIREMENTS;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_ARREST_DATE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_COMMITTED_END_DATE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_LOCATION;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_PROSECUTOR_COMPENSATION;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_SEQUENCE_NO;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_STATEMENT_OF_FACTS;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_STATEMENT_OF_FACTS_WELSH;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_WORDING;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.OFFENCE_WORDING_WELSH;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.ORGANISATION_COMPANY_TELEPHONE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.ORGANISATION_NAME;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_1;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_2;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_3;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_4;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_5;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_POSTCODE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_HOME_PHONE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_MOBILE_PHONE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_PRIMARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_SECONDARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_WORK_PHONE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_GENDER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_2;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_3;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_SURNAME;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_TITLE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_SELF_DEFINED_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ORGANISATION_COMPANY_TELEPHONE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ORGANISATION_NAME;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_TYPE;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvColumns.PROSECUTING_AUTHORITY;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Organisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianNameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianOrganisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SummonsProsecutionCase;

/**
 * Reconstructs a {@code summons-prosecution} JSON request from the flattened, pipe-delimited
 * CSV produced by {@code summons-prosecution-template.csv}.
 * <p>
 * The whole CSV file is treated as a single submission: {@code prosecutingAuthority} and
 * {@code hearingDetails.*} are read once, from the first data row. Rows are grouped into
 * {@link SummonsProsecutionCase}s by {@code case.urn}, and into {@link Defendant}s by
 * {@code defendant.prosecutorDefendantId} within each case; each row contributes exactly one
 * {@link Offence} to its defendant's offence list.
 */
public class SummonsProsecutionCsvToJsonConverter {

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(DELIMITER)
            .setHeader(SummonsProsecutionCsvColumns.HEADERS)
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    public String convertToJson(final Reader csvReader) throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(convertToObject(csvReader));
    }

    public Summons convertToObject(final Reader csvReader) throws IOException {
        try (CSVParser parser = CSV_FORMAT.parse(csvReader)) {
            final Map<String, CaseAccumulator> caseAccumulators = new LinkedHashMap<>();
            HearingDetails hearingDetails = null;
            String prosecutingAuthority = null;

            for (final CSVRecord record : parser) {
                if (hearingDetails == null) {
                    prosecutingAuthority = requireNonBlank(record, PROSECUTING_AUTHORITY);
                    hearingDetails = HearingDetails.hearingDetails()
                            .withCourtHearingLocation(requireNonBlank(record, HEARING_COURT_HEARING_LOCATION))
                            .withDateOfHearing(requireDate(record, HEARING_DATE_OF_HEARING))
                            .withTimeOfHearing(requireNonBlank(record, HEARING_TIME_OF_HEARING))
                            .build();
                }

                final String caseUrn = requireNonBlank(record, CASE_URN);
                final CaseAccumulator caseAccumulator = caseAccumulators.computeIfAbsent(caseUrn,
                        urn -> new CaseAccumulator(newCaseBuilder(record)));

                final String defendantId = requireNonBlank(record, DEFENDANT_PROSECUTOR_DEFENDANT_ID);
                final DefendantAccumulator defendantAccumulator = caseAccumulator.defendantAccumulators
                        .computeIfAbsent(defendantId, id -> newDefendantAccumulator(record));

                defendantAccumulator.offences.add(buildOffence(record));
            }

            if (hearingDetails == null) {
                throw new IllegalArgumentException("CSV file contains no data rows");
            }

            final List<SummonsProsecutionCase> prosecutionCases = new ArrayList<>();
            for (final CaseAccumulator caseAccumulator : caseAccumulators.values()) {
                final List<Defendant> defendants = new ArrayList<>();
                for (final DefendantAccumulator defendantAccumulator : caseAccumulator.defendantAccumulators.values()) {
                    defendants.add(Defendant.defendant()
                            .withDefendantDetails(defendantAccumulator.defendantDetails)
                            .withIndividual(defendantAccumulator.individual)
                            .withOrganisation(defendantAccumulator.organisation)
                            .withOffences(defendantAccumulator.offences)
                            .build());
                }
                prosecutionCases.add(caseAccumulator.caseBuilder.withDefendants(defendants).build());
            }

            return Summons.summons()
                    .withHearingDetails(hearingDetails)
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withProsecutionCases(prosecutionCases)
                    .build();
        }
    }

    private static SummonsProsecutionCase.Builder newCaseBuilder(final CSVRecord record) {
        return SummonsProsecutionCase.summonsProsecutionCase()
                .withUrn(requireNonBlank(record, CASE_URN))
                .withInformant(requireNonBlank(record, CASE_INFORMANT))
                .withSummonsCode(requireNonBlank(record, CASE_SUMMONS_CODE))
                .withCaseMarker(nullIfBlank(record.get(CASE_CASE_MARKER)))
                .withRelatedReferenceNumber(nullIfBlank(record.get(CASE_RELATED_REFERENCE_NUMBER)))
                .withPaymentReference(nullIfBlank(record.get(CASE_PAYMENT_REFERENCE)));
    }

    private static DefendantAccumulator newDefendantAccumulator(final CSVRecord record) {
        final DefendantAccumulator accumulator = new DefendantAccumulator();
        accumulator.defendantDetails = buildDefendantDetails(record);

        final String defendantType = requireNonBlank(record, DEFENDANT_TYPE);
        switch (defendantType) {
            case DEFENDANT_TYPE_INDIVIDUAL:
                accumulator.individual = buildIndividual(record);
                break;
            case DEFENDANT_TYPE_ORGANISATION:
                accumulator.organisation = buildOrganisation(record);
                break;
            default:
                throw new IllegalArgumentException("Column '" + DEFENDANT_TYPE + "' must be '"
                        + DEFENDANT_TYPE_INDIVIDUAL + "' or '" + DEFENDANT_TYPE_ORGANISATION
                        + "' but was '" + defendantType + "'");
        }
        return accumulator;
    }

    private static DefendantDetails buildDefendantDetails(final CSVRecord record) {
        return DefendantDetails.defendantDetails()
                .withProsecutorDefendantId(requireNonBlank(record, DEFENDANT_PROSECUTOR_DEFENDANT_ID))
                .withAsn(nullIfBlank(record.get(DEFENDANT_ASN)))
                .withPncIdentifier(nullIfBlank(record.get(DEFENDANT_PNC_IDENTIFIER)))
                .withCroNumber(nullIfBlank(record.get(DEFENDANT_CRO_NUMBER)))
                .withDocumentationLanguage(requireLanguage(record, DEFENDANT_DOCUMENTATION_LANGUAGE))
                .withHearingLanguage(requireLanguage(record, DEFENDANT_HEARING_LANGUAGE))
                .withProsecutorCosts(nullIfBlank(record.get(DEFENDANT_PROSECUTOR_COSTS)))
                .withAddress(buildAddress(record, DEFENDANT_ADDRESS_1, DEFENDANT_ADDRESS_2, DEFENDANT_ADDRESS_3,
                        DEFENDANT_ADDRESS_4, DEFENDANT_ADDRESS_5, DEFENDANT_ADDRESS_POSTCODE))
                .build();
    }

    private static Address buildAddress(final CSVRecord record, final String address1Column, final String address2Column,
                                         final String address3Column, final String address4Column,
                                         final String address5Column, final String postcodeColumn) {
        return Address.address()
                .withAddress1(requireNonBlank(record, address1Column))
                .withAddress2(nullIfBlank(record.get(address2Column)))
                .withAddress3(nullIfBlank(record.get(address3Column)))
                .withAddress4(nullIfBlank(record.get(address4Column)))
                .withAddress5(nullIfBlank(record.get(address5Column)))
                .withPostcode(nullIfBlank(record.get(postcodeColumn)))
                .build();
    }

    private static Individual buildIndividual(final CSVRecord record) {
        return Individual.individual()
                .withNameDetails(NameDetails.nameDetails()
                        .withTitle(nullIfBlank(record.get(INDIVIDUAL_NAME_TITLE)))
                        .withForename(requireNonBlank(record, INDIVIDUAL_NAME_FORENAME))
                        .withForename2(nullIfBlank(record.get(INDIVIDUAL_NAME_FORENAME_2)))
                        .withForename3(nullIfBlank(record.get(INDIVIDUAL_NAME_FORENAME_3)))
                        .withSurname(requireNonBlank(record, INDIVIDUAL_NAME_SURNAME))
                        .build())
                .withContactDetails(buildContactDetails(record, INDIVIDUAL_CONTACT_WORK_PHONE, INDIVIDUAL_CONTACT_HOME_PHONE,
                        INDIVIDUAL_CONTACT_MOBILE_PHONE, INDIVIDUAL_CONTACT_PRIMARY_EMAIL, INDIVIDUAL_CONTACT_SECONDARY_EMAIL))
                .withNationality(nullIfBlank(record.get(INDIVIDUAL_NATIONALITY)))
                .withAdditionalNationality(nullIfBlank(record.get(INDIVIDUAL_ADDITIONAL_NATIONALITY)))
                .withDateOfBirth(parseDate(record, INDIVIDUAL_DATE_OF_BIRTH))
                .withGender(requireGender(record, INDIVIDUAL_GENDER))
                .withOccupation(nullIfBlank(record.get(INDIVIDUAL_OCCUPATION)))
                .withObservedEthnicity(parseBigDecimal(record, INDIVIDUAL_OBSERVED_ETHNICITY))
                .withEthnicity(nullIfBlank(record.get(INDIVIDUAL_ETHNICITY)))
                .withDriverNumber(nullIfBlank(record.get(INDIVIDUAL_DRIVER_NUMBER)))
                .withLanguageRequirement(nullIfBlank(record.get(INDIVIDUAL_LANGUAGE_REQUIREMENT)))
                .withSpecificRequirements(nullIfBlank(record.get(INDIVIDUAL_SPECIFIC_REQUIREMENTS)))
                .withNationalInsuranceNumber(nullIfBlank(record.get(INDIVIDUAL_NATIONAL_INSURANCE_NUMBER)))
                .withCustodyStatus(nullIfBlank(record.get(INDIVIDUAL_CUSTODY_STATUS)))
                .withParentGuardian(buildParentGuardian(record))
                .build();
    }

    private static ParentGuardian buildParentGuardian(final CSVRecord record) {
        final String parentGuardianType = nullIfBlank(record.get(PARENT_GUARDIAN_TYPE));
        if (parentGuardianType == null) {
            return null;
        }

        final ParentGuardian.Builder builder = ParentGuardian.parentGuardian()
                .withAddress(buildAddress(record, PARENT_GUARDIAN_ADDRESS_1, PARENT_GUARDIAN_ADDRESS_2,
                        PARENT_GUARDIAN_ADDRESS_3, PARENT_GUARDIAN_ADDRESS_4, PARENT_GUARDIAN_ADDRESS_5,
                        PARENT_GUARDIAN_ADDRESS_POSTCODE));

        switch (parentGuardianType) {
            case DEFENDANT_TYPE_INDIVIDUAL:
                builder.withIndividual(ParentGuardianIndividual.parentGuardianIndividual()
                        .withNameDetails(ParentGuardianNameDetails.parentGuardianNameDetails()
                                .withTitle(nullIfBlank(record.get(PARENT_GUARDIAN_INDIVIDUAL_NAME_TITLE)))
                                .withForename(nullIfBlank(record.get(PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME)))
                                .withForename2(nullIfBlank(record.get(PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_2)))
                                .withForename3(nullIfBlank(record.get(PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_3)))
                                .withSurname(requireNonBlank(record, PARENT_GUARDIAN_INDIVIDUAL_NAME_SURNAME))
                                .build())
                        .withContactDetails(buildContactDetails(record, PARENT_GUARDIAN_INDIVIDUAL_CONTACT_WORK_PHONE,
                                PARENT_GUARDIAN_INDIVIDUAL_CONTACT_HOME_PHONE, PARENT_GUARDIAN_INDIVIDUAL_CONTACT_MOBILE_PHONE,
                                PARENT_GUARDIAN_INDIVIDUAL_CONTACT_PRIMARY_EMAIL, PARENT_GUARDIAN_INDIVIDUAL_CONTACT_SECONDARY_EMAIL))
                        .withDateOfBirth(parseDate(record, PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH))
                        .withGender(requireGender(record, PARENT_GUARDIAN_INDIVIDUAL_GENDER))
                        .withObservedEthnicity(parseBigDecimal(record, PARENT_GUARDIAN_INDIVIDUAL_OBSERVED_ETHNICITY))
                        .withSelfDefinedEthnicity(nullIfBlank(record.get(PARENT_GUARDIAN_INDIVIDUAL_SELF_DEFINED_ETHNICITY)))
                        .build());
                break;
            case DEFENDANT_TYPE_ORGANISATION:
                builder.withOrganisation(ParentGuardianOrganisation.parentGuardianOrganisation()
                        .withOrganisationName(requireNonBlank(record, PARENT_GUARDIAN_ORGANISATION_NAME))
                        .withCompanyTelephoneNumber(nullIfBlank(record.get(PARENT_GUARDIAN_ORGANISATION_COMPANY_TELEPHONE_NUMBER)))
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Column '" + PARENT_GUARDIAN_TYPE + "' must be '"
                        + DEFENDANT_TYPE_INDIVIDUAL + "' or '" + DEFENDANT_TYPE_ORGANISATION
                        + "' but was '" + parentGuardianType + "'");
        }
        return builder.build();
    }

    private static ContactDetails buildContactDetails(final CSVRecord record, final String workPhoneColumn,
                                                        final String homePhoneColumn, final String mobilePhoneColumn,
                                                        final String primaryEmailColumn, final String secondaryEmailColumn) {
        final String workPhone = nullIfBlank(record.get(workPhoneColumn));
        final String homePhone = nullIfBlank(record.get(homePhoneColumn));
        final String mobilePhone = nullIfBlank(record.get(mobilePhoneColumn));
        final String primaryEmail = nullIfBlank(record.get(primaryEmailColumn));
        final String secondaryEmail = nullIfBlank(record.get(secondaryEmailColumn));

        if (workPhone == null && homePhone == null && mobilePhone == null && primaryEmail == null && secondaryEmail == null) {
            return null;
        }

        return ContactDetails.contactDetails()
                .withWorkTelephoneNumber(workPhone)
                .withHomeTelephoneNumber(homePhone)
                .withMobileTelephoneNumber(mobilePhone)
                .withPrimaryEmail(primaryEmail)
                .withSecondaryEmail(secondaryEmail)
                .build();
    }

    private static Organisation buildOrganisation(final CSVRecord record) {
        return Organisation.organisation()
                .withOrganisationName(requireNonBlank(record, ORGANISATION_NAME))
                .withCompanyTelephoneNumber(nullIfBlank(record.get(ORGANISATION_COMPANY_TELEPHONE_NUMBER)))
                .build();
    }

    private static Offence buildOffence(final CSVRecord record) {
        return Offence.offence()
                .withOffenceDetails(OffenceDetails.offenceDetails()
                        .withCjsOffenceCode(requireNonBlank(record, OFFENCE_CJS_OFFENCE_CODE))
                        .withOffenceSequenceNo(requireInteger(record, OFFENCE_SEQUENCE_NO))
                        .withOffenceCommittedDate(parseDate(record, OFFENCE_COMMITTED_DATE))
                        .withLaidDate(requireDate(record, OFFENCE_LAID_DATE))
                        .withOffenceCommittedEndDate(parseDate(record, OFFENCE_COMMITTED_END_DATE))
                        .withOffenceLocation(nullIfBlank(record.get(OFFENCE_LOCATION)))
                        .withOffenceWording(requireNonBlank(record, OFFENCE_WORDING))
                        .withOffenceWordingWelsh(nullIfBlank(record.get(OFFENCE_WORDING_WELSH)))
                        .withProsecutorCompensation(nullIfBlank(record.get(OFFENCE_PROSECUTOR_COMPENSATION)))
                        .build())
                .withArrestDate(parseDate(record, OFFENCE_ARREST_DATE))
                .withStatementOfFacts(nullIfBlank(record.get(OFFENCE_STATEMENT_OF_FACTS)))
                .withStatementOfFactsWelsh(nullIfBlank(record.get(OFFENCE_STATEMENT_OF_FACTS_WELSH)))
                .build();
    }

    private static String nullIfBlank(final String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String requireNonBlank(final CSVRecord record, final String column) {
        final String value = nullIfBlank(record.get(column));
        if (value == null) {
            throw new IllegalArgumentException("Row " + record.getRecordNumber() + ": required column '"
                    + column + "' is blank");
        }
        return value;
    }

    private static LocalDate parseDate(final CSVRecord record, final String column) {
        final String value = nullIfBlank(record.get(column));
        return value == null ? null : LocalDate.parse(value);
    }

    private static LocalDate requireDate(final CSVRecord record, final String column) {
        return LocalDate.parse(requireNonBlank(record, column));
    }

    private static Integer requireInteger(final CSVRecord record, final String column) {
        return Integer.valueOf(requireNonBlank(record, column));
    }

    private static BigDecimal parseBigDecimal(final CSVRecord record, final String column) {
        final String value = nullIfBlank(record.get(column));
        return value == null ? null : new BigDecimal(value);
    }

    private static Gender requireGender(final CSVRecord record, final String column) {
        final String value = requireNonBlank(record, column);
        return Gender.valueFor(Integer.valueOf(value))
                .orElseThrow(() -> new IllegalArgumentException("Row " + record.getRecordNumber() + ": column '"
                        + column + "' has invalid gender value '" + value + "'"));
    }

    private static Language requireLanguage(final CSVRecord record, final String column) {
        final String value = requireNonBlank(record, column);
        return Language.valueFor(value)
                .orElseThrow(() -> new IllegalArgumentException("Row " + record.getRecordNumber() + ": column '"
                        + column + "' has invalid language value '" + value + "'"));
    }

    private static final class CaseAccumulator {
        private final SummonsProsecutionCase.Builder caseBuilder;
        private final Map<String, DefendantAccumulator> defendantAccumulators = new LinkedHashMap<>();

        private CaseAccumulator(final SummonsProsecutionCase.Builder caseBuilder) {
            this.caseBuilder = caseBuilder;
        }
    }

    private static final class DefendantAccumulator {
        private DefendantDetails defendantDetails;
        private Individual individual;
        private Organisation organisation;
        private final List<Offence> offences = new ArrayList<>();
    }
}
