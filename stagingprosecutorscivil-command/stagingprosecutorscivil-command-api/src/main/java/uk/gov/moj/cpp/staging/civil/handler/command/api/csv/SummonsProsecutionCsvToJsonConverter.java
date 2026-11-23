package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.CASE_CASE_MARKER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.CASE_INFORMANT;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.CASE_PAYMENT_REFERENCE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.CASE_RELATED_REFERENCE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.CASE_SUMMONS_CODE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.CASE_URN;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_1;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_2;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_3;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_4;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_5;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_ADDRESS_POSTCODE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_ASN;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_CRO_NUMBER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_DOCUMENTATION_LANGUAGE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_HEARING_LANGUAGE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_PNC_IDENTIFIER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_COSTS;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_TYPE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_TYPE_INDIVIDUAL;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DEFENDANT_TYPE_ORGANISATION;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.DELIMITER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.HEARING_COURT_HEARING_LOCATION;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.HEARING_DATE_OF_HEARING;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.HEARING_TIME_OF_HEARING;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_ADDITIONAL_NATIONALITY;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_HOME_PHONE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_MOBILE_PHONE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_PRIMARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_SECONDARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CONTACT_WORK_PHONE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_CUSTODY_STATUS;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_DRIVER_NUMBER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_GENDER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_LANGUAGE_REQUIREMENT;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_FORENAME;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_FORENAME_2;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_FORENAME_3;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_SURNAME;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NAME_TITLE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NATIONALITY;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_NATIONAL_INSURANCE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_OCCUPATION;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.INDIVIDUAL_SPECIFIC_REQUIREMENTS;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_ARREST_DATE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_COMMITTED_END_DATE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_LAID_DATE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_LOCATION;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_PROSECUTOR_COMPENSATION;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_SEQUENCE_NO;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_STATEMENT_OF_FACTS;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_STATEMENT_OF_FACTS_WELSH;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_WORDING;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.OFFENCE_WORDING_WELSH;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.ORGANISATION_COMPANY_TELEPHONE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.ORGANISATION_NAME;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_1;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_2;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_3;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_4;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_5;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ADDRESS_POSTCODE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_HOME_PHONE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_MOBILE_PHONE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_PRIMARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_SECONDARY_EMAIL;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_CONTACT_WORK_PHONE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_GENDER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_2;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_FORENAME_3;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_SURNAME;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_NAME_TITLE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_INDIVIDUAL_SELF_DEFINED_ETHNICITY;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ORGANISATION_COMPANY_TELEPHONE_NUMBER;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_ORGANISATION_NAME;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PARENT_GUARDIAN_TYPE;
import static uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns.PROSECUTING_AUTHORITY;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.staging.civil.common.csv.SummonsProsecutionCsvColumns;
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
 * Reconstructs a {@code summons-prosecution} JSON request from the flattened, comma-delimited
 * CSV produced by {@code summons-prosecution-template.csv}.
 * <p>
 * The whole CSV file is treated as a single submission: {@code prosecutingAuthority} and
 * {@code hearingDetails.*} are read once, from the first data row. They must still be present,
 * well-formed and hold row 1's value in every row; a row that breaks this is reported, but only row
 * 1's values are sent forward. Each row represents exactly
 * one {@link SummonsProsecutionCase} for a single {@link Defendant} with a single {@link Offence}
 * - there is no multi-defendant-per-case flow, so {@code case.urn} must be unique across the rows
 * of a file; a repeated URN is rejected.
 * <p>
 * {@link #convert(Reader)} collects every row-level problem (blank required column, malformed date,
 * unknown enum value, duplicate URN) as a {@link ComplaintsFileValidationError} so the upload can be
 * accepted and the problems reported in the submission's error report; only file-level problems
 * are thrown as {@link IllegalArgumentException}.
 * <p>
 * The number of data rows accepted per file is capped by the {@code
 * stagingprosecutorscivil.complaints-files.max-rows} configuration value (defaults to 1000 and can
 * be set per environment); a file with more rows than the configured limit is rejected.
 */
public class SummonsProsecutionCsvToJsonConverter {

