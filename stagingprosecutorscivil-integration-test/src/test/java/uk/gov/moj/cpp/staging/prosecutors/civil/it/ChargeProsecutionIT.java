package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.CHARGE_PROSECUTION_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.model.Submission;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.ProsecutionCaseFileApi;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.UrlResponse;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ChargeProsecutionIT {

    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED       = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_GROUP_SUBMISSION_SUCCEEDED                   = "public.prosecutioncasefile.group-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED                   = "public.prosecutioncasefile.group-prosecution-rejected";
    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED                   = "public.prosecutioncasefile.civil-prosecution-rejected";
    private static final String PUBLIC_EVENT_PCF_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";

    private static final String PROSECUTOR_CASE_REFERENCE = "URN-CIVIL-CASE-PROBLEM-1";
    private static final String CASE_PROBLEM_CODE = "CASE_URN_ALREADY_EXISTS";
    private static final String CASE_PROBLEM_VALUE_KEY = "urn";
    private static final String CASE_PROBLEM_VALUE = "URN-CIVIL-CASE-PROBLEM-1";
    private static final String GROUP_CASE_PROBLEM_CODE = "GROUP_PAYMENT_REFERENCE_INVALID";

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
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateGroupProsecutionInvokedWith("payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json");
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
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-mandatory-fields-only.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        ProsecutionCaseFileApi.expectInitiateGroupProsecutionInvokedWith("payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json");
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(urlResponse.getSubmissionId(), SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(urlResponse.getSubmissionId().toString()));
    }

    @Test
    public void shouldSubmitProsecutionForSingleCaseSuccess() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json");
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
    public void shouldUpdateStatusToRejectedForGroupProsecution() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateGroupProsecutionInvokedWith("payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json");
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
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json");
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
    public void shouldPersistCaseProblemShapedCaseErrorsForRejectedSingleCaseProsecution() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json");
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        JsonObject rejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", caseProblems())
                .add("defendantErrors", createArrayBuilder().build())
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));

        final JsonObject submission = StagingProsecutorsCivilUtils.pollForSubmissionAsJson(submissionId, SubmissionStatus.REJECTED);
        assertThat(submission.getString("id"), Matchers.is(submissionId.toString()));
        assertCaseProblemsPersisted(submission.getJsonArray("caseErrors"));
    }

    @Test
    public void shouldPersistCaseProblemShapedCaseErrorsForRejectedGroupProsecution() {
        stubPCFCommand(randomUUID());
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateGroupProsecutionInvokedWith("payload/charge/stagingprosecutors.submit-charge-prosecution-all-fields.json");
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        JsonObject rejectedEvent = createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", caseProblems())
                .add("groupCaseErrors", groupCaseProblems())
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_GROUP_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));

        final JsonObject submission = StagingProsecutorsCivilUtils.pollForSubmissionAsJson(submissionId, SubmissionStatus.REJECTED);
        assertThat(submission.getString("id"), Matchers.is(submissionId.toString()));

        // case-level and group-level problems are merged into a single CaseProblem[] list, persisted under caseErrors
        final JsonArray mergedCaseErrors = submission.getJsonArray("caseErrors");
        assertThat(mergedCaseErrors.size(), Matchers.is(2));

        final JsonObject caseLevelProblem = mergedCaseErrors.getJsonObject(0);
        assertThat(caseLevelProblem.getString("prosecutorCaseReference"), Matchers.is(PROSECUTOR_CASE_REFERENCE));
        assertThat(caseLevelProblem.getJsonArray("problems").getJsonObject(0).getString("code"), Matchers.is(CASE_PROBLEM_CODE));

        final JsonObject groupLevelProblem = mergedCaseErrors.getJsonObject(1);
        // group-level problems are not tied to a single case, so prosecutorCaseReference is
        // never serialised (the framework ObjectMapper uses NON_ABSENT inclusion, and
        // case-problem.json types prosecutorCaseReference as a plain string, so an explicit
        // null would fail command schema validation)
        assertThat(groupLevelProblem.containsKey("prosecutorCaseReference"), Matchers.is(false));
        assertThat(groupLevelProblem.getJsonArray("problems").getJsonObject(0).getString("code"), Matchers.is(GROUP_CASE_PROBLEM_CODE));
    }

    private JsonArray caseProblems() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("prosecutorCaseReference", PROSECUTOR_CASE_REFERENCE)
                        .add("problems", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("code", CASE_PROBLEM_CODE)
                                        .add("values", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("key", CASE_PROBLEM_VALUE_KEY)
                                                        .add("value", CASE_PROBLEM_VALUE))))))
                .build();
    }

    private JsonArray groupCaseProblems() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("problems", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("code", GROUP_CASE_PROBLEM_CODE)
                                        .add("values", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("key", "paymentReference")
                                                        .add("value", "INVALID"))))))
                .build();
    }

    private void assertCaseProblemsPersisted(final JsonArray errors) {
        assertThat(errors.size(), Matchers.is(1));

        final JsonObject caseProblem = errors.getJsonObject(0);
        assertThat(caseProblem.getString("prosecutorCaseReference"), Matchers.is(PROSECUTOR_CASE_REFERENCE));

        final JsonArray problems = caseProblem.getJsonArray("problems");
        assertThat(problems.size(), Matchers.is(1));

        final JsonObject problem = problems.getJsonObject(0);
        assertThat(problem.getString("code"), Matchers.is(CASE_PROBLEM_CODE));

        final JsonArray values = problem.getJsonArray("values");
        assertThat(values.size(), Matchers.is(1));
        assertThat(values.getJsonObject(0).getString("key"), Matchers.is(CASE_PROBLEM_VALUE_KEY));
        assertThat(values.getJsonObject(0).getString("value"), Matchers.is(CASE_PROBLEM_VALUE));
    }


    @Test
    public void shouldSubmitChargeProsecutionWithRelatedReferenceNumber() {
        stubPCFCommand(randomUUID());
        final String payload = "payload/charge/stagingprosecutors.submit-charge-prosecution-with-related-reference.json";
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution(payload, CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution(payload);
        final Submission submission = StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        assertThat(submission.getSubmissionId().toString(), Matchers.is(submissionId.toString()));
    }

    @Test
    public void shouldSubmitChargeProsecutionForYouthDefendantWithIndividualParentGuardian() {
        stubPCFCommand(randomUUID());
        final String payload = "payload/charge/stagingprosecutors.submit-charge-prosecution-youth-individual-guardian.json";
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution(payload, CHARGE_PROSECUTION_CONTENT_TYPE);
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
        UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitChargeProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json", CHARGE_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        ProsecutionCaseFileApi.expectInitiateSingleProsecution("payload/charge/stagingprosecutors.submit-charge-prosecution-single-case.json");
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
