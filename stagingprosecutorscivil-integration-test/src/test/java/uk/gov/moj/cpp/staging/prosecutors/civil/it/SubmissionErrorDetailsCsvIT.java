package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.PCFStub.stubPCFCommand;
import static uk.gov.moj.cpp.staging.prosecutors.civil.stub.SystemIDMapperStub.stubAddMany;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.SUMMONS_PROSECUTION_CONTENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.buildMetadata;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.UrlResponse;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

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
        final UrlResponse urlResponse = StagingProsecutorsCivilUtils.submitSummonsProsecution(
                "payload/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_PROSECUTION_CONTENT_TYPE);
        final UUID submissionId = urlResponse.getSubmissionId();
        StagingProsecutorsCivilUtils.pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final JsonObject rejectedEvent = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("externalId", submissionId.toString())
                .add("channel", "CIVIL")
                .add("caseErrors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("prosecutorCaseReference", "123")
                                .add("problems", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("code", "PROSECUTOR_OUCODE_NOT_RECOGNISED")
                                                .add("values", createArrayBuilder()
                                                        .add(createObjectBuilder()
                                                                .add("key", "prosecutingAuthority")
                                                                .add("value", "A010000")))))))
                .add("defendantErrors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("prosecutorDefendantReference", "cad5a01")
                                .add("problems", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("code", "OFFENCE_CODE_NOT_SUPPORTED")
                                                .add("values", createArrayBuilder()
                                                        .add(createObjectBuilder()
                                                                .add("key", "offence_offenceCode")
                                                                .add("value", "AX03547")))))))
                .build();

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
        final String expectedCsv = "Reference,Error Type,Error Code,Field,Value\n"
                + "123,Case,PROSECUTOR_OUCODE_NOT_RECOGNISED,prosecutingAuthority,A010000\n"
                + "cad5a01,Defendant,OFFENCE_CODE_NOT_SUPPORTED,offence_offenceCode,AX03547";
        assertThat(csv, is(expectedCsv));
    }
}
