package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.updateCivilProsecutionSubmissionSucceeded;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CivilProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionSubmissionSucceededPublicEventProcessorTest {

    @InjectMocks
    private ProsecutionSubmissionSucceededPublicEventProcessor prosecutionSubmissionSucceededPublicEventProcessor;

    @Mock
    private Response response;

    @Mock
    private Sender sender;

    @Test
    public void shouldHandleProsecutionSubmissionSucceeded() {
        final CivilProsecutionSubmissionSucceeded prosecutionSubmissionSucceeded = updateCivilProsecutionSubmissionSucceeded();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<CivilProsecutionSubmissionSucceeded> updateProsecutionSubmissionSucceededEnvelope = testEnvelope(prosecutionSubmissionSucceeded, "public.prosecutioncasefile.civil-prosecution-submission-succeeded",
                prosecutionSubmissionSucceeded.getExternalId().toString(), eventCreatedTime);
        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceeded(updateProsecutionSubmissionSucceededEnvelope);
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(prosecutionSubmissionSucceeded.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.name()));
    }

    @Test
    public void shouldHandleProsecutionSubmissionSucceededWithWarnings() {
        final ProsecutionSubmissionSucceededWithWarnings prosecutionSubmissionSucceededWithWarnings = ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings()
                .withExternalId(randomUUID())
                .withChannel(Channel.CIVIL)
                .withWarnings(new ArrayList<>())
                .build();

        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<ProsecutionSubmissionSucceededWithWarnings> updateProsecutionSubmissionSucceededEnvelope = testEnvelope(prosecutionSubmissionSucceededWithWarnings, "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings",
                prosecutionSubmissionSucceededWithWarnings.getExternalId().toString(), eventCreatedTime);
        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceededWithWarnings(updateProsecutionSubmissionSucceededEnvelope);
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(prosecutionSubmissionSucceededWithWarnings.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS_WITH_WARNINGS.name()));
    }

    private <T> Envelope<T> testEnvelope(final T payload, final String eventName, final String submissionId, final ZonedDateTime createdAt) {
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