    /**
     * CSV dates must be ISO {@code YYYY-MM-DD}: exactly four year digits, two month digits and two
     * day digits. {@link LocalDate#parse(CharSequence)} alone also accepts signed, expanded years
     * such as {@code +12022-01-01}, so the shape is checked first.
     */
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    /**
     * Copies of the {@code timeOfHearing} pattern and {@code courtHearingLocation} max length in
     * {@code hearing-details.json}: the schema only sees row 1's hearing values, so every row is
     * checked against them here. {@code SubmissionLevelColumnRulesTest} fails if they drift apart.
     */
    static final String TIME_OF_HEARING_REGEX = "^(2[0-3]|[01]?[0-9]):([0-5]?[0-9]):([0-5]?[0-9])$";
    static final int COURT_HEARING_LOCATION_MAX_LENGTH = 7;
    private static final Pattern TIME_OF_HEARING_PATTERN = Pattern.compile(TIME_OF_HEARING_REGEX);

    /**
     * Columns that describe the whole file rather than the row's case: they are read from row 1 and
     * must hold the same value in every row.
     */
    private static final List<String> SUBMISSION_LEVEL_COLUMNS = List.of(PROSECUTING_AUTHORITY,
            HEARING_COURT_HEARING_LOCATION, HEARING_DATE_OF_HEARING, HEARING_TIME_OF_HEARING);

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(DELIMITER)
            .setHeader(SummonsProsecutionCsvColumns.HEADERS)
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Inject
    @Value(key = "stagingprosecutorscivil.complaints-files.max-rows", defaultValue = "1000")
    public String maxRows = "1000";

