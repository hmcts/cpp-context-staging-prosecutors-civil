package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;

import java.time.ZonedDateTime;
import java.util.UUID;

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

        final Envelope<MaterialRejected> envelope = testEnvelopeWithSubmissionId(
                MaterialRejected.materialRejected().build(),
                "public.prosecutioncasefile.material-rejected",
                submissionId,
                eventCreatedTime);

        target.caseMaterialRejected(envelope);

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());

        assertThat(captor.getValue().metadata().name(), is("stagingprosecutorscivil.command.reject-material"));
        final JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(notNullValue()));
    }

    @Test
    public void shouldNotSendCommandWhenSubmissionIdNotPresentInMetadata() {
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();

        final Envelope<MaterialRejected> envelope = testEnvelope(
                MaterialRejected.materialRejected().build(),
                "public.prosecutioncasefile.material-rejected",
                eventCreatedTime);

        target.caseMaterialRejected(envelope);

        verifyNoInteractions(sender);
    }

    private <T> Envelope<T> testEnvelopeWithSubmissionId(final T payload, final String eventName, final UUID submissionId, final ZonedDateTime createdAt) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(createdAt);

        return Envelope.envelopeFrom(metadataFrom(createObjectBuilder(
                metadataBuilder.build().asJsonObject())
                .add("submissionId", submissionId.toString())
                .build())
                .withUserId(randomUUID().toString()).build(), payload);
    }

    private <T> Envelope<T> testEnvelope(final T payload, final String eventName, final ZonedDateTime createdAt) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(createdAt);

        return Envelope.envelopeFrom(metadataFrom(createObjectBuilder(
                metadataBuilder.build().asJsonObject()).build())
                .withUserId(randomUUID().toString()).build(), payload);
    }
}
