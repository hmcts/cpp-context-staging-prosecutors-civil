package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

import java.util.Objects;

/**
 * A single schema-level problem found in one row of an uploaded complaints CSV file - either while
 * converting the row (blank required column, malformed date, unknown enum value, duplicate case URN)
 * or while validating the converted submission against the {@code stagingcivil.summons} JSON schema.
 * <p>
 * These are reported back to the prosecutor through the submission's error report rather than as a
 * synchronous HTTP 400, so each error carries the case reference the prosecutor will recognise.
 */
public final class ComplaintsFileValidationError {

    public static final String REQUIRED_FIELD_MISSING = "REQUIRED_FIELD_MISSING";
    public static final String INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";
    public static final String INVALID_VALUE = "INVALID_VALUE";
    public static final String DUPLICATE_CASE_URN = "DUPLICATE_CASE_URN";
    public static final String INVALID_ROW = "INVALID_ROW";

    private final long rowNumber;
    private final String caseReference;
    private final String code;
    private final String field;
    private final String value;
    private final String message;

    public ComplaintsFileValidationError(final long rowNumber, final String caseReference, final String code,
                                         final String field, final String value, final String message) {
        this.rowNumber = rowNumber;
        this.caseReference = caseReference;
        this.code = code;
        this.field = field;
        this.value = value == null ? "" : value;
        this.message = message;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    /**
     * The row's {@code case.urn} - the prosecutor's own reference for the case - or, when the row has
     * no usable URN, a {@code "Row <n>"} label so the error can still be located in the file.
     */
    public String getCaseReference() {
        return caseReference;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    /**
     * The offending value exactly as found - an empty string when the value is missing - matching
     * how Prosecution Case File reports business-validation problem values.
     */
    public String getValue() {
        return value;
    }

    /**
     * Human-readable description, used for logging and for the synchronous 400 raised by strict
     * conversion; it is not stored in the error report.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ComplaintsFileValidationError that = (ComplaintsFileValidationError) o;
        return rowNumber == that.rowNumber
                && Objects.equals(caseReference, that.caseReference)
                && Objects.equals(code, that.code)
                && Objects.equals(field, that.field)
                && Objects.equals(value, that.value)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowNumber, caseReference, code, field, value, message);
    }

    @Override
    public String toString() {
        return "ComplaintsFileValidationError{rowNumber=" + rowNumber + ", caseReference='" + caseReference
                + "', code='" + code + "', field='" + field + "', value='" + value + "', message='" + message + "'}";
    }
}
