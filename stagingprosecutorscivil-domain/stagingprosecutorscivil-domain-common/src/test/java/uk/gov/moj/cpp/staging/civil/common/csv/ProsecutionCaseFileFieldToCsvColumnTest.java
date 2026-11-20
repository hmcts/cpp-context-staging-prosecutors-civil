package uk.gov.moj.cpp.staging.civil.common.csv;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ProsecutionCaseFileFieldToCsvColumnTest {

    /**
     * Every problem-value key Prosecution Case File can put in a business-validation error: its
     * {@code FieldName} enum values plus the keys its validation rules write inline. Copied from
     * {@code cpp-context-prosecution-casefile} - add new keys here when it gains them, and either map
     * them or list them in {@link #KEYS_WITHOUT_TEMPLATE_COLUMN}.
     */
    private static final List<String> KNOWN_PROSECUTION_CASE_FILE_KEYS = asList(
            // FieldName enum
            "summonsCode", "initiationCode", "cpsOrganisation", "documentType", "prosecutorDefendantId",
            "individual_selfDefinedInformation_nationality",
            "individual_personalInformation_contactDetails_primaryEmail",
            "individual_personalInformation_contactDetails_secondaryEmail",
            "emailAddress1", "emailAddress2",
            "individual_selfDefinedInformation_additional_nationality",
            "individual_selfDefinedInformation_dateOfBirth", "defendantInitiationCode",
            "individual_perceivedBirthYear", "individual_offenderCode", "individual_bailConditions", "custodyStatus",
            "individual_personalInformation_observedEthnicity", "individual_selfDefinedInformation_ethnicity",
            "initialHearing_courtHearingLocation", "initialHearing_dateOfHearing",
            "offence_vehicleRelatedOffence_vehicleCode", "offence_arrestDate", "offence_chargeDate", "offence_laidDate",
            "offence_statementOfFacts", "offence_statementOfFactsWelsh",
            "individual_parentGuardianInformation_selfDefinedEthnicity",
            "individual_parentGuardianInformation_observedEthnicity",
            "individual_parentGuardianInformation_dateOfBirth",
            "individual_parentGuardianInformation_personalInformation_contactDetails_primaryEmail",
            "individual_parentGuardianInformation_personalInformation_contactDetails_secondaryEmail",
            "offence_alcoholRelatedOffence_alcoholLevelMethod", "offence_alcoholRelatedOffence_alcoholLevelAmount",
            "offence_offenceCode", "offence_offenceSequenceNo", "offence_offenceLocation", "offence_offenceDateCode",
            "statementOfFactsWelsh", "caseMarkers", "ouCode", "courtReceivedToCode", "courtReceivedFromCode",
            "policeForceCode", "hearingTypeCode", "prosecutingAuthority",
            "offence_backduty_value", "offence_backduty_fromDate", "offence_backduty_toDate", "offence_backduty_fromDate_toDate",
            "pncIdentifier", "address_postcode",
            "individual_parentGuardianInformation_personalInformation_address_postcode",
            "individual_personalInformation_address_postcode", "croNumber",
            "title", "forename", "forename2", "forename3", "surename", "dataOfBirth",
            "cpsDefendantId", "asn", "courtApplicationId", "organisationName", "listDefendantRequests",
            // inline keys in validation rules
            "offenceCode", "offenceSequenceNo", "dateOfBirth", "dob", "chargeDate", "offenceInEffectDates",
            "daysOverdue", "referralReasonId", "caseInSession", "groupProsecutions.size");

    /**
     * Keys with no complaints-template column; the error report shows them unchanged.
     */
    private static final Set<String> KEYS_WITHOUT_TEMPLATE_COLUMN = new HashSet<>(asList(
            "initiationCode", "cpsOrganisation", "documentType", "emailAddress1", "emailAddress2",
            "defendantInitiationCode", "individual_perceivedBirthYear", "individual_offenderCode",
            "individual_bailConditions", "offence_vehicleRelatedOffence_vehicleCode", "offence_chargeDate",
            "offence_alcoholRelatedOffence_alcoholLevelMethod", "offence_alcoholRelatedOffence_alcoholLevelAmount",
            "offence_offenceDateCode", "ouCode", "courtReceivedToCode", "courtReceivedFromCode", "policeForceCode",
            "hearingTypeCode", "offence_backduty_value", "offence_backduty_fromDate", "offence_backduty_toDate",
            "offence_backduty_fromDate_toDate", "cpsDefendantId", "courtApplicationId", "listDefendantRequests",
            "chargeDate", "offenceInEffectDates", "daysOverdue", "referralReasonId", "caseInSession",
            "groupProsecutions.size"));

    @Test
    void everyKnownKeyIsEitherMappedOrDeliberatelyLeftUnmapped() {
        final List<String> unaccounted = KNOWN_PROSECUTION_CASE_FILE_KEYS.stream()
                .filter(key -> !ProsecutionCaseFileFieldToCsvColumn.mappings().containsKey(key))
                .filter(key -> !KEYS_WITHOUT_TEMPLATE_COLUMN.contains(key))
                .collect(Collectors.toList());

        assertTrue(unaccounted.isEmpty(), "Prosecution Case File keys neither mapped nor listed as unmapped: " + unaccounted);
    }

    @Test
    void noKeyIsBothMappedAndListedAsUnmapped() {
        final List<String> both = ProsecutionCaseFileFieldToCsvColumn.mappings().keySet().stream()
                .filter(KEYS_WITHOUT_TEMPLATE_COLUMN::contains)
                .collect(Collectors.toList());

        assertTrue(both.isEmpty(), "Keys both mapped and listed as unmapped: " + both);
    }

    @Test
    void everyMappedColumnIsATemplateColumn() {
        final Set<String> templateColumns = new HashSet<>(asList(SummonsProsecutionCsvColumns.HEADERS));
        final List<String> notInTemplate = java.util.stream.Stream.concat(
                        ProsecutionCaseFileFieldToCsvColumn.mappings().values().stream(),
                        ProsecutionCaseFileFieldToCsvColumn.codeSpecificMappings().values().stream())
                .filter(column -> !templateColumns.contains(column))
                .collect(Collectors.toList());

        assertTrue(notInTemplate.isEmpty(), "Mapped to columns that are not in the template: " + notInTemplate);
    }

    @Test
    void mapsKnownKeysToTheirTemplateColumn() {
        assertEquals(Optional.of(SummonsProsecutionCsvColumns.OFFENCE_CJS_OFFENCE_CODE),
                ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("offence_offenceCode"));
        assertEquals(Optional.of(SummonsProsecutionCsvColumns.INDIVIDUAL_DATE_OF_BIRTH),
                ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("individual_selfDefinedInformation_dateOfBirth"));
        assertEquals(Optional.of(SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_POSTCODE),
                ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("individual_personalInformation_address_postcode"));
        assertEquals(Optional.of(SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH),
                ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("individual_parentGuardianInformation_dateOfBirth"));
    }

    @Test
    void usesTheProblemCodeForKeysProsecutionCaseFileReusesForDifferentFields() {
        assertEquals(Optional.of(SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH),
                ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE", "dateOfBirth"));
        assertEquals(Optional.of(SummonsProsecutionCsvColumns.INDIVIDUAL_DATE_OF_BIRTH),
                ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("DEFENDANT_DOB_IN_FUTURE", "dateOfBirth"));
        assertEquals(Optional.of(SummonsProsecutionCsvColumns.OFFENCE_CJS_OFFENCE_CODE),
                ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("OFFENCE_CODE_NOT_SUPPORTED", "offence_offenceCode"));
        assertFalse(ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("CASE_COUNT_EXCEEDED", "groupProsecutions.size").isPresent());
    }

    @Test
    void returnsEmptyForUnmappedTemplateColumnAndNullKeys() {
        assertFalse(ProsecutionCaseFileFieldToCsvColumn.toCsvColumn("groupProsecutions.size").isPresent());
        assertFalse(ProsecutionCaseFileFieldToCsvColumn.toCsvColumn(SummonsProsecutionCsvColumns.INDIVIDUAL_DATE_OF_BIRTH).isPresent());
        assertFalse(ProsecutionCaseFileFieldToCsvColumn.toCsvColumn(null).isPresent());
    }
}
