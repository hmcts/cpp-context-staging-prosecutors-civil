package uk.gov.moj.cpp.staging.civil.handler.command.api.validators;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OffenceValidator {

    public static final String FIELD_OFFENCE_OFFENCE_SEQUENCE_NO = "defendant.offences.offenceSequenceNo";
    public static final String OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE = "The offences %s have same offence sequence numbers. Offence sequence numbers must be unique.";

    public Map<String, List<String>> validate(final List<OffenceDetails> offenceDetails, final Map<String, List<String>> validationErrors) {
        validateUniqueOffenceSequenceNumber(offenceDetails, validationErrors);

        return validationErrors;
    }

    private Map<String, List<String>> validateUniqueOffenceSequenceNumber(final List<OffenceDetails> offenceDetails, final Map<String, List<String>> validationErrors) {
        final String nonUniqueOffences = offenceDetails
                .stream()
                .collect(
                        groupingBy(OffenceDetails::getOffenceSequenceNo,
                                mapping(OffenceDetails::getCjsOffenceCode, toList())
                        )).entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getValue().toString())
                .collect(joining(", "));

        if (!nonUniqueOffences.isEmpty()) {
            addError(validationErrors, FIELD_OFFENCE_OFFENCE_SEQUENCE_NO, format(OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE, nonUniqueOffences));
        }

        return validationErrors;
    }

    private void addError(final Map<String, List<String>> errors, final String field, final String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
