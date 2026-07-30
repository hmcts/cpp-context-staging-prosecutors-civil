package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.OTHER_CASE_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.common.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.model.Submission;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.ProsecutionCaseFileApi;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.UrlResponse;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class OtherProsecutionIT {

    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED       = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED                   = "public.prosecutioncasefile.group-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED                   = "public.prosecutioncasefile.group-prosecution-rejected";
    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED                   = "public.prosecutioncasefile.civil-prosecution-rejected";
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
    }

    @Test
    public void shouldSubmitProsecutionForGroupCaseSuccess() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-all-fields.json", OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateGroupProsecutionInvokedWith("payload/other/stagingcivil.submit-other-prosecution-all-fields.json");
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));

        JsonObject caseSucceededPublicEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .build();
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED, randomUUID().toString()), caseSucceededPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED, publicEventEnvelope);

        final Submission submission2 = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        assertThat(submission2.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }
    @Test
    public void shouldSubmitProsecutionForGroupCaseSuccessWithOnlyMandatoryFields() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-mandatory-fields-only.json", OTHER_CASE_CONTENT_TYPE);
        ProsecutionCaseFileApi.expectInitiateGroupProsecutionInvokedWith("payload/other/stagingcivil.submit-other-prosecution-all-fields.json");
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(urlResponse.getSubmissionId(), SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(urlResponse.getSubmissionId().toString()));
    }

    @Test
    public void shouldSubmitProsecutionForSingleCaseSuccess() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-single-case.json", OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/other/stagingcivil.submit-other-prosecution-single-case.json");
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(urlResponse.getSubmissionId().toString()));

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
    public void shouldReturnSuccessResponseWithAllRequiredFieldsForAC2() {
        final String ouCode = "GAAAA01";
        stubPCFCommand(randomUUID());
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase(
                "payload/other/stagingcivil.submit-other-prosecution-single-case.json",
                OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/other/stagingcivil.submit-other-prosecution-single-case.json");

        final JsonObject caseSucceededPublicEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        final JsonEnvelope publicEventEnvelope = envelopeFrom(
                buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, randomUUID().toString()),
                caseSucceededPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, publicEventEnvelope);

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS, ouCode);

        assertThat(submission.getSubmissionId().toString(), is(submissionId.toString()));
        assertThat(submission.getSubmissionStatus(), is(SubmissionStatus.SUCCESS.name()));
        assertThat(submission.getType(), is("PROSECUTION"));
        assertThat(submission.getErrors(), is(empty()));
        assertThat(submission.getWarnings(), is(empty()));
        assertThat(submission.getReceivedAt(), is(notNullValue()));
        assertThat(submission.getCompletedAt(), is(notNullValue()));
    }

    @Test
    public void shouldReturnSuccessWithWarningsResponseWithWarningObjectsForAC3() {
        final String ouCode = "GAAAA01";
        stubPCFCommand(randomUUID());
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase(
                "payload/other/stagingcivil.submit-other-prosecution-single-case.json",
                OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/other/stagingcivil.submit-other-prosecution-single-case.json");

        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject warningsEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("warnings", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("code", "DEFENDANT_DOB_IN_FUTURE")
                                .add("values", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("key", "dob")
                                                .add("value", "2050-01-01")))))
                .build();
        final JsonEnvelope warningsEventEnvelope = envelopeFrom(
                buildMetadata("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings", randomUUID().toString()),
                warningsEvent);
        messageProducerClientPublic.sendMessage(
                "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings",
                warningsEventEnvelope);

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS_WITH_WARNINGS, ouCode);

        assertThat(submission.getSubmissionId().toString(), is(submissionId.toString()));
        assertThat(submission.getSubmissionStatus(), is(SubmissionStatus.SUCCESS_WITH_WARNINGS.name()));
        assertThat(submission.getWarnings(), hasSize(greaterThanOrEqualTo(1)));
        final Problem firstWarning = submission.getWarnings().get(0);
        assertThat(firstWarning.code, is("DEFENDANT_DOB_IN_FUTURE"));
        assertThat(firstWarning.values, hasSize(greaterThanOrEqualTo(1)));
        assertThat(firstWarning.values.get(0).key, is("dob"));
        assertThat(firstWarning.values.get(0).value, is("2050-01-01"));
    }

    @Test
    public void shouldReturnRejectedResponseWithErrorCodesForAC4() {
        final String ouCode = "GAAAA01";
        stubPCFCommand(randomUUID());
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase(
                "payload/other/stagingcivil.submit-other-prosecution-single-case.json",
                OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/other/stagingcivil.submit-other-prosecution-single-case.json");

        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject rejectionEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("code", "DEFENDANT_DOB_IN_FUTURE")
                                .add("values", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("key", "dob")
                                                .add("value", "2050-01-01")))))
                .build();
        final JsonEnvelope rejectionEventEnvelope = envelopeFrom(
                buildMetadata("public.prosecutioncasefile.civil-prosecution-rejected", randomUUID().toString()),
                rejectionEvent);
        messageProducerClientPublic.sendMessage(
                "public.prosecutioncasefile.civil-prosecution-rejected",
                rejectionEventEnvelope);

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.REJECTED, ouCode);

        assertThat(submission.getSubmissionId().toString(), is(submissionId.toString()));
        assertThat(submission.getSubmissionStatus(), is(SubmissionStatus.REJECTED.name()));
        assertThat(submission.getErrors(), hasSize(greaterThanOrEqualTo(1)));
        final Problem firstError = submission.getErrors().get(0);
        assertThat(firstError.code, is("DEFENDANT_DOB_IN_FUTURE"));
        assertThat(firstError.values, hasSize(greaterThanOrEqualTo(1)));
        assertThat(firstError.values.get(0).key, is("dob"));
    }

    @Test
    public void shouldUpdateStatusToRejectedForGroupProsecution() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-all-fields.json", OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateGroupProsecutionInvokedWith("payload/other/stagingcivil.submit-other-prosecution-all-fields.json");
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        JsonObject rejectedEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));

        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.REJECTED);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldUpdateStatusToRejectedForSingleCaseProsecution() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-single-case.json", OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/other/stagingcivil.submit-other-prosecution-single-case.json");
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
    public void shouldSubmitChargeProsecutionWithRelatedReferenceNumber() {
        stubPCFCommand(randomUUID());
        final String payload = "payload/other/stagingprosecutors.submit-charge-prosecution-with-related-reference.json";
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase(payload, OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution(payload);
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldSubmitChargeProsecutionForYouthDefendantWithIndividualParentGuardian() {
        stubPCFCommand(randomUUID());
        final String payload = "payload/other/stagingprosecutors.submit-charge-prosecution-youth-individual-guardian.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase(payload, OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution(payload);
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

    @Disabled("Works locally but fails in pipeline")
    @Test
    public void shouldUpdateStatusToSuccessWithWarningsForSingleCaseProsecution() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-single-case.json", OTHER_CASE_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/other/stagingcivil.submit-other-prosecution-single-case.json");
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
