package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProgressionPublicEventProcessorTest {

    private static final String PUBLIC_EVENT_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    private static final String COMMAND_RECEIVE_MATERIAL_SUBMISSION_SUCCESSFUL = "stagingprosecutorscivil.command.receive-material-submission-successful";

    @InjectMocks
    private ProgressionPublicEventProcessor progressionPublicEventProcessor;
    @Mock
    private StagingProsecutorsCivilService stagingProsecutorsCivilService;

    @Mock
    private Sender sender;

    @Test
    public void shouldHandleCourtDocumentAddedEvent() {
        assertThat(progressionPublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("caseDocumentUploaded")
                        .thatHandles(PUBLIC_EVENT_COURT_DOCUMENT_ADDED)));
    }

    @Test
    public void shouldSendReceiveMaterialSubmissionSuccessfulCommandWhenSubmissionIdIsPresent() {
        final UUID submissionId = randomUUID();
        final JsonEnvelope envelope = buildCourtDocumentAddedEnvelopeWithSubmissionId(submissionId);

        when(stagingProsecutorsCivilService.submissionExistsById(envelope, submissionId.toString()))
                .thenReturn(java.util.Optional.of(Json.createObjectBuilder().build()));

        progressionPublicEventProcessor.caseDocumentUploaded(envelope);

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().metadata().name(), is(COMMAND_RECEIVE_MATERIAL_SUBMISSION_SUCCESSFUL));
        final JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(submissionId.toString()));
    }

    @Test
    public void shouldNotSendCommandWhenSubmissionDoesNotExist() {
        final UUID submissionId = randomUUID();
        final JsonEnvelope envelope = buildCourtDocumentAddedEnvelopeWithSubmissionId(submissionId);

        when(stagingProsecutorsCivilService.submissionExistsById(envelope, submissionId.toString()))
                .thenReturn(java.util.Optional.empty());

        progressionPublicEventProcessor.caseDocumentUploaded(envelope);

        verifyNoInteractions(sender);
    }

    @Test
    public void shouldNotSendCommandWhenSubmissionIdIsAbsentFromMetadata() {
        final JsonEnvelope envelope = buildCourtDocumentAddedEnvelope();

        progressionPublicEventProcessor.caseDocumentUploaded(envelope);

        verifyNoInteractions(sender);
    }

    private JsonEnvelope buildCourtDocumentAddedEnvelopeWithSubmissionId(final UUID submissionId) {
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENT_COURT_DOCUMENT_ADDED)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(eventCreatedTime);

        return envelopeFrom(
                metadataFrom(createObjectBuilder(metadataBuilder.build().asJsonObject())
                        .add("submissionId", submissionId.toString())
                        .build())
                        .withUserId(randomUUID().toString()).build(),
                Json.createObjectBuilder().build());
    }

    private JsonEnvelope buildCourtDocumentAddedEnvelope() {
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENT_COURT_DOCUMENT_ADDED)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(eventCreatedTime);

        return envelopeFrom(
                metadataFrom(createObjectBuilder(metadataBuilder.build().asJsonObject()).build())
                        .withUserId(randomUUID().toString()).build(),
                Json.createObjectBuilder().build());
    }
}
