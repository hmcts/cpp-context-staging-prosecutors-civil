package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

import static java.util.Collections.singletonList;

import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileConversionResult.ConvertedRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.everit.json.schema.ValidationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Turns a failed {@code stagingcivil.summons} JSON-schema validation of a converted complaints CSV
 * file into one {@link ComplaintsFileValidationError} per violation, each traced back to the CSV row
 * (and its {@code case.urn}) and CSV column it came from.
 * <p>
 * The framework's {@code JsonSchemaValidator} wraps the everit {@link ValidationException} - which
 * carries every violation as a tree of causing exceptions - in its own exception. Each leaf has a
 * JSON pointer such as {@code #/prosecutionCases/3/defendants/0/individual/dateOfBirth}: the case
 * index identifies the row, and the remaining path maps onto the template's column names
 * ({@code defendant.individual.dateOfBirth}).
 */
public final class SummonsSchemaViolationMapper {

    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String DEFENDANTS = "defendants";
    private static final String DEFENDANT_DETAILS = "defendantDetails";
    private static final String OFFENCES = "offences";
    private static final Pattern REQUIRED_KEY = Pattern.compile("required key \\[(.+?)\\] not found");
    private static final Pattern NUMERIC = Pattern.compile("\\d+");
    private static final long FIRST_DATA_ROW = 1L;

    private SummonsSchemaViolationMapper() {
    }

    /**
     * @param schemaFailure the exception thrown by the schema validator
     * @param convertedRows the rows behind {@code summons.prosecutionCases}, index for index
     * @param summonsJson   the JSON that was validated, used to report each offending value as-is
     */
    public static List<ComplaintsFileValidationError> map(final Throwable schemaFailure,
                                                          final List<ConvertedRow> convertedRows,
                                                          final String summonsJson) {
        final ValidationException validationException = findValidationException(schemaFailure);
        if (validationException == null) {
            return singletonList(new ComplaintsFileValidationError(FIRST_DATA_ROW, firstRowReference(convertedRows), null,
                    ComplaintsFileValidationError.INVALID_VALUE, null, "",
                    "Complaints file failed schema validation: " + schemaFailure.getMessage()));
        }

        final JSONObject validatedJson = new JSONObject(summonsJson);
        final Set<ComplaintsFileValidationError> errors = new LinkedHashSet<>();
        for (final ValidationException violation : leavesOf(validationException)) {
            errors.add(toError(violation, convertedRows, validatedJson));
        }
        return new ArrayList<>(errors);
    }

    private static ValidationException findValidationException(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ValidationException) {
                return (ValidationException) current;
            }
            if (current.getCause() == current) {
                return null;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * {@code oneOf}/{@code anyOf} failures are kept whole: their causing exceptions describe every
     * alternative that did not match, which would read as contradictory errors in the report.
     */
    private static List<ValidationException> leavesOf(final ValidationException violation) {
        final List<ValidationException> causes = violation.getCausingExceptions();
        if (causes == null || causes.isEmpty()
                || "oneOf".equals(violation.getKeyword()) || "anyOf".equals(violation.getKeyword())) {
            return singletonList(violation);
        }
        final List<ValidationException> leaves = new ArrayList<>();
        causes.forEach(cause -> leaves.addAll(leavesOf(cause)));
        return leaves;
    }

    private static ComplaintsFileValidationError toError(final ValidationException violation,
                                                         final List<ConvertedRow> convertedRows,
                                                         final JSONObject validatedJson) {
        final String errorMessage = violation.getErrorMessage();
        final List<String> path = new ArrayList<>(pathSegments(violation.getPointerToViolation()));

        final Matcher requiredKey = REQUIRED_KEY.matcher(errorMessage == null ? "" : errorMessage);
        final boolean isRequired = requiredKey.find();
        if (isRequired) {
            path.add(requiredKey.group(1));
        }

        final long rowNumber;
        final String caseReference;
        final String defendantReference;
        final String column;
        if (path.size() >= 2 && PROSECUTION_CASES.equals(path.get(0)) && isCaseIndex(path.get(1), convertedRows)) {
            final ConvertedRow row = convertedRows.get(Integer.parseInt(path.get(1)));
            rowNumber = row.getRowNumber();
            caseReference = row.getCaseReference();
            defendantReference = row.getDefendantReference();
            column = caseColumn(path.subList(2, path.size()));
        } else {
            // submission-level fields (hearingDetails.*, prosecutingAuthority) are read from the first data row
            rowNumber = FIRST_DATA_ROW;
            caseReference = firstRowReference(convertedRows);
            defendantReference = null;
            column = path.isEmpty() ? null : String.join(".", withoutIndexes(path));
        }

        final String location = "Row " + rowNumber + ": ";
        if (isRequired) {
            return new ComplaintsFileValidationError(rowNumber, caseReference, defendantReference,
                    ComplaintsFileValidationError.REQUIRED_FIELD_MISSING, column, "",
                    location + "required column '" + column + "' is blank");
        }
        return new ComplaintsFileValidationError(rowNumber, caseReference, defendantReference,
                ComplaintsFileValidationError.INVALID_VALUE, column,
                offendingValue(validatedJson, violation.getPointerToViolation()),
                location + (column == null ? "" : "'" + column + "' ") + errorMessage);
    }

    /**
     * The value at the violation's pointer, as a plain string - only scalar values are reported,
     * the way Prosecution Case File reports problem values.
     */
    private static String offendingValue(final JSONObject validatedJson, final String pointer) {
        if (pointer == null || "#".equals(pointer)) {
            return "";
        }
        try {
            final Object value = validatedJson.optQuery(pointer);
            if (value == null || value == JSONObject.NULL || value instanceof JSONObject || value instanceof JSONArray) {
                return "";
            }
            return value.toString();
        } catch (JSONException | IllegalArgumentException e) {
            return "";
        }
    }

    private static boolean isCaseIndex(final String segment, final List<ConvertedRow> convertedRows) {
        return NUMERIC.matcher(segment).matches() && Integer.parseInt(segment) < convertedRows.size();
    }

    /**
     * Maps a path inside one prosecution case onto the template's column names, e.g.
     * {@code defendants/0/defendantDetails/asn} to {@code defendant.asn} and
     * {@code defendants/0/offences/0/offenceDetails/laidDate} to {@code offence.offenceDetails.laidDate}.
     */
    private static String caseColumn(final List<String> casePath) {
        final List<String> segments = withoutIndexes(casePath);
        if (segments.isEmpty()) {
            return null;
        }
        if (!DEFENDANTS.equals(segments.get(0))) {
            return "case." + String.join(".", segments);
        }
        final List<String> defendantPath = segments.subList(1, segments.size());
        if (defendantPath.isEmpty()) {
            return "defendant";
        }
        if (OFFENCES.equals(defendantPath.get(0))) {
            final List<String> offencePath = defendantPath.subList(1, defendantPath.size());
            return offencePath.isEmpty() ? "offence" : "offence." + String.join(".", offencePath);
        }
        if (DEFENDANT_DETAILS.equals(defendantPath.get(0))) {
            final List<String> detailsPath = defendantPath.subList(1, defendantPath.size());
            return detailsPath.isEmpty() ? "defendant" : "defendant." + String.join(".", detailsPath);
        }
        return "defendant." + String.join(".", defendantPath);
    }

    private static List<String> pathSegments(final String pointer) {
        if (pointer == null) {
            return new ArrayList<>();
        }
        final String path = pointer.startsWith("#") ? pointer.substring(1) : pointer;
        final List<String> segments = new ArrayList<>();
        Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isEmpty())
                .forEach(segments::add);
        return segments;
    }

    private static List<String> withoutIndexes(final List<String> segments) {
        final List<String> named = new ArrayList<>();
        segments.stream()
                .filter(segment -> !NUMERIC.matcher(segment).matches())
                .forEach(named::add);
        return named;
    }

    private static String firstRowReference(final List<ConvertedRow> convertedRows) {
        return !convertedRows.isEmpty() && convertedRows.get(0).getRowNumber() == FIRST_DATA_ROW
                ? convertedRows.get(0).getCaseReference()
                : null;
    }
}
