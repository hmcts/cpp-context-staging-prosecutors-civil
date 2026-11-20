package uk.gov.moj.cpp.staging.prosecutors.civil;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.OTHER_CASE_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master) against the deployed service.
 *
 * SC-I01 (SC-P04): a new submission is stored with errors and warnings null, and the submission-details query adds
 * both to the response unconditionally; on J17 JsonArrayConverter turns the null columns into empty arrays. Pin the
 * whole PENDING response.
 * SC-I02 (BC-16): the framework metrics endpoints under /internal/metrics.
 * SC-I03 moved to SubmissionRepositoryParityTest#scP07: case_detail rows are not exposed by any API, and the CI
 * validation viewstore cannot be reached with the local IT database credentials.
 */
public class JavaUpgradeParityIT {

    private static final String SUBMISSION_URL = getBaseUri() + "/stagingprosecutorscivil-query-api/query/api/rest/staging-civil/submissions/";
    private static final String METRICS_PING_URL = getBaseUri() + "/stagingprosecutorscivil-service/internal/metrics/ping";

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
    public void scI01_pendingSubmissionShouldBeReturnedInTheJ17Shape() {
        final UUID submissionId = StagingProsecutorsCivilUtils
                .submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-single-case.json", OTHER_CASE_CONTENT_TYPE)
                .getSubmissionId();

        final String response = poll(requestParams(SUBMISSION_URL + submissionId, "application/vnd.stagingcivil.submission-details+json")
                .withHeader(USER_ID, randomUUID()))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(10, SECONDS)
                .until(status().is(OK), payload().isJson(withJsonPath("$.status", is("PENDING"))))
                .getPayload();

        assertThat(response.replace(submissionId.toString(), "SUBMISSION_ID"), is(
                "{\"id\":\"SUBMISSION_ID\",\"status\":\"PENDING\",\"warnings\":[],\"errors\":[],\"caseErrors\":[],\"defendantErrors\":[]}"));
    }

    @Test
    public void scI02_metricsPingShouldAnswerWithTheJ17Status() throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(METRICS_PING_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));
    }
}
