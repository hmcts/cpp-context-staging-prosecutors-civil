package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

import static java.util.Collections.unmodifiableList;

import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;

import java.util.List;
import java.util.Optional;

/**
 * Outcome of converting an uploaded complaints CSV file.
 * <p>
 * {@link #getSummons()} holds only the rows that converted cleanly, in file order;
 * {@link #getConvertedRows()} lines up index-for-index with {@code summons.prosecutionCases} so a
 * schema violation reported against {@code #/prosecutionCases/<n>} can be traced back to its CSV row
 * and case reference. {@link #getErrors()} holds every row-level conversion problem found.
 */
public final class ComplaintsFileConversionResult {

    private final String prosecutingAuthority;
    private final Summons summons;
    private final List<ConvertedRow> convertedRows;
    private final List<ComplaintsFileValidationError> errors;

    public ComplaintsFileConversionResult(final String prosecutingAuthority,
                                          final Summons summons,
                                          final List<ConvertedRow> convertedRows,
                                          final List<ComplaintsFileValidationError> errors) {
        this.prosecutingAuthority = prosecutingAuthority;
        this.summons = summons;
        this.convertedRows = unmodifiableList(convertedRows);
        this.errors = unmodifiableList(errors);
    }

    /**
     * Always present - a file without a {@code prosecutingAuthority} on its first data row cannot be
     * attributed to a prosecutor organisation and is rejected outright during conversion.
     */
    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    /**
     * Empty when no row converted cleanly. Hearing details that could not be read are left blank
     * here - their problems are already in {@link #getErrors()}.
     */
    public Optional<Summons> getSummons() {
        return Optional.ofNullable(summons);
    }

    public List<ConvertedRow> getConvertedRows() {
        return convertedRows;
    }

    public List<ComplaintsFileValidationError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * The CSV row number, case reference and defendant reference behind one entry of
     * {@code summons.prosecutionCases}. Either reference is {@code null} when the row has none.
     */
    public static final class ConvertedRow {
        private final long rowNumber;
        private final String caseReference;
        private final String defendantReference;

        public ConvertedRow(final long rowNumber, final String caseReference, final String defendantReference) {
            this.rowNumber = rowNumber;
            this.caseReference = caseReference;
            this.defendantReference = defendantReference;
        }

        public long getRowNumber() {
            return rowNumber;
        }

        public String getCaseReference() {
            return caseReference;
        }

        public String getDefendantReference() {
            return defendantReference;
        }
    }
}
