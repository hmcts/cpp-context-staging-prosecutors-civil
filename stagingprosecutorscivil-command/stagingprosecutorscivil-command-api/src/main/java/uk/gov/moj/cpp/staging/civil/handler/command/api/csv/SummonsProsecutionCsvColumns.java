package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

/**
 * Column names for the flattened summons-prosecution CSV (comma-delimited).
 * One row represents one (prosecutionCase, defendant, offence) combination; case- and
 * defendant-level columns repeat across rows that share the same {@link #CASE_URN} /
 * {@link #DEFENDANT_PROSECUTOR_DEFENDANT_ID}. Order here matches
 * {@code src/main/resources/csv/summons-prosecution-template.csv}.
 */
public final class SummonsProsecutionCsvColumns {

    public static final char DELIMITER = ',';

    // Submission-level (same value on every row of the file)
    public static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    public static final String HEARING_COURT_HEARING_LOCATION = "hearingDetails.courtHearingLocation";
    public static final String HEARING_DATE_OF_HEARING = "hearingDetails.dateOfHearing";
    public static final String HEARING_TIME_OF_HEARING = "hearingDetails.timeOfHearing";

    // Case-level (repeats across rows sharing the same CASE_URN)
    public static final String CASE_URN = "case.urn";
    public static final String CASE_INFORMANT = "case.informant";
    public static final String CASE_SUMMONS_CODE = "case.summonsCode";
    public static final String CASE_CASE_MARKER = "case.caseMarker";
    public static final String CASE_RELATED_REFERENCE_NUMBER = "case.relatedReferenceNumber";
    public static final String CASE_PAYMENT_REFERENCE = "case.paymentReference";

    // Defendant-level (repeats across rows sharing the same DEFENDANT_PROSECUTOR_DEFENDANT_ID within a case)
    public static final String DEFENDANT_PROSECUTOR_DEFENDANT_ID = "defendant.prosecutorDefendantId";
    public static final String DEFENDANT_ASN = "defendant.asn";
    public static final String DEFENDANT_PNC_IDENTIFIER = "defendant.pncIdentifier";
    public static final String DEFENDANT_CRO_NUMBER = "defendant.croNumber";
    public static final String DEFENDANT_DOCUMENTATION_LANGUAGE = "defendant.documentationLanguage";
    public static final String DEFENDANT_HEARING_LANGUAGE = "defendant.hearingLanguage";
    public static final String DEFENDANT_PROSECUTOR_COSTS = "defendant.prosecutorCosts";
    public static final String DEFENDANT_ADDRESS_1 = "defendant.address.address1";
    public static final String DEFENDANT_ADDRESS_2 = "defendant.address.address2";
    public static final String DEFENDANT_ADDRESS_3 = "defendant.address.address3";
    public static final String DEFENDANT_ADDRESS_4 = "defendant.address.address4";
    public static final String DEFENDANT_ADDRESS_5 = "defendant.address.address5";
    public static final String DEFENDANT_ADDRESS_POSTCODE = "defendant.address.postcode";

    /** Discriminator: {@code INDIVIDUAL} or {@code ORGANISATION}. Selects which column group below applies. */
    public static final String DEFENDANT_TYPE = "defendant.type";
    public static final String DEFENDANT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    public static final String DEFENDANT_TYPE_ORGANISATION = "ORGANISATION";

    // Defendant -> individual
    public static final String INDIVIDUAL_NAME_TITLE = "defendant.individual.nameDetails.title";
    public static final String INDIVIDUAL_NAME_FORENAME = "defendant.individual.nameDetails.forename";
    public static final String INDIVIDUAL_NAME_FORENAME_2 = "defendant.individual.nameDetails.forename2";
    public static final String INDIVIDUAL_NAME_FORENAME_3 = "defendant.individual.nameDetails.forename3";
    public static final String INDIVIDUAL_NAME_SURNAME = "defendant.individual.nameDetails.surname";
    public static final String INDIVIDUAL_CONTACT_WORK_PHONE = "defendant.individual.contactDetails.workTelephoneNumber";
    public static final String INDIVIDUAL_CONTACT_HOME_PHONE = "defendant.individual.contactDetails.homeTelephoneNumber";
    public static final String INDIVIDUAL_CONTACT_MOBILE_PHONE = "defendant.individual.contactDetails.mobileTelephoneNumber";
    public static final String INDIVIDUAL_CONTACT_PRIMARY_EMAIL = "defendant.individual.contactDetails.primaryEmail";
    public static final String INDIVIDUAL_CONTACT_SECONDARY_EMAIL = "defendant.individual.contactDetails.secondaryEmail";
    public static final String INDIVIDUAL_NATIONALITY = "defendant.individual.nationality";
    public static final String INDIVIDUAL_ADDITIONAL_NATIONALITY = "defendant.individual.additionalNationality";
    public static final String INDIVIDUAL_DATE_OF_BIRTH = "defendant.individual.dateOfBirth";
    public static final String INDIVIDUAL_GENDER = "defendant.individual.gender";
    public static final String INDIVIDUAL_OCCUPATION = "defendant.individual.occupation";
    public static final String INDIVIDUAL_OBSERVED_ETHNICITY = "defendant.individual.observedEthnicity";
    public static final String INDIVIDUAL_ETHNICITY = "defendant.individual.ethnicity";
    public static final String INDIVIDUAL_DRIVER_NUMBER = "defendant.individual.driverNumber";
    public static final String INDIVIDUAL_LANGUAGE_REQUIREMENT = "defendant.individual.languageRequirement";
    public static final String INDIVIDUAL_SPECIFIC_REQUIREMENTS = "defendant.individual.specificRequirements";
    public static final String INDIVIDUAL_NATIONAL_INSURANCE_NUMBER = "defendant.individual.nationalInsuranceNumber";
    public static final String INDIVIDUAL_CUSTODY_STATUS = "defendant.individual.custodyStatus";

