package uk.gov.moj.cpp.staging.civil.handler.command.api.validators;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails.offenceDetails;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class OffenceValidatorTest {

    private final OffenceValidator offenceValidator = new OffenceValidator();

    @Test
    public void shouldFailValidationWhenOffenceSequenceNumbersAreNotUnique() {
        final Map<String, List<String>> actualValidations = offenceValidator.validate(nonUniqueOffenceSequenceNumbers(), new HashMap<>());

        final Map<String, List<String>> expectedViolations = new HashMap<>();
        expectedViolations.put("defendant.offences.offenceSequenceNo",
                singletonList("The offences [CA03013, CA03014] have same offence sequence numbers. Offence sequence numbers must be unique."));

        assertThat(actualValidations, equalTo(expectedViolations));
    }

    @Test
    public void shouldPassValidationWhenOffenceSequenceNumbersAreUnique() {
        final Map<String, List<String>> actualValidations = offenceValidator.validate(uniqueOffenceSequenceNumbers(), new HashMap<>());

        assertThat(actualValidations, equalTo(Collections.EMPTY_MAP));
    }

    private List<OffenceDetails> nonUniqueOffenceSequenceNumbers() {
        final OffenceDetails offenceDetails1 = offenceDetails().withCjsOffenceCode("CA03013").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails2 = offenceDetails().withCjsOffenceCode("CA03014").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails3 = offenceDetails().withCjsOffenceCode("CA03015").withOffenceSequenceNo(2).build();
        return asList(offenceDetails1, offenceDetails2, offenceDetails3);
    }

    private List<OffenceDetails> uniqueOffenceSequenceNumbers() {
        final OffenceDetails offenceDetails1 = offenceDetails().withCjsOffenceCode("CA03013").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails2 = offenceDetails().withCjsOffenceCode("CA03014").withOffenceSequenceNo(2).build();
        return asList(offenceDetails1, offenceDetails2);
    }
}
