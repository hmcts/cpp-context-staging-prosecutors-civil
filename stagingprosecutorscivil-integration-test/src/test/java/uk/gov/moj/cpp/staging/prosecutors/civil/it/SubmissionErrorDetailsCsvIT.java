package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.ResourcesUtils.asJsonObject;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.ResourcesUtils.readExpectedCsv;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.SUMMONS_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.UrlResponse;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives a real {@code civil-prosecution-rejected} public event (the same one PCF sends on
 * business-validation failure) so the viewstore's case_errors/defendant_errors columns are
 * populated exactly as they would be in production, then asserts the CSV rendering of
 * GET /submissions/{submissionId} against that real data - not just the "empty arrays" shortcut
 * the existing status-transition ITs use, since an empty-array run would never exercise the CSV
 * row-building logic itself.
 */
public class SubmissionErrorDetailsCsvIT {

    private static final String PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED = "public.prosecutioncasefile.civil-prosecution-rejected";

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
    public void shouldReturnCsvOfCaseAndDefendantErrorsWhenSubmissionFailed() {
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummons(
                "payload/summons/stagingcivil.submit-summons-prosecution-all-fields.json", SUMMONS_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject rejectedEvent = asJsonObject("payload/events/civil-prosecution-rejected-case-and-defendant-errors.json",
                Map.of("%CASE_ID%", randomUUID().toString(), "%SUBMISSION_ID%", submissionId.toString()));

        messageProducerClientPublic.sendMessage(
                PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED,
                envelopeFrom(buildMetadata(PUBLIC_EVENT_PCF_CIVIL_PROSECUTION_REJECTED, randomUUID().toString()), rejectedEvent));

        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.FAILED);

        final Response response = StagingProsecutorsCivilUtils.getSubmissionErrorDetailsCsv(submissionId);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("Content-Type"), containsString("text/csv"));
        assertThat(response.getHeaderString("Content-Disposition"), containsString("attachment"));
        assertThat(response.getHeaderString("Content-Disposition"), containsString(submissionId.toString()));

        final String csv = response.readEntity(String.class);
        // A direct JSON API submission has no uploaded file, so prosecutioncasefile's keys are kept as sent
        // (complaints-file uploads report them as template columns - see ComplaintsFilesUploadIT).
        final String expectedCsv = readExpectedCsv("payload/error-reports/json-api-case-and-defendant-errors.csv");
        assertThat(csv, is(expectedCsv));
    }
}
