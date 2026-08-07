package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static com.google.common.io.Resources.getResource;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.pollForSubmission;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendComplaintsFileUploadRequest;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendComplaintsFileUploadRequestWithoutFilePart;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendComplaintsFileUploadRequestWithoutUserIdHeader;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

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
    private static final String CSV_PROSECUTING_AUTHORITY = "GAAAA01";

    private final WiremockUtils wiremockUtils = new WiremockUtils();

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
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(CSV_PROSECUTING_AUTHORITY);

        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(response);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);
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

    // Disabled per request, alongside the production check it exercised: verification of the
    // calling user's organisation against the CSV's prosecuting authority is currently commented
    // out in DefaultCommandApiComplaintsFilesResource, so this scenario no longer applies.
    // @Test
    // public void shouldRejectUploadWhenCallingUserOrganisationDoesNotMatchCsvProsecutingAuthority() throws IOException {
    //     wiremockUtils.stubUserGroupsWithProsecutingAuthority("TFL");
    //
    //     final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
    //
    //     assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    // }

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
