package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.OTHER_CASES_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.SUMMONS_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.model.Submission;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.UrlResponse;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SubmitSummonsIT {

    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.group-submission-succeeded";

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
    public void shouldSubmitSummons() {
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummons("payload/summons/stagingcivil.submit-summons-prosecution-all-fields.json", SUMMONS_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));

        JsonObject caseSucceededPublicEvent = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, randomUUID().toString()), caseSucceededPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, publicEventEnvelope);

        final Submission submission2 = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        Assert.assertThat(submission2.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldSubmitOtherCases() {
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCases("payload/other/stagingcivil.submit-other-prosecution-all-fields.json", OTHER_CASES_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));

        JsonObject caseSucceededPublicEvent = Json.createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .build();
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED, randomUUID().toString()), caseSucceededPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED, publicEventEnvelope);

        final Submission submission2 = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        Assert.assertThat(submission2.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }
}
