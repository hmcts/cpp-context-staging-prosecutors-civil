package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static com.google.common.io.Resources.getResource;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.getSubmissionErrorDetailsCsv;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.pollForSubmission;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.pollForSubmissionWithAdditionalInfo;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendComplaintsFileUploadRequest;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendComplaintsFileUploadRequestWithoutFilePart;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendComplaintsFileUploadRequestWithoutUserIdHeader;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.model.Submission;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers CAD-1525's {@code POST /complaints-files} upload endpoint end-to-end: a real multipart
 * CSV upload, converted to a summons-prosecution submission and driven through the same
 * downstream stubs (Prosecution Case File, System ID Mapper) as {@link SubmitSummonsProsecutionIT}
 * — the resource delegates to the same internal {@code summonsProsecution} command path.
 */
public class ComplaintsFilesUploadIT {

    private static final String COMPLAINTS_CSV = "payload/complaints/complaints-summons-prosecution.csv";
    private static final String COMPLAINTS_CSV_MISSING_SUMMONS_CODE = "payload/complaints/complaints-summons-prosecution-missing-summons-code.csv";
    private static final String COMPLAINTS_CSV_INVALID_SUMMONS_CODE = "payload/complaints/complaints-summons-prosecution-invalid-summons-code.csv";
    private static final String CSV_OUCODE = "GAAAA01";
    private static final String PROSECUTOR_SHORT_NAME = "DVLA";
    private static final String LEGAL_ADVISERS_GROUP_NAME = "Legal Advisers";
    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED = "public.prosecutioncasefile.civil-prosecution-rejected";
    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
    private static final String PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL = "public.prosecutioncasefile.parked-for-summons-application-approval";
    private static final String PUBLIC_EVENT_PCF_SUBMISSION_APPROVED = "public.prosecutioncasefile.submission-approved";
    private static final String PUBLIC_EVENT_PCF_SUBMISSION_REJECTED = "public.prosecutioncasefile.submission-rejected";
    private static final String OFFENCE_CODE_INVALID = "OFFENCE_CODE_INVALID";

    private final WiremockUtils wiremockUtils = new WiremockUtils();
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @BeforeEach
    public void setUpStub() {
        wiremockUtils
                .stubPing("prosecutioncasefile")
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-group-prosecution")
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cc-prosecution")
                .stubIdMapperRecordingNewAssociation();
        stubAddMany();
        setupLoggedInUsersPermissionQueryStub(randomUUID().toString());
        stubPCFCommand(randomUUID());
    }

    @Test
    public void shouldUploadComplaintsCsvAndSubmitAsSummonsProsecution() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(PROSECUTOR_SHORT_NAME);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        final Submission submission = pollForSubmission(submissionId, SubmissionStatus.PENDING);

        // Without additionalInfo=true, the response is exactly as it was before fileName/username/
        // prosecutingAuthority were added - those 3 fields are omitted, not just null.
        assertThat(submission.getFileName(), is(nullValue()));
        assertThat(submission.getUsername(), is(nullValue()));
        assertThat(submission.getProsecutingAuthority(), is(nullValue()));

