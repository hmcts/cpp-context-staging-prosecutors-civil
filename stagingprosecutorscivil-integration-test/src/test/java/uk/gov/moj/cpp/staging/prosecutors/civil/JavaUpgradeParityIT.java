package uk.gov.moj.cpp.staging.prosecutors.civil;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.OTHER_CASE_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
 * SC-I03 (SC-P06): the received handlers save a new Submission with cascaded CaseDetail rows, and the update handlers
 * load, change and save it again (J25: always merge, with orphanRemoval on the case details). Pin the case_detail
 * rows before and after a status update, read straight from the viewstore.
 */
public class JavaUpgradeParityIT {

    private static final String SUBMISSION_URL = getBaseUri() + "/stagingprosecutorscivil-query-api/query/api/rest/staging-civil/submissions/";
    private static final String VIEWSTORE_JDBC_URL = "jdbc:postgresql://localhost:5432/stagingprosecutorscivilviewstore";
    private static final String VIEWSTORE_USER_AND_PASSWORD = "stagingprosecutorscivil";
    private static final String PCF_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
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
    public void scI03_statusUpdateShouldKeepTheCaseDetailRows() throws SQLException {
        final UUID submissionId = StagingProsecutorsCivilUtils
                .submitOtherCase("payload/other/stagingcivil.submit-other-prosecution-single-case.json", OTHER_CASE_CONTENT_TYPE)
                .getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);
        final List<String> caseDetailsWhenPending = caseDetailRows(submissionId);

        final JmsMessageProducerClient publicProducer = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicProducer.sendMessage(PCF_SUBMISSION_SUCCEEDED, envelopeFrom(buildMetadata(PCF_SUBMISSION_SUCCEEDED, randomUUID().toString()),
                createObjectBuilder()
                        .add("caseId", randomUUID().toString())
                        .add("externalId", submissionId.toString())
                        .add("channel", "CIVIL")
                        .build()));
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);

        // one row for the single case; the update keeps the same row (same id), it does not replace or drop it
        assertThat(caseDetailsWhenPending.size(), is(1));
        assertThat(caseDetailsWhenPending.get(0).endsWith("|case-urn-value-1"), is(true));
        assertThat(caseDetailRows(submissionId), is(caseDetailsWhenPending));
    }

    @Test
    public void scI02_metricsPingShouldAnswerWithTheJ17Status() throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(METRICS_PING_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));
    }

    private static List<String> caseDetailRows(final UUID submissionId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(VIEWSTORE_JDBC_URL, VIEWSTORE_USER_AND_PASSWORD, VIEWSTORE_USER_AND_PASSWORD);
             PreparedStatement statement = connection.prepareStatement("select id, case_urn from case_detail where submission_id = ? order by case_urn")) {
            statement.setObject(1, submissionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<String> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(resultSet.getString("id") + "|" + resultSet.getString("case_urn"));
                }
                return rows;
            }
        }
    }
}