    // Defendant -> individual -> parentGuardian (optional; oneOf individual/organisation, same pattern as defendant.type)
    public static final String PARENT_GUARDIAN_TYPE = "defendant.individual.parentGuardian.type";
    public static final String PARENT_GUARDIAN_ADDRESS_1 = "defendant.individual.parentGuardian.address.address1";
    public static final String PARENT_GUARDIAN_ADDRESS_2 = "defendant.individual.parentGuardian.address.address2";
    public static final String PARENT_GUARDIAN_ADDRESS_3 = "defendant.individual.parentGuardian.address.address3";
    public static final String PARENT_GUARDIAN_ADDRESS_4 = "defendant.individual.parentGuardian.address.address4";
    public static final String PARENT_GUARDIAN_ADDRESS_5 = "defendant.individual.parentGuardian.address.address5";
    public static final String PARENT_GUARDIAN_ADDRESS_POSTCODE = "defendant.individual.parentGuardian.address.postcode";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_NAME_TITLE = "defendant.individual.parentGuardian.individual.nameDetails.title";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME = "defendant.individual.parentGuardian.individual.nameDetails.forename";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_2 = "defendant.individual.parentGuardian.individual.nameDetails.forename2";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_3 = "defendant.individual.parentGuardian.individual.nameDetails.forename3";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_NAME_SURNAME = "defendant.individual.parentGuardian.individual.nameDetails.surname";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_CONTACT_WORK_PHONE = "defendant.individual.parentGuardian.individual.contactDetails.workTelephoneNumber";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_CONTACT_HOME_PHONE = "defendant.individual.parentGuardian.individual.contactDetails.homeTelephoneNumber";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_CONTACT_MOBILE_PHONE = "defendant.individual.parentGuardian.individual.contactDetails.mobileTelephoneNumber";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_CONTACT_PRIMARY_EMAIL = "defendant.individual.parentGuardian.individual.contactDetails.primaryEmail";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_CONTACT_SECONDARY_EMAIL = "defendant.individual.parentGuardian.individual.contactDetails.secondaryEmail";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH = "defendant.individual.parentGuardian.individual.dateOfBirth";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_GENDER = "defendant.individual.parentGuardian.individual.gender";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_OBSERVED_ETHNICITY = "defendant.individual.parentGuardian.individual.observedEthnicity";
    public static final String PARENT_GUARDIAN_INDIVIDUAL_SELF_DEFINED_ETHNICITY = "defendant.individual.parentGuardian.individual.selfDefinedEthnicity";
    public static final String PARENT_GUARDIAN_ORGANISATION_NAME = "defendant.individual.parentGuardian.organisation.organisationName";
    public static final String PARENT_GUARDIAN_ORGANISATION_COMPANY_TELEPHONE_NUMBER = "defendant.individual.parentGuardian.organisation.companyTelephoneNumber";

    // Defendant -> organisation
    public static final String ORGANISATION_NAME = "defendant.organisation.organisationName";
    public static final String ORGANISATION_COMPANY_TELEPHONE_NUMBER = "defendant.organisation.companyTelephoneNumber";

    // Offence-level (one offence per row)
    public static final String OFFENCE_CJS_OFFENCE_CODE = "offence.offenceDetails.cjsOffenceCode";
    public static final String OFFENCE_SEQUENCE_NO = "offence.offenceDetails.offenceSequenceNo";
    public static final String OFFENCE_COMMITTED_DATE = "offence.offenceDetails.offenceCommittedDate";
    public static final String OFFENCE_LAID_DATE = "offence.offenceDetails.laidDate";
    public static final String OFFENCE_COMMITTED_END_DATE = "offence.offenceDetails.offenceCommittedEndDate";
    public static final String OFFENCE_LOCATION = "offence.offenceDetails.offenceLocation";
    public static final String OFFENCE_WORDING = "offence.offenceDetails.offenceWording";
    public static final String OFFENCE_WORDING_WELSH = "offence.offenceDetails.offenceWordingWelsh";
    public static final String OFFENCE_PROSECUTOR_COMPENSATION = "offence.offenceDetails.prosecutorCompensation";
    public static final String OFFENCE_ARREST_DATE = "offence.arrestDate";
    public static final String OFFENCE_STATEMENT_OF_FACTS = "offence.statementOfFacts";
    public static final String OFFENCE_STATEMENT_OF_FACTS_WELSH = "offence.statementOfFactsWelsh";