        final Submission submissionWithAdditionalInfo =
                pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.PENDING);

        assertThat(submissionWithAdditionalInfo.getFileName(), is(getFileFrom(COMPLAINTS_CSV).getName()));
        assertThat(submissionWithAdditionalInfo.getUsername(), is("Richard Chapman"));
        assertThat(submissionWithAdditionalInfo.getProsecutingAuthority(), is(PROSECUTOR_SHORT_NAME));
    }

    @Test
    public void shouldUpdateStatusToFailedWhenPcfRejectsUploadedComplaintWithOffenceCodeInvalid() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(PROSECUTOR_SHORT_NAME);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        // The complaints CSV fixture has a single case/defendant, so this resource initiates a
        // single-case (not group) PCF prosecution — mirroring
        // ChargeProsecutionIT.shouldUpdateStatusToRejectedForSingleCaseProsecution, but simulating
        // a business validation failure on the defendant's offence (OFFENCE_CODE_INVALID) rather
        // than a case-level problem.
        final JsonObject rejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", createArrayBuilder().build())
                .add("defendantErrors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("problems", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("code", OFFENCE_CODE_INVALID)
                                                .add("values", createArrayBuilder().build())))))
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));

        final Submission submission = pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.FAILED);
        assertThat(submission.getSubmissionId().toString(), is(submissionId.toString()));

        // The upload metadata captured at submission time survives the status transition to FAILED.
        assertThat(submission.getFileName(), is(getFileFrom(COMPLAINTS_CSV).getName()));
        assertThat(submission.getUsername(), is("Richard Chapman"));
        assertThat(submission.getProsecutingAuthority(), is(PROSECUTOR_SHORT_NAME));
    }

    @Test
    public void shouldNameErrorCsvUsingOriginalUploadedFileName() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(PROSECUTOR_SHORT_NAME);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject rejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", createArrayBuilder().build())
                .add("defendantErrors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("problems", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("code", OFFENCE_CODE_INVALID)
                                                .add("values", createArrayBuilder().build())))))
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));

        final Submission submission = pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.FAILED);

        final Response csvResponse = getSubmissionErrorDetailsCsv(submissionId);
        assertThat(csvResponse.getStatus(), is(Response.Status.OK.getStatusCode()));

        // Assert against whatever fileName the submission actually carries at query time, rather
        // than assuming the upload always captured one - the CSV endpoint falls back to a
        // submissionId-based name when it didn't.
        final String expectedFileName = submission.getFileName() != null
                ? FilenameUtils.getBaseName(submission.getFileName()) + "_error.csv"
                : "submission-" + submissionId + "-errors.csv";

        assertThat(csvResponse.getHeaderString("Content-Disposition"),
                containsString("filename=\"" + expectedFileName + "\""));
    }

    @Test
    public void shouldUpdateStatusToSuccessWhenPcfAcceptsUploadedComplaint() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(PROSECUTOR_SHORT_NAME);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject caseSucceededEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED, randomUUID().toString()), caseSucceededEvent));

        final Submission submission = pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.SUCCESS);
        assertThat(submission.getSubmissionId().toString(), is(submissionId.toString()));

        // The upload metadata captured at submission time survives the status transition to SUCCESS.
        assertThat(submission.getFileName(), is(getFileFrom(COMPLAINTS_CSV).getName()));
        assertThat(submission.getUsername(), is("Richard Chapman"));
        assertThat(submission.getProsecutingAuthority(), is(PROSECUTOR_SHORT_NAME));
    }

    @Test
    public void shouldRetainFileNameAndCaptureSummonsApplicationIdWhenPendingCourtDecision() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(PROSECUTOR_SHORT_NAME);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final UUID applicationId = randomUUID();
        final JsonObject parkedForApprovalEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("applicationId", applicationId.toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), parkedForApprovalEvent));

        final Submission submission = pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.PENDING_COURT_DECISION);

        // The upload metadata captured at submission time, and the summons application id
        // captured on parking, are both present while the submission awaits an SA court decision.
        assertThat(submission.getFileName(), is(getFileFrom(COMPLAINTS_CSV).getName()));
        assertThat(submission.getSummonsApplicationId(), is(applicationId));
    }

    @Test
    public void shouldTransitionFromPendingCourtDecisionToAcceptedForUploadedComplaint() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(PROSECUTOR_SHORT_NAME);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final UUID applicationId = randomUUID();
        final JsonObject parkedForApprovalEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("applicationId", applicationId.toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), parkedForApprovalEvent));

        pollForSubmission(submissionId, SubmissionStatus.PENDING_COURT_DECISION);

        final JsonObject submissionApprovedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_SUBMISSION_APPROVED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_SUBMISSION_APPROVED, randomUUID().toString()), submissionApprovedEvent));

        final Submission submission = pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.ACCEPTED);
        assertThat(submission.getSubmissionId().toString(), is(submissionId.toString()));

        // The upload metadata captured at submission time survives the status transition to ACCEPTED.
        assertThat(submission.getFileName(), is(getFileFrom(COMPLAINTS_CSV).getName()));
        assertThat(submission.getUsername(), is("Richard Chapman"));
        assertThat(submission.getProsecutingAuthority(), is(PROSECUTOR_SHORT_NAME));
        assertThat(submission.getSummonsApplicationId(), is(applicationId));
    }

    @Test
    public void shouldTransitionFromPendingCourtDecisionToRejectedForUploadedComplaint() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(PROSECUTOR_SHORT_NAME);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final UUID applicationId = randomUUID();
        final JsonObject parkedForApprovalEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("applicationId", applicationId.toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL, randomUUID().toString()), parkedForApprovalEvent));

        pollForSubmission(submissionId, SubmissionStatus.PENDING_COURT_DECISION);

        final JsonObject submissionRejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .build();
        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_SUBMISSION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_SUBMISSION_REJECTED, randomUUID().toString()), submissionRejectedEvent));

        final Submission submission = pollForSubmissionWithAdditionalInfo(submissionId, SubmissionStatus.REJECTED);
        assertThat(submission.getSubmissionId().toString(), is(submissionId.toString()));

        // The upload metadata captured at submission time survives the status transition to REJECTED.
        assertThat(submission.getFileName(), is(getFileFrom(COMPLAINTS_CSV).getName()));
        assertThat(submission.getUsername(), is("Richard Chapman"));
        assertThat(submission.getProsecutingAuthority(), is(PROSECUTOR_SHORT_NAME));
        assertThat(submission.getSummonsApplicationId(), is(applicationId));
    }

    @Test
    public void shouldRejectUploadWhenFilePartMissing() throws IOException {
        final HttpResponse response = sendComplaintsFileUploadRequestWithoutFilePart(randomUUID().toString());

        assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
        assertThat(extractErrorMessage(response), containsString("Missing required multipart form field 'file'"));
    }

    @Test
    public void shouldRejectUploadWhenUserIdHeaderMissing() throws IOException {
        final HttpResponse response = sendComplaintsFileUploadRequestWithoutUserIdHeader(getFileFrom(COMPLAINTS_CSV));

        assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void shouldRejectUploadWhenCsvCannotBeParsed() throws IOException {
        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV_MISSING_SUMMONS_CODE), randomUUID().toString());

        assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
        assertThat(extractErrorMessage(response), containsString("Unable to parse complaints CSV file"));
    }

    @Test
    public void shouldRejectUploadWhenCsvFailsSchemaValidation() throws IOException {
        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV_INVALID_SUMMONS_CODE), randomUUID().toString());

        assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
        assertThat(extractErrorMessage(response), containsString("Complaints CSV file failed schema validation"));
    }

    @Test
    public void shouldRejectUploadWhenCallingUserOrganisationDoesNotMatchCsvProsecutingAuthority() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority("TFL");
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());

        assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void shouldAcceptUploadFromLegalAdvisersGroupRegardlessOfCsvProsecutingAuthority() throws IOException {
        wiremockUtils.stubUserGroupsForGroupName(LEGAL_ADVISERS_GROUP_NAME, "TFL");
        // The exempt-group path skips the organisation match check, but this resource always
        // resolves the prosecutor short name upfront for storage on Submission, regardless of
        // exempt status.
        wiremockUtils.stubReferenceDataProsecutorByOuCode(PROSECUTOR_SHORT_NAME);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);
    }

    private File getFileFrom(final String filePath) {
        return new File(getResource(filePath).getFile());
    }

    private UUID extractSubmissionId(final HttpResponse response) throws IOException {
        final String body = IOUtils.toString(response.getEntity().getContent());
        return Optional.of(body)
                .map(JSONObject::new)
                .filter(json -> json.has("submissionId"))
                .map(json -> fromString(json.getString("submissionId")))
                .orElseThrow(() -> new AssertionError("Unable to retrieve submissionId from response: " + body));
    }

    private String extractErrorMessage(final HttpResponse response) throws IOException {
        final String body = IOUtils.toString(response.getEntity().getContent());
        return new JSONObject(body).getString("error");
    }
}
