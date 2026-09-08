package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static com.google.common.io.Resources.getResource;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.getSubmissionDetailsRaw;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.getSubmissionErrorDetailsCsv;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.pollForSubmission;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendComplaintsFileUploadRequest;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the query-side prosecuting-authority check on GET /submissions/{submissionId} (both the
 * JSON details and CSV error-details variants): a caller must belong to the prosecuting authority
 * the submission was made for - or hold an exempt role - to view it. See
 * openspec/changes/validate-prosecuting-authority-on-submission-query for the design.
 *
 * <p>Uses the complaints-CSV-upload flow (rather than a plain summons/charge-prosecution submit)
 * because it is the only flow that lets a test control {@code prosecutorShortName} directly via
 * {@link WiremockUtils#stubUserGroupsWithProsecutingAuthority}, without needing a separate
 * reference-data stub for every scenario.
 */
public class SubmissionQueryAuthorizationIT {

    private static final String COMPLAINTS_CSV = "payload/complaints/complaints-summons-prosecution.csv";
    private static final String DVLA = "DVLA";
    private static final String TFL = "TFL";

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
    public void shouldReturnSubmissionDetailsWhenCallerBelongsToTheSubmissionsProsecutingAuthority() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(DVLA);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(DVLA);

        final UUID submissionId = uploadComplaintsCsvAndAwaitPending();

        // caller's WireMock-stubbed authority (DVLA) still matches the submission's - unchanged
        // since upload
        final Response response = getSubmissionDetailsRaw(submissionId, randomUUID().toString());

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.readEntity(String.class), containsString(submissionId.toString()));
    }

    @Test
    public void shouldRejectSubmissionDetailsWhenCallerDoesNotBelongToTheSubmissionsProsecutingAuthority() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(DVLA);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(DVLA);

        final UUID submissionId = uploadComplaintsCsvAndAwaitPending();

        // now simulate a caller from a different, non-exempt prosecuting authority
        wiremockUtils.stubUserGroupsForGroupName("Charging Lawyers", TFL);

        final Response response = getSubmissionDetailsRaw(submissionId, randomUUID().toString());

        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(new JSONObject(response.readEntity(String.class)).getString("error"),
                is("The prosecutor information does not match"));
    }

    @Test
    public void shouldRejectSubmissionErrorDetailsCsvWhenCallerDoesNotBelongToTheSubmissionsProsecutingAuthority() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(DVLA);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(DVLA);

        final UUID submissionId = uploadComplaintsCsvAndAwaitPending();

        wiremockUtils.stubUserGroupsForGroupName("Charging Lawyers", TFL);

        final Response response = getSubmissionErrorDetailsCsv(submissionId, randomUUID().toString());

        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(new JSONObject(response.readEntity(String.class)).getString("error"),
                is("The prosecutor information does not match"));
    }

    @Test
    public void shouldReturnSubmissionDetailsWhenCallerBelongsToAnExemptGroupRegardlessOfProsecutingAuthority() throws IOException {
        wiremockUtils.stubUserGroupsWithProsecutingAuthority(DVLA);
        wiremockUtils.stubReferenceDataProsecutorByOuCode(DVLA);

        final UUID submissionId = uploadComplaintsCsvAndAwaitPending();

        // exempt role, different (non-matching) authority - still allowed
        wiremockUtils.stubUserGroupsForGroupName("Court Associate", TFL);

        final Response response = getSubmissionDetailsRaw(submissionId, randomUUID().toString());

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.readEntity(String.class), containsString(submissionId.toString()));
    }

    private UUID uploadComplaintsCsvAndAwaitPending() throws IOException {
        final HttpResponse response = sendComplaintsFileUploadRequest(getFileFrom(COMPLAINTS_CSV), randomUUID().toString());
        final String body = EntityUtils.toString(response.getEntity());
        final UUID submissionId = UUID.fromString(new JSONObject(body).getString("submissionId"));
        pollForSubmission(submissionId, SubmissionStatus.PENDING);
        return submissionId;
    }

    private File getFileFrom(final String filePath) {
        return new File(getResource(filePath).getFile());
    }
}