    public static final String[] HEADERS = {
            PROSECUTING_AUTHORITY, HEARING_COURT_HEARING_LOCATION, HEARING_DATE_OF_HEARING, HEARING_TIME_OF_HEARING,
            CASE_URN, CASE_INFORMANT, CASE_SUMMONS_CODE, CASE_CASE_MARKER, CASE_RELATED_REFERENCE_NUMBER, CASE_PAYMENT_REFERENCE,
            DEFENDANT_PROSECUTOR_DEFENDANT_ID, DEFENDANT_ASN, DEFENDANT_PNC_IDENTIFIER, DEFENDANT_CRO_NUMBER,
            DEFENDANT_DOCUMENTATION_LANGUAGE, DEFENDANT_HEARING_LANGUAGE, DEFENDANT_PROSECUTOR_COSTS,
            DEFENDANT_ADDRESS_1, DEFENDANT_ADDRESS_2, DEFENDANT_ADDRESS_3, DEFENDANT_ADDRESS_4, DEFENDANT_ADDRESS_5, DEFENDANT_ADDRESS_POSTCODE,
            DEFENDANT_TYPE,
            INDIVIDUAL_NAME_TITLE, INDIVIDUAL_NAME_FORENAME, INDIVIDUAL_NAME_FORENAME_2, INDIVIDUAL_NAME_FORENAME_3, INDIVIDUAL_NAME_SURNAME,
            INDIVIDUAL_CONTACT_WORK_PHONE, INDIVIDUAL_CONTACT_HOME_PHONE, INDIVIDUAL_CONTACT_MOBILE_PHONE, INDIVIDUAL_CONTACT_PRIMARY_EMAIL, INDIVIDUAL_CONTACT_SECONDARY_EMAIL,
            INDIVIDUAL_NATIONALITY, INDIVIDUAL_ADDITIONAL_NATIONALITY, INDIVIDUAL_DATE_OF_BIRTH, INDIVIDUAL_GENDER, INDIVIDUAL_OCCUPATION,
            INDIVIDUAL_OBSERVED_ETHNICITY, INDIVIDUAL_ETHNICITY, INDIVIDUAL_DRIVER_NUMBER, INDIVIDUAL_LANGUAGE_REQUIREMENT, INDIVIDUAL_SPECIFIC_REQUIREMENTS,
            INDIVIDUAL_NATIONAL_INSURANCE_NUMBER, INDIVIDUAL_CUSTODY_STATUS,
            PARENT_GUARDIAN_TYPE,
            PARENT_GUARDIAN_ADDRESS_1, PARENT_GUARDIAN_ADDRESS_2, PARENT_GUARDIAN_ADDRESS_3, PARENT_GUARDIAN_ADDRESS_4, PARENT_GUARDIAN_ADDRESS_5, PARENT_GUARDIAN_ADDRESS_POSTCODE,
            PARENT_GUARDIAN_INDIVIDUAL_NAME_TITLE, PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME, PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_2, PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_3, PARENT_GUARDIAN_INDIVIDUAL_NAME_SURNAME,
            PARENT_GUARDIAN_INDIVIDUAL_CONTACT_WORK_PHONE, PARENT_GUARDIAN_INDIVIDUAL_CONTACT_HOME_PHONE, PARENT_GUARDIAN_INDIVIDUAL_CONTACT_MOBILE_PHONE, PARENT_GUARDIAN_INDIVIDUAL_CONTACT_PRIMARY_EMAIL, PARENT_GUARDIAN_INDIVIDUAL_CONTACT_SECONDARY_EMAIL,
            PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH, PARENT_GUARDIAN_INDIVIDUAL_GENDER, PARENT_GUARDIAN_INDIVIDUAL_OBSERVED_ETHNICITY, PARENT_GUARDIAN_INDIVIDUAL_SELF_DEFINED_ETHNICITY,
            PARENT_GUARDIAN_ORGANISATION_NAME, PARENT_GUARDIAN_ORGANISATION_COMPANY_TELEPHONE_NUMBER,
            ORGANISATION_NAME, ORGANISATION_COMPANY_TELEPHONE_NUMBER,
            OFFENCE_CJS_OFFENCE_CODE, OFFENCE_SEQUENCE_NO, OFFENCE_COMMITTED_DATE, OFFENCE_LAID_DATE, OFFENCE_COMMITTED_END_DATE,
            OFFENCE_LOCATION, OFFENCE_WORDING, OFFENCE_WORDING_WELSH, OFFENCE_PROSECUTOR_COMPENSATION,
            OFFENCE_ARREST_DATE, OFFENCE_STATEMENT_OF_FACTS, OFFENCE_STATEMENT_OF_FACTS_WELSH
    };

    private SummonsProsecutionCsvColumns() {
    }
}
