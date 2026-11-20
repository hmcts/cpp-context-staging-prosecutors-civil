package uk.gov.moj.cpp.staging.civil.common.csv;

import static java.util.Collections.unmodifiableMap;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps the problem-value keys that Prosecution Case File uses in business-validation errors
 * (its {@code FieldName} enum values, plus the few keys it writes inline) to the complaints CSV
 * template column that holds the same value, so the error report can name the column the user has
 * to correct in their file.
 * <p>
 * Target columns are always {@link SummonsProsecutionCsvColumns} constants - the same constants the
 * CSV-to-JSON conversion and schema-error reporting use - so the error report cannot name a column
 * that is not in the template.
 * <p>
 * Keys with no equivalent template column (for example {@code groupProsecutions.size},
 * {@code ouCode}, {@code courtReceivedToCode}, {@code chargeDate}) are deliberately absent:
 * {@link #toCsvColumn(String)} returns empty and callers keep the raw key.
 * <p>
 * Source of the keys: {@code cpp-context-prosecution-casefile},
 * {@code prosecutioncasefile-domain-aggregate/.../validation/rules/FieldName.java} and the inline
 * keys in its validation rules. Keep this in step when Prosecution Case File adds keys that civil
 * summons submissions can produce.
 */
public final class ProsecutionCaseFileFieldToCsvColumn {

    private static final Map<String, String> KEY_TO_COLUMN = buildMapping();

    /**
     * Keys Prosecution Case File reuses for different fields, told apart by the problem code -
     * e.g. {@code dateOfBirth} is the defendant's in {@code DEFENDANT_DOB_IN_FUTURE} but the parent or
     * guardian's in {@code DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE}. Keyed by
     * {@code code + "|" + key}; checked before {@link #KEY_TO_COLUMN}.
     */
    private static final Map<String, String> CODE_AND_KEY_TO_COLUMN = Map.of(
            codeAndKey("DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE", "dateOfBirth"), PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH);

    private ProsecutionCaseFileFieldToCsvColumn() {
    }

    /**
     * @return the template column for a Prosecution Case File problem key, or empty if the key has
     * no template column
     */
    public static Optional<String> toCsvColumn(final String prosecutionCaseFileKey) {
        return Optional.ofNullable(prosecutionCaseFileKey).map(KEY_TO_COLUMN::get);
    }

    /**
     * As {@link #toCsvColumn(String)}, but resolves keys that Prosecution Case File reuses for
     * different fields using the problem code.
     */
    public static Optional<String> toCsvColumn(final String problemCode, final String prosecutionCaseFileKey) {
        final String byCode = CODE_AND_KEY_TO_COLUMN.get(codeAndKey(problemCode, prosecutionCaseFileKey));
        return byCode != null ? Optional.of(byCode) : toCsvColumn(prosecutionCaseFileKey);
    }

    /**
     * Every mapped key and its column, for tests that keep the mapping and the template in step.
     */
    public static Map<String, String> mappings() {
        return KEY_TO_COLUMN;
    }

    /**
     * Every code-specific mapping ({@code code|key} -> column), for the same tests.
     */
    public static Map<String, String> codeSpecificMappings() {
        return CODE_AND_KEY_TO_COLUMN;
    }

    private static String codeAndKey(final String code, final String key) {
        return code + "|" + key;
    }

    @SuppressWarnings("squid:S138")
    private static Map<String, String> buildMapping() {
        final Map<String, String> mapping = new HashMap<>();

        // submission / hearing
        mapping.put("prosecutingAuthority", PROSECUTING_AUTHORITY);
        mapping.put("initialHearing_courtHearingLocation", HEARING_COURT_HEARING_LOCATION);
        mapping.put("initialHearing_dateOfHearing", HEARING_DATE_OF_HEARING);

        // case
        mapping.put("summonsCode", CASE_SUMMONS_CODE);
        mapping.put("caseMarkers", CASE_CASE_MARKER);

        // defendant
        mapping.put("prosecutorDefendantId", DEFENDANT_PROSECUTOR_DEFENDANT_ID);
        mapping.put("asn", DEFENDANT_ASN);
        mapping.put("pncIdentifier", DEFENDANT_PNC_IDENTIFIER);
        mapping.put("croNumber", DEFENDANT_CRO_NUMBER);
        mapping.put("address_postcode", DEFENDANT_ADDRESS_POSTCODE);
        mapping.put("individual_personalInformation_address_postcode", DEFENDANT_ADDRESS_POSTCODE);

        // defendant -> individual
        mapping.put("title", INDIVIDUAL_NAME_TITLE);
        mapping.put("forename", INDIVIDUAL_NAME_FORENAME);
        mapping.put("forename2", INDIVIDUAL_NAME_FORENAME_2);
        mapping.put("forename3", INDIVIDUAL_NAME_FORENAME_3);
        mapping.put("surename", INDIVIDUAL_NAME_SURNAME); // sic - Prosecution Case File's FieldName.SURNAME
        mapping.put("individual_personalInformation_contactDetails_primaryEmail", INDIVIDUAL_CONTACT_PRIMARY_EMAIL);
        mapping.put("individual_personalInformation_contactDetails_secondaryEmail", INDIVIDUAL_CONTACT_SECONDARY_EMAIL);
        mapping.put("individual_selfDefinedInformation_nationality", INDIVIDUAL_NATIONALITY);
        mapping.put("individual_selfDefinedInformation_additional_nationality", INDIVIDUAL_ADDITIONAL_NATIONALITY);
        mapping.put("individual_selfDefinedInformation_dateOfBirth", INDIVIDUAL_DATE_OF_BIRTH);
        mapping.put("dataOfBirth", INDIVIDUAL_DATE_OF_BIRTH); // sic - Prosecution Case File's FieldName.DATE_OF_BIRTH
        mapping.put("dateOfBirth", INDIVIDUAL_DATE_OF_BIRTH); // except DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE - see CODE_AND_KEY_TO_COLUMN
        mapping.put("dob", INDIVIDUAL_DATE_OF_BIRTH);
        mapping.put("individual_personalInformation_observedEthnicity", INDIVIDUAL_OBSERVED_ETHNICITY);
        mapping.put("individual_selfDefinedInformation_ethnicity", INDIVIDUAL_ETHNICITY);
        mapping.put("custodyStatus", INDIVIDUAL_CUSTODY_STATUS);

        // defendant -> individual -> parentGuardian
        mapping.put("individual_parentGuardianInformation_personalInformation_address_postcode", PARENT_GUARDIAN_ADDRESS_POSTCODE);
        mapping.put("individual_parentGuardianInformation_dateOfBirth", PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH);
        mapping.put("individual_parentGuardianInformation_observedEthnicity", PARENT_GUARDIAN_INDIVIDUAL_OBSERVED_ETHNICITY);
        mapping.put("individual_parentGuardianInformation_selfDefinedEthnicity", PARENT_GUARDIAN_INDIVIDUAL_SELF_DEFINED_ETHNICITY);
        mapping.put("individual_parentGuardianInformation_personalInformation_contactDetails_primaryEmail", PARENT_GUARDIAN_INDIVIDUAL_CONTACT_PRIMARY_EMAIL);
        mapping.put("individual_parentGuardianInformation_personalInformation_contactDetails_secondaryEmail", PARENT_GUARDIAN_INDIVIDUAL_CONTACT_SECONDARY_EMAIL);

        // defendant -> organisation
        mapping.put("organisationName", ORGANISATION_NAME);

        // offence
        mapping.put("offence_offenceCode", OFFENCE_CJS_OFFENCE_CODE);
        mapping.put("offenceCode", OFFENCE_CJS_OFFENCE_CODE);
        mapping.put("offence_offenceSequenceNo", OFFENCE_SEQUENCE_NO);
        mapping.put("offenceSequenceNo", OFFENCE_SEQUENCE_NO);
        mapping.put("offence_laidDate", OFFENCE_LAID_DATE);
        mapping.put("offence_offenceLocation", OFFENCE_LOCATION);
        mapping.put("offence_arrestDate", OFFENCE_ARREST_DATE);
        mapping.put("offence_statementOfFacts", OFFENCE_STATEMENT_OF_FACTS);
        mapping.put("offence_statementOfFactsWelsh", OFFENCE_STATEMENT_OF_FACTS_WELSH);
        mapping.put("statementOfFactsWelsh", OFFENCE_STATEMENT_OF_FACTS_WELSH);

        return unmodifiableMap(mapping);
    }
}
