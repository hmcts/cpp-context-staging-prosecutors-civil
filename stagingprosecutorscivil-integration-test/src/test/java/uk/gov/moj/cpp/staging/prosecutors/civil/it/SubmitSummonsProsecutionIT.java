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
import org.junit.jupiter.api.Test;

public class SubmitSummonsProsecutionIT {

    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED         = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED                     = "public.prosecutioncasefile.civil-prosecution-rejected";
    private static final String PUBLIC_EVENT_PCF_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";
    private static final String PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL        = "public.prosecutioncasefile.parked-for-summons-application-approval";
    private static final String PUBLIC_EVENT_PCF_SUBMISSION_APPROVED                            = "public.prosecutioncasefile.submission-approved";
    private static final String PUBLIC_EVENT_PCF_SUBMISSION_REJECTED                            = "public.prosecutioncasefile.submission-rejected";
    private static final String PUBLIC_EVENT_PCF_GROUP_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL  = "public.prosecutioncasefile.group-parked-for-summons-application-approval";
    private static final String PUBLIC_EVENT_PCF_GROUP_SUBMISSION_APPROVED                      = "public.prosecutioncasefile.group-submission-approved";
    private static final String PUBLIC_EVENT_PCF_GROUP_SUBMISSION_REJECTED                      = "public.prosecutioncasefile.group-submission-rejected";
    private static final String PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED                     = "public.prosecutioncasefile.group-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED                     = "public.prosecutioncasefile.group-prosecution-rejected";

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

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.FAILED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldUpdateStatusToPendingCourtDecisionWhenParkedForSummonsApplicationApproval() {
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution("payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final UUID applicationId = randomUUID();
        JsonObject parkedForApprovalEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("applicationId", applicationId.toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), parkedForApprovalEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.PENDING_COURT_DECISION);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
        assertThat(submission.getSummonsApplicationId(), Matchers.is(applicationId));
    }

    @Test
    public void shouldTransitionFromPendingCourtDecisionToAcceptedWhenSubmissionApproved() {
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution("payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject parkedForApprovalEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("applicationId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), parkedForApprovalEvent));
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING_COURT_DECISION);

        final JsonObject submissionApprovedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_SUBMISSION_APPROVED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_SUBMISSION_APPROVED, randomUUID().toString()), submissionApprovedEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.ACCEPTED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldNotOverwriteFailedWithRejectedWhenSubmissionWasNeverParkedForApproval() {
        // Reproduces PCF's real behaviour: civil-prosecution-rejected and submission-rejected fire
        // from the same trigger for every single-case CIVIL rejection, not only ones rejected via
        // SA court decision after parking. A submission that never went through
        // PENDING_COURT_DECISION must stay FAILED, not be overwritten with REJECTED by the paired
        // event that PCF sends immediately afterwards.
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution("payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject rejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", createArrayBuilder().build())
                .add("defendantErrors", createArrayBuilder().build())
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.FAILED);

        final JsonObject submissionRejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_SUBMISSION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_SUBMISSION_REJECTED, randomUUID().toString()), submissionRejectedEvent));

        // Still FAILED - the guarded REJECTED transition never applied since current status was
        // never PENDING_COURT_DECISION.
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.FAILED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldTransitionFromPendingCourtDecisionToRejectedWhenSubmissionRejected() {
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution("payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject parkedForApprovalEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("applicationId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), parkedForApprovalEvent));
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING_COURT_DECISION);

        final JsonObject submissionRejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_SUBMISSION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_SUBMISSION_REJECTED, randomUUID().toString()), submissionRejectedEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.REJECTED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldNotDowngradeSuccessWhenSubmissionApprovedArrivesAfterSuccess() {
        // Reproduces PCF's real behaviour: civil.prosecution-submission-succeeded and
        // submission-approved fire from the same trigger for every successful CIVIL case
        // creation, not only ones approved via SA court decision after parking. A submission
        // that already reached SUCCESS must stay SUCCESS, not be downgraded to ACCEPTED by the
        // paired event that PCF sends immediately afterwards.
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution("payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject caseSucceededPublicEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, randomUUID().toString()), caseSucceededPublicEvent));
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);

        final JsonObject submissionApprovedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_SUBMISSION_APPROVED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_SUBMISSION_APPROVED, randomUUID().toString()), submissionApprovedEvent));

        // Still SUCCESS - the guarded ACCEPTED transition never applied since current status was
        // never PENDING_COURT_DECISION.
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldUpdateStatusToPendingCourtDecisionForGroupSummonsProsecution() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-group.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final UUID applicationId = randomUUID();
        final JsonObject groupParkedForApprovalEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("applicationId", applicationId.toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), groupParkedForApprovalEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.PENDING_COURT_DECISION);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
        assertThat(submission.getSummonsApplicationId(), Matchers.is(applicationId));
    }

    @Test
    public void shouldTransitionFromPendingCourtDecisionToAcceptedForGroupSummonsProsecution() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-group.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject groupParkedForApprovalEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("applicationId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), groupParkedForApprovalEvent));
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING_COURT_DECISION);

        final JsonObject groupSubmissionApprovedEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_SUBMISSION_APPROVED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_SUBMISSION_APPROVED, randomUUID().toString()), groupSubmissionApprovedEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.ACCEPTED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldTransitionFromPendingCourtDecisionToRejectedForGroupSummonsProsecution() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-group.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject groupParkedForApprovalEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("applicationId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), groupParkedForApprovalEvent));
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING_COURT_DECISION);

        final JsonObject groupSubmissionRejectedEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_SUBMISSION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_SUBMISSION_REJECTED, randomUUID().toString()), groupSubmissionRejectedEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.REJECTED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldSubmitGroupSummonsProsecutionSuccessfully() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-group.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject groupSucceededPublicEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED, randomUUID().toString()), groupSucceededPublicEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldUpdateStatusToFailedForGroupSummonsProsecution() {
        final String payload = "payload/summons/stagingprosecutors.submit-summons-prosecution-group.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(payload, SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject groupRejectedEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", createArrayBuilder().build())
                .add("defendantErrors", createArrayBuilder().build())
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED, randomUUID().toString()), groupRejectedEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.FAILED);
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