    public String convertToJson(final Reader csvReader) throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(convertToObject(csvReader));
    }

    /**
     * Strict conversion: fails on the first row-level problem found. Kept for callers that need a
     * complete, valid {@link Summons} or nothing - the complaints-file upload itself uses
     * {@link #convert(Reader)} so it can report every problem at once.
     *
     * @throws IllegalArgumentException for a file-level problem (see {@link #convert(Reader)}) or
     *                                  the first row-level problem in the file
     */
    public Summons convertToObject(final Reader csvReader) throws IOException {
        final ComplaintsFileConversionResult result = convert(csvReader);
        if (result.hasErrors()) {
            throw new IllegalArgumentException(result.getErrors().get(0).getMessage());
        }
        return result.getSummons()
                .orElseThrow(() -> new IllegalArgumentException("CSV file contains no data rows"));
    }

    /**
     * Converts the whole file, collecting every row-level problem instead of stopping at the first.
     * <p>
     * Rows with a problem are left out of the returned {@link Summons}; their problems are returned
     * in {@link ComplaintsFileConversionResult#getErrors()} keyed by the row's {@code case.urn}.
     * {@code prosecutingAuthority} and {@code hearingDetails.*} are only read from the first data
     * row, but are checked in every row: a blank or malformed value, or one that differs from row 1's
     * ({@link ComplaintsFileValidationError#INCONSISTENT_VALUE}), is reported against that row. Such
     * a problem does not stop the row's case from being converted, and the {@link Summons} is still
     * returned (with row 1's unreadable hearing date left blank) so the converted cases can be
     * schema-validated and every problem reported together.
     *
     * @throws IllegalArgumentException for problems that mean the file cannot be processed or
     *                                  attributed to a prosecutor at all: no data rows, more rows than
     *                                  the configured limit, a first data row that does not match the
     *                                  template's columns, or a blank {@code prosecutingAuthority}
     */
    public ComplaintsFileConversionResult convert(final Reader csvReader) throws IOException {
        try (CSVParser parser = CSV_FORMAT.parse(csvReader)) {
            final int maxRowsAllowed = Integer.parseInt(maxRows);
            final List<ComplaintsFileValidationError> errors = new ArrayList<>();
            final Set<String> seenCaseUrns = new HashSet<>();
            final List<SummonsProsecutionCase> prosecutionCases = new ArrayList<>();
            final List<ComplaintsFileConversionResult.ConvertedRow> convertedRows = new ArrayList<>();
            HearingDetails hearingDetails = null;
            String prosecutingAuthority = null;
            Row firstRow = null;
            Set<String> firstRowInvalidColumns = Set.of();
            int rowCount = 0;

            for (final CSVRecord csvRecord : parser) {
                rowCount++;
                if (rowCount > maxRowsAllowed) {
                    throw new IllegalArgumentException("CSV file exceeds the maximum allowed number of rows ("
                            + maxRowsAllowed + ")");
                }

                final Row record = new Row(csvRecord);

                if (rowCount == 1) {
                    if (record.isMissingColumns()) {
                        throw new IllegalArgumentException("Row " + record.getRecordNumber() + " has "
                                + csvRecord.size() + " columns but the complaints file template has "
                                + SummonsProsecutionCsvColumns.HEADERS.length);
                    }
                    prosecutingAuthority = nullIfBlank(record.get(PROSECUTING_AUTHORITY));
                    if (prosecutingAuthority == null) {
                        throw new IllegalArgumentException("Row " + record.getRecordNumber() + ": required column '"
                                + PROSECUTING_AUTHORITY + "' is blank");
                    }
                    firstRow = record;
                    // only row 1's values are sent forward; an unreadable hearing date is left blank
                    hearingDetails = HearingDetails.hearingDetails()
                            .withCourtHearingLocation(nullIfBlank(record.get(HEARING_COURT_HEARING_LOCATION)))
                            .withDateOfHearing(isoDateOrNull(nullIfBlank(record.get(HEARING_DATE_OF_HEARING))))
                            .withTimeOfHearing(nullIfBlank(record.get(HEARING_TIME_OF_HEARING)))
                            .build();
                }

                if (record.isMissingColumns()) {
                    record.error(ComplaintsFileValidationError.INVALID_ROW, null, "Row " + record.getRecordNumber()
                            + " has " + csvRecord.size() + " columns but the complaints file template has "
                            + SummonsProsecutionCsvColumns.HEADERS.length);
                    errors.addAll(record.errors);
                    continue;
                }

                // these columns belong to the whole file, not to the row's case - report their
                // problems against the row, but don't let them exclude its case from conversion
                final Set<String> invalidColumns = validateSubmissionLevelColumns(record, firstRow, firstRowInvalidColumns);
                if (record == firstRow) {
                    firstRowInvalidColumns = invalidColumns;
                }
                errors.addAll(record.errors);
                record.errors.clear();

                final SummonsProsecutionCase prosecutionCase = buildProsecutionCase(record);

                final String caseUrn = nullIfBlank(record.get(CASE_URN));
                if (caseUrn != null && !seenCaseUrns.add(caseUrn)) {
                    record.error(ComplaintsFileValidationError.DUPLICATE_CASE_URN, CASE_URN,
                            "Row " + record.getRecordNumber() + ": duplicate case URN '" + caseUrn
                                    + "' - each row must have a unique " + CASE_URN);
                }

                if (record.hasErrors()) {
                    errors.addAll(record.errors);
                } else {
                    prosecutionCases.add(prosecutionCase);
                    convertedRows.add(new ComplaintsFileConversionResult.ConvertedRow(
                            record.getRecordNumber(), record.caseReference, record.defendantReference));
                }
            }

            if (rowCount == 0) {
                throw new IllegalArgumentException("CSV file contains no data rows");
            }

            final Summons summons = !prosecutionCases.isEmpty()
                    ? Summons.summons()
                            .withHearingDetails(hearingDetails)
                            .withProsecutingAuthority(prosecutingAuthority)
                            .withProsecutionCases(prosecutionCases)
                            .build()
                    : null;

            return new ComplaintsFileConversionResult(prosecutingAuthority, summons, convertedRows, errors);
        }
    }

    /**
     * Each row is exactly one case with one defendant and one offence.
     */
    private static SummonsProsecutionCase buildProsecutionCase(final Row record) {
        final SummonsProsecutionCase.Builder caseBuilder = newCaseBuilder(record);
        final DefendantAccumulator defendantAccumulator = newDefendantAccumulator(record);
        defendantAccumulator.offences.add(buildOffence(record));

        final Defendant defendant = Defendant.defendant()
                .withDefendantDetails(defendantAccumulator.defendantDetails)
                .withIndividual(defendantAccumulator.individual)
                .withOrganisation(defendantAccumulator.organisation)
                .withOffences(defendantAccumulator.offences)
                .build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);
        return caseBuilder.withDefendants(defendants).build();
    }

    private static SummonsProsecutionCase.Builder newCaseBuilder(final Row record) {
        return SummonsProsecutionCase.summonsProsecutionCase()
                .withUrn(requireNonBlank(record, CASE_URN))
                .withInformant(nullIfBlank(record.get(CASE_INFORMANT)))
                .withSummonsCode(requireNonBlank(record, CASE_SUMMONS_CODE))
                .withCaseMarker(nullIfBlank(record.get(CASE_CASE_MARKER)))
                .withRelatedReferenceNumber(nullIfBlank(record.get(CASE_RELATED_REFERENCE_NUMBER)))
                .withPaymentReference(nullIfBlank(record.get(CASE_PAYMENT_REFERENCE)));
    }

    private static DefendantAccumulator newDefendantAccumulator(final Row record) {
        final DefendantAccumulator accumulator = new DefendantAccumulator();
        accumulator.defendantDetails = buildDefendantDetails(record);

        final String defendantType = requireNonBlank(record, DEFENDANT_TYPE);
        if (defendantType == null) {
            return accumulator;
        }
        switch (defendantType) {
            case DEFENDANT_TYPE_INDIVIDUAL:
                accumulator.individual = buildIndividual(record);
                break;
            case DEFENDANT_TYPE_ORGANISATION:
                accumulator.organisation = buildOrganisation(record);
                break;
            default:
                record.error(ComplaintsFileValidationError.INVALID_VALUE, DEFENDANT_TYPE,
                        "Row " + record.getRecordNumber() + ": column '" + DEFENDANT_TYPE + "' must be '"
                                + DEFENDANT_TYPE_INDIVIDUAL + "' or '" + DEFENDANT_TYPE_ORGANISATION
                                + "' but was '" + defendantType + "'");
        }
        return accumulator;
    }

    private static DefendantDetails buildDefendantDetails(final Row record) {
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

    private static Address buildAddress(final Row record, final String address1Column, final String address2Column,
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

    private static Individual buildIndividual(final Row record) {
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

    private static ParentGuardian buildParentGuardian(final Row record) {
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
                record.error(ComplaintsFileValidationError.INVALID_VALUE, PARENT_GUARDIAN_TYPE,
                        "Row " + record.getRecordNumber() + ": column '" + PARENT_GUARDIAN_TYPE + "' must be '"
                                + DEFENDANT_TYPE_INDIVIDUAL + "' or '" + DEFENDANT_TYPE_ORGANISATION
                                + "' but was '" + parentGuardianType + "'");
        }
        return builder.build();
    }

    private static ContactDetails buildContactDetails(final Row record, final String workPhoneColumn,
                                                        final String homePhoneColumn, final String mobilePhoneColumn,
                                                        final String primaryEmailColumn, final String secondaryEmailColumn) {
        final String workPhone = nullIfBlank(record.get(workPhoneColumn));
        final String homePhone = nullIfBlank(record.get(homePhoneColumn));
        final String mobilePhone = nullIfBlank(record.get(mobilePhoneColumn));
        final String primaryEmail = nullIfBlank(record.get(primaryEmailColumn));
        final String secondaryEmail = nullIfBlank(record.get(secondaryEmailColumn));

        final boolean isPhoneValuesNull = workPhone == null && homePhone == null && mobilePhone == null;
        final boolean isEmailValuesNull = primaryEmail == null && secondaryEmail == null;

        if (isEmailValuesNull && isPhoneValuesNull) {
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

    private static Organisation buildOrganisation(final Row record) {
        return Organisation.organisation()
                .withOrganisationName(requireNonBlank(record, ORGANISATION_NAME))
                .withCompanyTelephoneNumber(nullIfBlank(record.get(ORGANISATION_COMPANY_TELEPHONE_NUMBER)))
                .build();
    }

    private static Offence buildOffence(final Row record) {
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

    // The helpers below never throw for a bad value: they record the problem against the row and
    // return null, so every problem in a row is collected in a single pass.

    private static String requireNonBlank(final Row record, final String column) {
        final String value = nullIfBlank(record.get(column));
        if (value == null) {
            record.error(ComplaintsFileValidationError.REQUIRED_FIELD_MISSING, column,
                    "Row " + record.getRecordNumber() + ": required column '" + column + "' is blank");
        }
        return value;
    }

    private static LocalDate parseDate(final Row record, final String column) {
        final String value = nullIfBlank(record.get(column));
        return value == null ? null : toIsoDate(record, column, value);
    }

    private static LocalDate requireDate(final Row record, final String column) {
        final String value = requireNonBlank(record, column);
        return value == null ? null : toIsoDate(record, column, value);
    }

    private static LocalDate toIsoDate(final Row record, final String column, final String value) {
        final LocalDate date = isoDateOrNull(value);
        if (date == null) {
            record.error(ComplaintsFileValidationError.INVALID_DATE_FORMAT, column,
                    "Row " + record.getRecordNumber() + ": '" + column + "' has invalid date '"
                            + value + "' - expected ISO date format YYYY-MM-DD");
        }
        return date;
    }

    /**
     * @return the date, or {@code null} for a blank value, a value not shaped {@code YYYY-MM-DD}, or
     * a well-formed value that is not a real calendar date such as {@code 2022-02-30}
     */
    private static LocalDate isoDateOrNull(final String value) {
        if (value == null || !ISO_DATE_PATTERN.matcher(value).matches()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Checks the submission-level columns of one row: each must be present and well-formed, and -
     * after row 1 - hold the same value as row 1. {@code prosecutingAuthority} is not checked on row
     * 1 here: a blank value there rejects the whole file, and its format is schema-validated.
     * <p>
     * A cell gets at most one error: a blank or malformed value is not also reported as inconsistent,
     * and nothing is compared with a blank or malformed row 1 value (row 1 has its own error for it,
     * and fixing row 1 is what the prosecutor needs to do).
     *
     * @return the columns of this row whose value is blank or malformed
     */
    private static Set<String> validateSubmissionLevelColumns(final Row record, final Row firstRow,
                                                              final Set<String> firstRowInvalidColumns) {
        final boolean isFirstRow = record == firstRow;
        final Set<String> invalidColumns = new HashSet<>();

        if (!isFirstRow && requireNonBlank(record, PROSECUTING_AUTHORITY) == null) {
            invalidColumns.add(PROSECUTING_AUTHORITY);
        }

        final String courtHearingLocation = requireNonBlank(record, HEARING_COURT_HEARING_LOCATION);
        if (courtHearingLocation == null) {
            invalidColumns.add(HEARING_COURT_HEARING_LOCATION);
        } else if (courtHearingLocation.length() > COURT_HEARING_LOCATION_MAX_LENGTH) {
            record.error(ComplaintsFileValidationError.INVALID_VALUE, HEARING_COURT_HEARING_LOCATION,
                    "Row " + record.getRecordNumber() + ": column '" + HEARING_COURT_HEARING_LOCATION
                            + "' must be at most " + COURT_HEARING_LOCATION_MAX_LENGTH + " characters but was '"
                            + courtHearingLocation + "'");
            invalidColumns.add(HEARING_COURT_HEARING_LOCATION);
        }

        if (requireDate(record, HEARING_DATE_OF_HEARING) == null) {
            invalidColumns.add(HEARING_DATE_OF_HEARING);
        }

        final String timeOfHearing = requireNonBlank(record, HEARING_TIME_OF_HEARING);
        if (timeOfHearing == null) {
            invalidColumns.add(HEARING_TIME_OF_HEARING);
        } else if (!TIME_OF_HEARING_PATTERN.matcher(timeOfHearing).matches()) {
            record.error(ComplaintsFileValidationError.INVALID_VALUE, HEARING_TIME_OF_HEARING,
                    "Row " + record.getRecordNumber() + ": column '" + HEARING_TIME_OF_HEARING
                            + "' has invalid time '" + timeOfHearing + "' - expected HH:MM:SS");
            invalidColumns.add(HEARING_TIME_OF_HEARING);
        }

        if (isFirstRow) {
            return invalidColumns;
        }
        for (final String column : SUBMISSION_LEVEL_COLUMNS) {
            if (invalidColumns.contains(column) || firstRowInvalidColumns.contains(column)) {
                continue;
            }
            final String firstRowValue = firstRow.get(column);
            final String value = record.get(column);
            if (!sameValue(column, firstRowValue, value)) {
                record.error(ComplaintsFileValidationError.INCONSISTENT_VALUE, column,
                        "Row " + record.getRecordNumber() + ": column '" + column + "' is '" + value
                                + "' but row " + firstRow.getRecordNumber() + " has '" + firstRowValue
                                + "' - it must be the same in every row");
            }
        }
        return invalidColumns;
    }

    /**
     * Both values are present and well-formed. Times are compared by value, since the schema's
     * pattern allows optional leading zeros ({@code 9:05:00} is {@code 09:05:00}); everything else
     * must match exactly.
     */
    private static boolean sameValue(final String column, final String first, final String other) {
        if (HEARING_TIME_OF_HEARING.equals(column)) {
            return normalisedTime(first).equals(normalisedTime(other));
        }
        return first.equals(other);
    }

    private static String normalisedTime(final String time) {
        final Matcher matcher = TIME_OF_HEARING_PATTERN.matcher(time);
        if (!matcher.matches()) {
            return time;
        }
        return String.format("%02d:%02d:%02d", Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    private static Integer requireInteger(final Row record, final String column) {
        final String value = requireNonBlank(record, column);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            record.error(ComplaintsFileValidationError.INVALID_VALUE, column,
                    "Row " + record.getRecordNumber() + ": column '" + column + "' has invalid whole number value '"
                            + value + "'");
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(final Row record, final String column) {
        final String value = nullIfBlank(record.get(column));
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            record.error(ComplaintsFileValidationError.INVALID_VALUE, column,
                    "Row " + record.getRecordNumber() + ": column '" + column + "' has invalid numeric value '"
                            + value + "'");
            return null;
        }
    }

    private static Gender requireGender(final Row record, final String column) {
        final String value = requireNonBlank(record, column);
        if (value == null) {
            return null;
        }
        Optional<Gender> gender;
        try {
            gender = Gender.valueFor(Integer.valueOf(value));
        } catch (NumberFormatException e) {
            gender = Optional.empty();
        }
        if (gender.isEmpty()) {
            record.error(ComplaintsFileValidationError.INVALID_VALUE, column,
                    "Row " + record.getRecordNumber() + ": column '" + column + "' has invalid gender value '"
                            + value + "'");
        }
        return gender.orElse(null);
    }

    private static Language requireLanguage(final Row record, final String column) {
        final String value = requireNonBlank(record, column);
        if (value == null) {
            return null;
        }
        final Optional<Language> language = Language.valueFor(value);
        if (language.isEmpty()) {
            record.error(ComplaintsFileValidationError.INVALID_VALUE, column,
                    "Row " + record.getRecordNumber() + ": column '" + column + "' has invalid language value '"
                            + value + "'");
        }
        return language.orElse(null);
    }

    /**
     * One CSV data row plus the problems found while converting it.
     */
    private static final class Row {
        private final CSVRecord csvRecord;
        private final String caseReference;
        private final String defendantReference;
        private final List<ComplaintsFileValidationError> errors = new ArrayList<>();

        private Row(final CSVRecord csvRecord) {
            this.csvRecord = csvRecord;
            this.caseReference = valueOrNull(csvRecord, CASE_URN);
            this.defendantReference = valueOrNull(csvRecord, DEFENDANT_PROSECUTOR_DEFENDANT_ID);
        }

        private static String valueOrNull(final CSVRecord csvRecord, final String column) {
            return csvRecord.isSet(column) ? nullIfBlank(csvRecord.get(column)) : null;
        }

        private String get(final String column) {
            return csvRecord.get(column);
        }

        private long getRecordNumber() {
            return csvRecord.getRecordNumber();
        }

        private boolean isMissingColumns() {
            return csvRecord.size() < SummonsProsecutionCsvColumns.HEADERS.length;
        }

        private void error(final String code, final String column, final String message) {
            final String rawValue = column != null && csvRecord.isSet(column) ? csvRecord.get(column) : "";
            errors.add(new ComplaintsFileValidationError(getRecordNumber(), caseReference, defendantReference,
                    code, column, rawValue, message));
        }

        private boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private static final class DefendantAccumulator {
        private DefendantDetails defendantDetails;
        private Individual individual;
        private Organisation organisation;
        private final List<Offence> offences = new ArrayList<>();
    }
}
