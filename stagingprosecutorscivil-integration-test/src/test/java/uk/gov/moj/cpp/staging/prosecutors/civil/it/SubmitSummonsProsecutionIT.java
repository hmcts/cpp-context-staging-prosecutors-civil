package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.SUMMONS_PROSECUTION_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.submitSummonsProsecutionStatus;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.ProsecutionCaseFileApi;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.model.Submission;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.UrlResponse;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SubmitSummonsProsecutionIT {

    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED         = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED                     = "public.prosecutioncasefile.civil-prosecution-rejected";
    private static final String PUBLIC_EVENT_PCF_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @BeforeEach
    public void setUpStub() {
        new WiremockUtils()
                .stubPing("prosecutioncasefile")
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-group-prosecution")
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cc-prosecution")
                .stubIdMapperRecordingNewAssociation();
        stubAddMany();
        setupLoggedInUsersPermissionQueryStub(randomUUID().toString());
        stubPCFCommand(randomUUID());
    }

    @Test
    public void shouldSubmitSummonsProsecution() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSummonsProsecution(payload);
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
        assertThat(submission.getType(), Matchers.is("PROSECUTION"));
        assertThat(submission.getReceivedAt(), Matchers.notNullValue());
        assertThat(submission.getCompletedAt(), Matchers.nullValue());

        JsonObject caseSucceededPublicEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, randomUUID().toString()), caseSucceededPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, publicEventEnvelope);

        final Submission submission2 = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        Assert.assertThat(submission2.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
        assertThat(submission2.getCompletedAt(), Matchers.notNullValue());
    }

    @Test
    public void shouldUpdateStatusToRejectedForSummonsProsecution() {
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution("payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        JsonObject rejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", createArrayBuilder().build())
                .add("defendantErrors", createArrayBuilder().build())
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.REJECTED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldSubmitSummonsProsecutionWithRelatedReferenceNumber() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-with-related-reference.json";
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSummonsProsecution(payload);
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldSubmitSummonsProsecutionForYouthDefendantWithIndividualParentGuardian() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-youth-individual-guardian.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));

        JsonObject caseSucceededPublicEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, randomUUID().toString()), caseSucceededPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, publicEventEnvelope);

        final Submission submission2 = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        assertThat(submission2.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldRejectSummonsProsecutionWhenSummonsCodeAbsent() {
        int status = submitSummonsProsecutionStatus(
                "payload/summons/stagingprosecutors.submit-summons-prosecution-missing-summons-code.json",
                SUMMONS_PROSECUTION_CONTENT_TYPE);
        assertThat(status, Matchers.is(400));
    }

    @Disabled("Works locally but fails in pipeline")
    @Test
    public void shouldUpdateStatusToSuccessWithWarningsForSummonsProsecution() {
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution("payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        JsonObject warningsEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS, randomUUID().toString()), warningsEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS_WITH_WARNINGS);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }
}
