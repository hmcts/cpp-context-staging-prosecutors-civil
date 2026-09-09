package uk.gov.moj.cpp.staging.civil.query;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonArray;

import org.junit.jupiter.api.Test;

public class SubmissionErrorDetailsCsvBuilderTest {

    @Test
    public void shouldReturnHeaderOnlyWhenBothArraysAreNull() {
        assertThat(SubmissionErrorDetailsCsvBuilder.build(null, null), is("Reference,Error Type,Error Code,Field,Value"));
    }

    @Test
    public void shouldReturnHeaderOnlyWhenBothArraysAreEmpty() {
        final JsonArray empty = createArrayBuilder().build();
        assertThat(SubmissionErrorDetailsCsvBuilder.build(empty, empty), is("Reference,Error Type,Error Code,Field,Value"));
    }

    @Test
    public void shouldEmitOneRowWithEmptyFieldAndValueWhenProblemHasNoValues() {
        final JsonArray caseErrors = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("prosecutorCaseReference", "REF1")
                        .add("problems", createArrayBuilder()
                                .add(createObjectBuilder().add("code", "NO_VALUES_CODE"))))
                .build();

        final String csv = SubmissionErrorDetailsCsvBuilder.build(caseErrors, null);

        assertThat(csv, is("Reference,Error Type,Error Code,Field,Value\nREF1,Case,NO_VALUES_CODE,,"));
    }

    @Test
    public void shouldPreserveOrderOfCasesBeforeDefendants() {
        final JsonArray caseErrors = createArrayBuilder()
                .add(caseError("C1", "CASE_CODE"))
                .build();
        final JsonArray defendantErrors = createArrayBuilder()
                .add(defendantError("D1", "DEF_CODE"))
                .build();

        final String csv = SubmissionErrorDetailsCsvBuilder.build(caseErrors, defendantErrors);

        assertThat(csv, is("Reference,Error Type,Error Code,Field,Value\n"
                + "C1,Case,CASE_CODE,f,v\n"
                + "D1,Defendant,DEF_CODE,f,v"));
    }

    private static javax.json.JsonObjectBuilder caseError(final String reference, final String code) {
        return createObjectBuilder()
                .add("prosecutorCaseReference", reference)
                .add("problems", createArrayBuilder()
                        .add(createObjectBuilder().add("code", code)
                                .add("values", createArrayBuilder().add(createObjectBuilder().add("key", "f").add("value", "v")))));
    }

    private static javax.json.JsonObjectBuilder defendantError(final String reference, final String code) {
        return createObjectBuilder()
                .add("prosecutorDefendantReference", reference)
                .add("problems", createArrayBuilder()
                        .add(createObjectBuilder().add("code", code)
                                .add("values", createArrayBuilder().add(createObjectBuilder().add("key", "f").add("value", "v")))));
    }
}
