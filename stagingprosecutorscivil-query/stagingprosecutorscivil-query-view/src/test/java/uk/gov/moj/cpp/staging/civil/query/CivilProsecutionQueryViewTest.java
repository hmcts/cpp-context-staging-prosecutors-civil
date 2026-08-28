package uk.gov.moj.cpp.staging.civil.query;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.entity.SubmissionType;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilProsecutionQueryViewTest {

    @InjectMocks
    private CivilProsecutionQueryView civilProsecutionQueryView;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private SubmissionRepository submissionRepository;

    @Test
    public void shouldReturnSubmissionDetails() {

        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final UUID submissionId = UUID.randomUUID();

        final Set<CaseDetail> caseDetails = new HashSet<>();
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(UUID.randomUUID());
        caseDetail.setCaseUrn("CASEURN");
        caseDetails.add(caseDetail);

        final JsonArray errors = createArrayBuilder().add("error1").build();
        final JsonArray warnings = createArrayBuilder().add("warning1").build();
        final JsonArray caseWarnings = createArrayBuilder().add("casewarning1").build();
        final JsonArray defendantWarnings = createArrayBuilder().add("defendantWarning1").build();

        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "ouCode",
                errors,
                warnings,
                caseWarnings,
                defendantWarnings,
                receivedAt,
                completedAt,
                caseDetails,
                SubmissionType.PROSECUTION,
                "summons-batch.csv",
                "Richard Chapman",
                "CPS");

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutorscivil.query.submission-details", payload);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), Is.is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), Is.is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), Is.is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("materialErrors"), Is.is(errors));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("materialWarnings"), Is.is(warnings));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("caseWarnings"), Is.is(caseWarnings));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("defendantWarnings"), Is.is(defendantWarnings));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), Is.is(SubmissionType.PROSECUTION.name()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), Is.is(ZonedDateTimes.toString(receivedAt)));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), Is.is(ZonedDateTimes.toString(completedAt)));
        // additionalInfo was not requested, so fileName/username/prosecutingAuthority are omitted
        // even though they are present on the Submission - restores pre-existing response shape.
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("fileName"), Is.is(false));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("username"), Is.is(false));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("prosecutingAuthority"), Is.is(false));

        // the pre-rename key names must no longer appear on the response
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("errors"), Is.is(false));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("warnings"), Is.is(false));
    }

    @Test
    public void shouldReturnCaseErrorsAndDefendantErrorsFromTheViewstore() {

        final UUID submissionId = UUID.randomUUID();

        final JsonArray caseErrors = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("prosecutorCaseReference", "URN01")
                        .add("problems", createArrayBuilder().add(createObjectBuilder().add("code", "CASE_ERR"))))
                .build();
        final JsonArray defendantErrors = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("prosecutorDefendantReference", "URN01-D1")
                        .add("problems", createArrayBuilder().add(createObjectBuilder().add("code", "DEF_ERR"))))
                .build();

        final Submission submission = new Submission(
                submissionId,
                "REJECTED",
                "ouCode",
                null,
                null,
                null,
                null,
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                new HashSet<>(),
                SubmissionType.PROSECUTION,
                null,
                null,
                null);
        submission.setGroupCaseErrors(caseErrors);
        submission.setDefendantErrors(defendantErrors);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(
                createEnvelope("stagingprosecutorscivil.query.submission-details",
                        createObjectBuilder().add("submissionId", submissionId.toString()).build()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("caseErrors"), Is.is(caseErrors));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("defendantErrors"), Is.is(defendantErrors));
    }

    @Test
    public void shouldReturnEmptyArraysForEveryProblemAttributeWhenTheViewstoreHoldsNone() {

        final UUID submissionId = UUID.randomUUID();

        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "ouCode",
                null,
                null,
                null,
                null,
                ZonedDateTime.now(),
                null,
                new HashSet<>(),
                SubmissionType.PROSECUTION,
                null,
                null,
                null);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(
                createEnvelope("stagingprosecutorscivil.query.submission-details",
                        createObjectBuilder().add("submissionId", submissionId.toString()).build()));

        final JsonObject response = jsonEnvelope.payloadAsJsonObject();
        final JsonArray empty = createArrayBuilder().build();

        // required-with-empty-array semantics: the key is always present so consumers never
        // have to distinguish "absent" from "nothing to report"
        for (final String attribute : new String[]{"materialErrors", "materialWarnings", "caseErrors",
                "defendantErrors", "caseWarnings", "defendantWarnings"}) {
            assertThat(response.containsKey(attribute), Is.is(true));
            assertThat(response.getJsonArray(attribute), Is.is(empty));
        }
    }

    @Test
    public void shouldReturnFileNameUsernameAndProsecutingAuthorityWhenAdditionalInfoRequested() {

        final UUID submissionId = UUID.randomUUID();

        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "ouCode",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                null,
                null,
                null,
                null,
                new HashSet<>(),
                null,
                "summons-batch.csv",
                "Richard Chapman",
                "CPS");

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("additionalInfo", true)
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutorscivil.query.submission-details", payload);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(requestEnvelope);

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("fileName"), Is.is("summons-batch.csv"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("username"), Is.is("Richard Chapman"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("prosecutingAuthority"), Is.is("CPS"));
    }

    @Test
    public void shouldOmitFileNameUsernameAndProsecutingAuthorityWhenNotPresentEvenIfAdditionalInfoRequested() {

        final UUID submissionId = UUID.randomUUID();

        final JsonArray errors = createArrayBuilder().build();
        final JsonArray warnings = createArrayBuilder().build();

        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "ouCode",
                errors,
                warnings,
                null,
                null,
                null,
                null,
                new HashSet<>(),
                null,
                null,
                null,
                null);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("additionalInfo", true)
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutorscivil.query.submission-details", payload);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(requestEnvelope);

        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("fileName"), Is.is(false));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("username"), Is.is(false));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("prosecutingAuthority"), Is.is(false));
    }

    @Test
    public void shouldOmitCompletedAtAndTypeWhenNotPresent() {

        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final UUID submissionId = UUID.randomUUID();

        final JsonArray errors = createArrayBuilder().build();
        final JsonArray warnings = createArrayBuilder().build();

        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "ouCode",
                errors,
                warnings,
                null,
                null,
                receivedAt,
                null,
                new HashSet<>(),
                null,
                null,
                null,
                null);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutorscivil.query.submission-details", payload);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(requestEnvelope);

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), Is.is(ZonedDateTimes.toString(receivedAt)));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("completedAt"), Is.is(false));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("type"), Is.is(false));
    }

    @Test
    public void shouldOmitReceivedAtWhenNotPresent() {

        final UUID submissionId = UUID.randomUUID();

        final JsonArray errors = createArrayBuilder().build();
        final JsonArray warnings = createArrayBuilder().build();

        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "ouCode",
                errors,
                warnings,
                null,
                null,
                null,
                null,
                new HashSet<>(),
                null,
                null,
                null,
                null);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutorscivil.query.submission-details", payload);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(requestEnvelope);

        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("receivedAt"), Is.is(false));
    }

    @Test
    public void shouldReturnNullPayloadWhenSubmissionNotFound() {
        when(submissionRepository.findBy(any())).thenReturn(null);

        final JsonEnvelope responseEnvelope = civilProsecutionQueryView
                .querySubmission(createEnvelope("stagingprosecutorscivil.query.submission-details",
                        createObjectBuilder()
                                .add("submissionId", UUID.randomUUID().toString())
                                .build())
                );

        assertThat(responseEnvelope.metadata().name(), Is.is("stagingprosecutorscivil.query.submission-details"));
        assertEquals(JsonValue.NULL, responseEnvelope.payload());
    }

    @Test
    public void shouldBuildCsvFromCaseAndDefendantErrors() {

        final UUID submissionId = UUID.randomUUID();

        final JsonArray caseErrors = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("prosecutorCaseReference", "123")
                        .add("problems", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("code", "PROSECUTOR_OUCODE_NOT_RECOGNISED")
                                        .add("values", createArrayBuilder()
                                                .add(createObjectBuilder().add("key", "prosecutingAuthority").add("value", "A010000"))))
                                .add(createObjectBuilder()
                                        .add("code", "CASE_MARKER_IS_INVALID")
                                        .add("values", createArrayBuilder()
                                                .add(createObjectBuilder().add("key", "caseMarkers").add("value", "MC"))))))
                .build();
        final JsonArray defendantErrors = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("prosecutorDefendantReference", "cad5a01")
                        .add("problems", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("code", "OFFENCE_CODE_NOT_SUPPORTED")
                                        .add("values", createArrayBuilder()
                                                .add(createObjectBuilder().add("key", "offence_offenceCode").add("value", "AX03547"))
                                                .add(createObjectBuilder().add("key", "offence_offenceSequenceNo").add("value", "1"))))
                                .add(createObjectBuilder()
                                        .add("code", "DATE_OF_HEARING_IN_THE_PAST")
                                        .add("values", createArrayBuilder()
                                                .add(createObjectBuilder().add("key", "initialHearing_dateOfHearing").add("value", "2026-05-05"))))))
                .build();

        final Submission submission = new Submission(
                submissionId, "REJECTED", "ouCode", null, null, null, null,
                ZonedDateTime.now(), ZonedDateTime.now(), new HashSet<>(),
                SubmissionType.PROSECUTION, null, null, null);
        submission.setGroupCaseErrors(caseErrors);
        submission.setDefendantErrors(defendantErrors);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmissionErrorDetailsCsv(
                createEnvelope("stagingprosecutorscivil.query.submission-error-details-csv",
                        createObjectBuilder().add("submissionId", submissionId.toString()).build()));

        assertThat(jsonEnvelope.metadata().name(), Is.is("stagingprosecutorscivil.query.submission-error-details-csv"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("submissionId"), Is.is(submissionId.toString()));

        final String expectedCsv = "Reference,Error Type,Error Code,Field,Value\n"
                + "123,Case,PROSECUTOR_OUCODE_NOT_RECOGNISED,prosecutingAuthority,A010000\n"
                + "123,Case,CASE_MARKER_IS_INVALID,caseMarkers,MC\n"
                + "cad5a01,Defendant,OFFENCE_CODE_NOT_SUPPORTED,offence_offenceCode,AX03547\n"
                + "cad5a01,Defendant,OFFENCE_CODE_NOT_SUPPORTED,offence_offenceSequenceNo,1\n"
                + "cad5a01,Defendant,DATE_OF_HEARING_IN_THE_PAST,initialHearing_dateOfHearing,2026-05-05";

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("csv"), Is.is(expectedCsv));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("fileName"), Is.is(false));
    }

    @Test
    public void shouldIncludeFileNameInPayloadWhenSubmissionHasOne() {

        final UUID submissionId = UUID.randomUUID();

        final Submission submission = new Submission(
                submissionId, "REJECTED", "ouCode", null, null, null, null,
                ZonedDateTime.now(), ZonedDateTime.now(), new HashSet<>(),
                SubmissionType.PROSECUTION, "complaints-2026-01-01.csv", null, null);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmissionErrorDetailsCsv(
                createEnvelope("stagingprosecutorscivil.query.submission-error-details-csv",
                        createObjectBuilder().add("submissionId", submissionId.toString()).build()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("fileName"), Is.is("complaints-2026-01-01.csv"));
    }

    @Test
    public void shouldReturnHeaderOnlyCsvWhenNoCaseOrDefendantErrors() {

        final UUID submissionId = UUID.randomUUID();

        final Submission submission = new Submission(
                submissionId, "PENDING", "ouCode", null, null, null, null,
                ZonedDateTime.now(), null, new HashSet<>(),
                SubmissionType.PROSECUTION, null, null, null);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmissionErrorDetailsCsv(
                createEnvelope("stagingprosecutorscivil.query.submission-error-details-csv",
                        createObjectBuilder().add("submissionId", submissionId.toString()).build()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("csv"), Is.is("Reference,Error Type,Error Code,Field,Value"));
    }

    @Test
    public void shouldReturnHeaderOnlyCsvWhenSubmissionNotFound() {
        when(submissionRepository.findBy(any())).thenReturn(null);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmissionErrorDetailsCsv(
                createEnvelope("stagingprosecutorscivil.query.submission-error-details-csv",
                        createObjectBuilder().add("submissionId", UUID.randomUUID().toString()).build()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("csv"), Is.is("Reference,Error Type,Error Code,Field,Value"));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("fileName"), Is.is(false));
    }

    @Test
    public void shouldEscapeCsvFieldsContainingCommasOrQuotes() {

        final UUID submissionId = UUID.randomUUID();

        final JsonArray caseErrors = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("prosecutorCaseReference", "ref,with,commas")
                        .add("problems", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("code", "SOME_CODE")
                                        .add("values", createArrayBuilder()
                                                .add(createObjectBuilder().add("key", "field").add("value", "has \"quotes\" in it"))))))
                .build();

        final Submission submission = new Submission(
                submissionId, "REJECTED", "ouCode", null, null, null, null,
                ZonedDateTime.now(), ZonedDateTime.now(), new HashSet<>(),
                SubmissionType.PROSECUTION, null, null, null);
        submission.setGroupCaseErrors(caseErrors);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmissionErrorDetailsCsv(
                createEnvelope("stagingprosecutorscivil.query.submission-error-details-csv",
                        createObjectBuilder().add("submissionId", submissionId.toString()).build()));

        final String expectedCsv = "Reference,Error Type,Error Code,Field,Value\n"
                + "\"ref,with,commas\",Case,SOME_CODE,field,\"has \"\"quotes\"\" in it\"";

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("csv"), Is.is(expectedCsv));
    }
}