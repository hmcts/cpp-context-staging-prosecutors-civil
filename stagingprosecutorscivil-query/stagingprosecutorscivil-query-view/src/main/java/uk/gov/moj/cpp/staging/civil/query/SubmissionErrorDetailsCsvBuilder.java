package uk.gov.moj.cpp.staging.civil.query;

import static java.util.Objects.isNull;

import uk.gov.moj.cpp.staging.civil.common.csv.ProsecutionCaseFileFieldToCsvColumn;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Builds the submission error report CSV ({@code Reference,Error Type,Error Code,Field,Value}).
 * <p>
 * For a complaints-file (CSV upload) submission the {@code Field} column names the template column
 * the user has to correct: Prosecution Case File business-error keys (e.g.
 * {@code individual_selfDefinedInformation_dateOfBirth}) are translated through
 * {@link ProsecutionCaseFileFieldToCsvColumn}; keys that are already column names (stagingcivil
 * schema errors) or have no template column are written unchanged. Direct JSON API submissions keep
 * the raw keys. Stored data is never changed - translation happens only here.
 */
public final class SubmissionErrorDetailsCsvBuilder {

    private static final String HEADER = "Reference,Error Type,Error Code,Field,Value";
    private static final String LINE_SEPARATOR = "\n";

    private SubmissionErrorDetailsCsvBuilder() {
    }

    public static String build(final JsonArray caseErrors, final JsonArray defendantErrors) {
        return build(caseErrors, defendantErrors, false);
    }

    /**
     * @param useCsvColumnNames {@code true} for a complaints-file upload, to report Prosecution Case
     *                          File keys as template column names
     */
    public static String build(final JsonArray caseErrors, final JsonArray defendantErrors,
                               final boolean useCsvColumnNames) {
        final StringBuilder csv = new StringBuilder(HEADER);
        appendRows(csv, caseErrors, "Case", "prosecutorCaseReference", useCsvColumnNames);
        appendRows(csv, defendantErrors, "Defendant", "prosecutorDefendantReference", useCsvColumnNames);
        return csv.toString();
    }

    private static void appendRows(final StringBuilder csv, final JsonArray errors, final String errorType,
                                    final String referenceKey, final boolean useCsvColumnNames) {
        if (isNull(errors)) {
            return;
        }

        for (final JsonValue errorValue : errors) {
            final JsonObject error = errorValue.asJsonObject();
            final String reference = error.getString(referenceKey, "");
            final JsonArray problems = error.getJsonArray("problems");
            appendProblems(csv, problems, reference, errorType, useCsvColumnNames);
        }
    }

    private static void appendProblems(final StringBuilder csv, final JsonArray problems, final String reference,
                                        final String errorType, final boolean useCsvColumnNames) {
        if (isNull(problems)) {
            return;
        }

        for (final JsonValue problemValue : problems) {
            final JsonObject problem = problemValue.asJsonObject();
            final String code = problem.getString("code", "");
            final JsonArray values = problem.getJsonArray("values");

            if (isNull(values) || values.isEmpty()) {
                appendRow(csv, reference, errorType, code, "", "");
            } else {
                for (final JsonValue value : values) {
                    final JsonObject valueEntry = value.asJsonObject();
                    final String key = asPlainString(valueEntry.get("key"));
                    appendRow(csv, reference, errorType, code,
                            useCsvColumnNames ? toCsvColumn(code, key) : key, asPlainString(valueEntry.get("value")));
                }
            }
        }
    }

    private static String toCsvColumn(final String code, final String key) {
        return ProsecutionCaseFileFieldToCsvColumn.toCsvColumn(code, key).orElse(key);
    }

    private static void appendRow(final StringBuilder csv, final String... fields) {
        csv.append(LINE_SEPARATOR);
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(fields[i]));
        }
    }

    private static String asPlainString(final JsonValue value) {
        if (isNull(value) || value.getValueType() == JsonValue.ValueType.NULL) {
            return "";
        }
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        return value.toString();
    }

    private static String escape(final String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
