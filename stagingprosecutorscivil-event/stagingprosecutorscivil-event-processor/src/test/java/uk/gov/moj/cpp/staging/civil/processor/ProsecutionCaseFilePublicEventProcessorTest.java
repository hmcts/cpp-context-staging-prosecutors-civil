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
        final UUID submissionId = UUID.fromString("646c31e8-5ed4-4d0d-ba89-ea0f4aa95edb");

        final JsonArray errors = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("code", "DEFENDANT_ID_INVALID")
                        .add("values", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("key", "prosecutorDefendantId")
                                        .add("value", "162d987f-b5e5-4528-b7c1-1396d6f02bb6")
                                        .build())
                                .build())
                        .build())
                .build();

        final JsonObject incomingPayload = Json.createObjectBuilder()
                .add("caseId", "9c3390ba-66bf-4096-a8bf-8eecee43fc5e")
                .add("errors", errors)
                .add("material", Json.createObjectBuilder()
                        .add("documentType", "Defence Statement")
                        .add("fileStoreId", "49e2baf9-2f6e-4595-89c2-224a1f0d6b3a")
                        .add("fileType", "application/pdf")
                        .build())
                .add("prosecutingAuthority", "GAEAA01")
                .add("prosecutorDefendantId", "162d987f-b5e5-4528-b7c1-1396d6f02bb6")
                .build();

        final JsonObject metadataJson = Json.createObjectBuilder()
                .add("id", "379100ea-fbfd-4e21-953f-8e5a4d44e4b9")
                .add("name", "prosecutioncasefile.events.material-rejected")
                .add("createdAt", "2026-06-23T08:22:01.356Z")
                .add("source", "prosecutioncasefile")
                .add("context", Json.createObjectBuilder()
                        .add("user", "6503ff7a-f05d-11eb-9a03-0242ac130003")
                        .build())
                .add("stream", Json.createObjectBuilder()
                        .add("id", "9c3390ba-66bf-4096-a8bf-8eecee43fc5e")
                        .add("version", 5)
                        .build())
                .add("event", Json.createObjectBuilder()
                        .add("eventNumber", 2461)
                        .add("previousEventNumber", 2460)
                        .build())
                .add("submissionId", submissionId.toString())
                .add("causation", Json.createArrayBuilder()
                        .add("27527e56-30f2-4ca2-ade3-117473d04e33")
                        .add("589b2682-27ca-43a4-9917-82547fc2140b")
                        .add("4d837dac-b45a-4794-bc9e-136677abbaae")
                        .add("4d837dac-b45a-4794-bc9e-136677abbaae")
                        .build())
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataFrom(metadataJson).build(),
                incomingPayload);

        target.caseMaterialRejected(envelope);

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());

        assertThat(captor.getValue().metadata().name(), is("stagingprosecutorscivil.command.reject-material"));
        final JsonObject sentPayload = (JsonObject) captor.getValue().payload();
        assertThat(sentPayload.getString("submissionId"), is(submissionId.toString()));
        assertThat(sentPayload.getJsonArray("errors"), is(errors));
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
