package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFilePublicEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private ProsecutionCaseFilePublicEventProcessor target;

    @Test
    public void shouldHandleCaseMaterialRejectedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method("caseMaterialRejected")
                        .thatHandles("public.prosecutioncasefile.material-rejected")
                ));
    }

    @Test
    public void shouldSendRejectMaterialCommandWhenSubmissionIdPresent() {
        final UUID submissionId = randomUUID();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final JsonArray errors = Json.createArrayBuilder().add("some error").build();

        final JsonEnvelope envelope = testEnvelopeWithSubmissionId(
                "public.prosecutioncasefile.material-rejected",
                submissionId,
                eventCreatedTime,
                errors);

        target.caseMaterialRejected(envelope);

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());

        assertThat(captor.getValue().metadata().name(), is("stagingprosecutorscivil.command.reject-material"));
        final JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(submissionId.toString()));
        assertThat(payload.getJsonArray("errors"), is(errors));
    }

    @Test
    public void shouldNotSendCommandWhenSubmissionIdNotPresentInMetadata() {
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();

        final JsonEnvelope envelope = testEnvelope(
                "public.prosecutioncasefile.material-rejected",
                eventCreatedTime);

        target.caseMaterialRejected(envelope);

        verifyNoInteractions(sender);
    }

    private JsonEnvelope testEnvelopeWithSubmissionId(final String eventName, final UUID submissionId, final ZonedDateTime createdAt, final JsonArray errors) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(createdAt);

        return envelopeFrom(
                metadataFrom(createObjectBuilder(
                        metadataBuilder.build().asJsonObject())
                        .add("submissionId", submissionId.toString())
                        .add("errors", errors)
                        .build())
                        .withUserId(randomUUID().toString()).build(),
                Json.createObjectBuilder().build());
    }

    private JsonEnvelope testEnvelope(final String eventName, final ZonedDateTime createdAt) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(createdAt);

        return envelopeFrom(
                metadataFrom(createObjectBuilder(
                        metadataBuilder.build().asJsonObject()).build())
                        .withUserId(randomUUID().toString()).build(),
                Json.createObjectBuilder().build());
    }
}
