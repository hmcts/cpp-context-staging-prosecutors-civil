package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.pollForSubmission;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.sendFileUploadRequest;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.model.common.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SubmitMaterialIT {

    private static final String PROSECUTING_AUTHORITY = "TVL";
    private static final String MATERIAL_TYPE = "Trial Documents";
    private static final String PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cases/%s/material";
    private UUID CASE_ID = null;
    public static final String CONTEXT_NAME = "stagingprosecutorscivil";
    private static final String PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED = "public.prosecutioncasefile.material-rejected";
    private final WiremockUtils wiremockUtils = new WiremockUtils();
    private String materialUploadUrl = null;

    @BeforeEach
    public void stub() {
        CASE_ID = randomUUID();
        materialUploadUrl = format(PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL, CASE_ID);

        wiremockUtils
                .stubPost(materialUploadUrl)
                .stubIdMapperReturningExistingAssociation(CASE_ID);
        setupLoggedInUsersPermissionQueryStub(randomUUID().toString());
    }

    @Test
    public void shouldSubmitMaterial() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequest(caseUrn,
                getFileFrom("submitProsecutionDocument/Testing.pdf"),
                MATERIAL_TYPE, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        wiremockUtils.verifyMaterialUpload(materialUploadUrl, MATERIAL_TYPE, PROSECUTING_AUTHORITY);

        publishPublicProgressionCourtDocumentAdded(submissionId);

        pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
    }

    @Test
    public void shouldHandleRejectedMaterialSubmission() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequest(caseUrn,
                getFileFrom("submitProsecutionDocument/Testing.pdf"),
                MATERIAL_TYPE, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final Problem problem = new Problem("INVALID_DOCUMENT_TYPE", List.of(new Problem.ProblemValue("documentType", "PLEA")));

        publishPublicMaterialSubmissionRejected(CASE_ID, submissionId, problem);
    }

    private File getFileFrom(final String filePath) {
        return new File(getResource(filePath).getFile());
    }

    public static UUID extractSubmissionId(final Optional<String> submissionIdWrapper) {
        return submissionIdWrapper.map(JSONObject::new)
                .filter(eventPayload -> eventPayload.has("submissionId"))
                .map(eventPayload -> fromString(eventPayload.getString("submissionId")))
                .orElseThrow(() -> new AssertionError("Impossible retrieve submissionId"));
    }

    private String extractResponse(final HttpResponse response) throws IOException {
        return IOUtils.toString(response.getEntity().getContent());
    }

    private static void publishPublicProgressionCourtDocumentAdded(final UUID submissionId) {

        sendPublicMessageAndExpectPrivateMessage(
                "stagingprosecutorscivil.command.receive-material-submission-successful",
                createObjectBuilder().build(),
                publicProgressionCourtDocumentAdded(submissionId),
                PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED);
    }
    private static void sendPublicMessageAndExpectPrivateMessage(final String privateEventName,
                                                                 final JsonObject publicEventPayload,
                                                                 final Metadata publicEventMetadata,
                                                                 final String publicEventName) {
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(privateEventName).getMessageConsumerClient();
        sendPublicEvent(publicEventPayload, publicEventMetadata, publicEventName);
        final Optional<String> message = messageConsumerClient.retrieveMessage();
    }

    private static void sendPublicEvent(final JsonObject publicEventPayload, final Metadata publicEventMetadata, final String publicEventName) {
        final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        final JsonEnvelope jsonEnvelope = envelopeFrom(publicEventMetadata, publicEventPayload);
        messageProducerClientPublic.sendMessage(publicEventName, jsonEnvelope);
    }

    private static Metadata publicProgressionCourtDocumentAdded(final UUID submissionId) {
        return publicProsecutionMetadata(submissionId, "public.progression.court-document-added");
    }

    private static Metadata publicProsecutionMetadata(final UUID submissionId, final String name) {
        return metadataFrom(createObjectBuilder(
                metadataBuilder()
                        .withName(name)
                        .withUserId(randomUUID().toString())
                        .withId(UUID.randomUUID())
                        .createdAt(new UtcClock().now())
                        .build()
                        .asJsonObject())
                .add("submissionId", submissionId.toString()).build())
                .build();
    }

    public static void publishPublicMaterialSubmissionRejected(final UUID caseId, final UUID submissionId, final Problem... problems) {

        final JsonArrayBuilder errorBuilder = createArrayBuilder();

        Stream.of(problems).forEach(problem -> {
            final JsonArrayBuilder errorValuesBuilder = createArrayBuilder();

            problem.values.forEach(value -> errorValuesBuilder.add(createObjectBuilder()
                    .add("key", value.key)
                    .add("value", value.value)));

            errorBuilder.add(createObjectBuilder()
                    .add("code", problem.code)
                    .add("values", errorValuesBuilder.build()));
        });

        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("errors", errorBuilder)
                .build();

        sendPublicEvent(eventPayload, publicMaterialRejectedMetadata(submissionId), PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED);
    }

    private static Metadata publicMaterialRejectedMetadata(final UUID submissionId) {
        return publicProsecutionMetadata(submissionId, "public.prosecutioncasefile.material-rejected");
    }
}