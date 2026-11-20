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

    @Test
    public void shouldReportProsecutionCaseFileKeysAsTemplateColumnsForComplaintsFileUploads() {
        final JsonArray defendantErrors = createArrayBuilder()
                .add(problemWithKey("prosecutorDefendantReference", "D1", "OFFENCE_CODE_NOT_SUPPORTED", "offence_offenceCode", "AX03547"))
                .add(problemWithKey("prosecutorDefendantReference", "D1", "DEFENDANT_DOB_IN_FUTURE",
                        "individual_selfDefinedInformation_dateOfBirth", "2090-01-01"))
                .build();

        final String csv = SubmissionErrorDetailsCsvBuilder.build(null, defendantErrors, true);

        assertThat(csv, is("Reference,Error Type,Error Code,Field,Value\n"
                + "D1,Defendant,OFFENCE_CODE_NOT_SUPPORTED,offence.offenceDetails.cjsOffenceCode,AX03547\n"
                + "D1,Defendant,DEFENDANT_DOB_IN_FUTURE,defendant.individual.dateOfBirth,2090-01-01"));
    }

    @Test
    public void shouldReportAReusedKeyAgainstTheColumnItsProblemCodeRefersTo() {
        final JsonArray defendantErrors = createArrayBuilder()
                .add(problemWithKey("prosecutorDefendantReference", "D1", "DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE",
                        "dateOfBirth", "2090-01-01"))
                .add(problemWithKey("prosecutorDefendantReference", "D1", "DEFENDANT_DOB_IN_FUTURE", "dateOfBirth", "2091-01-01"))
                .build();

        assertThat(SubmissionErrorDetailsCsvBuilder.build(null, defendantErrors, true),
                is("Reference,Error Type,Error Code,Field,Value\n"
                        + "D1,Defendant,DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE,defendant.individual.parentGuardian.individual.dateOfBirth,2090-01-01\n"
                        + "D1,Defendant,DEFENDANT_DOB_IN_FUTURE,defendant.individual.dateOfBirth,2091-01-01"));
    }

    @Test
    public void shouldLeaveTemplateColumnKeysFromSchemaErrorsUnchanged() {
        final JsonArray defendantErrors = createArrayBuilder()
                .add(problemWithKey("prosecutorDefendantReference", "D1", "INVALID_DATE_FORMAT",
                        "defendant.individual.dateOfBirth", "31/02/1990"))
                .build();

        final String csv = SubmissionErrorDetailsCsvBuilder.build(null, defendantErrors, true);

        assertThat(csv, is("Reference,Error Type,Error Code,Field,Value\n"
                + "D1,Defendant,INVALID_DATE_FORMAT,defendant.individual.dateOfBirth,31/02/1990"));
    }

    @Test
    public void shouldKeepTheRawKeyWhenAProsecutionCaseFileKeyHasNoTemplateColumn() {
        final JsonArray caseErrors = createArrayBuilder()
                .add(problemWithKey("prosecutorCaseReference", "C1", "CASE_COUNT_EXCEEDED", "groupProsecutions.size", "101"))
                .build();

        final String csv = SubmissionErrorDetailsCsvBuilder.build(caseErrors, null, true);

        assertThat(csv, is("Reference,Error Type,Error Code,Field,Value\n"
                + "C1,Case,CASE_COUNT_EXCEEDED,groupProsecutions.size,101"));
    }

    @Test
    public void shouldKeepRawKeysForSubmissionsThatDidNotComeFromAComplaintsFile() {
        final JsonArray defendantErrors = createArrayBuilder()
                .add(problemWithKey("prosecutorDefendantReference", "D1", "OFFENCE_CODE_NOT_SUPPORTED", "offence_offenceCode", "AX03547"))
                .build();

        assertThat(SubmissionErrorDetailsCsvBuilder.build(null, defendantErrors, false),
                is("Reference,Error Type,Error Code,Field,Value\n"
                        + "D1,Defendant,OFFENCE_CODE_NOT_SUPPORTED,offence_offenceCode,AX03547"));
        assertThat(SubmissionErrorDetailsCsvBuilder.build(null, defendantErrors),
                is(SubmissionErrorDetailsCsvBuilder.build(null, defendantErrors, false)));
    }

    @Test
    public void shouldLeaveTheReferenceBlankWhenTheErrorHasNone() {
        final JsonArray defendantErrors = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("problems", createArrayBuilder()
                                .add(createObjectBuilder().add("code", "REQUIRED_FIELD_MISSING")
                                        .add("values", createArrayBuilder().add(createObjectBuilder()
                                                .add("key", "defendant.prosecutorDefendantId").add("value", ""))))))
                .build();

        assertThat(SubmissionErrorDetailsCsvBuilder.build(null, defendantErrors, true),
                is("Reference,Error Type,Error Code,Field,Value\n"
                        + ",Defendant,REQUIRED_FIELD_MISSING,defendant.prosecutorDefendantId,"));
    }

    private static javax.json.JsonObjectBuilder problemWithKey(final String referenceName, final String reference,
                                                              final String code, final String key, final String value) {
        return createObjectBuilder()
                .add(referenceName, reference)
                .add("problems", createArrayBuilder()
                        .add(createObjectBuilder().add("code", code)
                                .add("values", createArrayBuilder().add(createObjectBuilder().add("key", key).add("value", value)))));
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
