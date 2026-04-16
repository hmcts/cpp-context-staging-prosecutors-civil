package uk.gov.moj.cpp.staging.civil.processor;

import uk.gov.justice.services.messaging.JsonObjects;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.updateGroupSubmissionSucceeded;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSubmissionSucceeded;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GroupSubmissionSucceededPublicEventProcessorTest {

    @InjectMocks
    private GroupSubmissionSucceededPublicEventProcessor groupSubmissionSucceededPublicEventProcessor;

    @Mock
    private Response response;

    @Mock
    private Sender sender;

    @Test
    public void shouldHandleGroupSubmissionSucceeded() {
        final GroupSubmissionSucceeded groupSubmissionSucceeded = updateGroupSubmissionSucceeded();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<GroupSubmissionSucceeded> updateGroupSubmissionSucceededEnvelope = testEnvelope(groupSubmissionSucceeded, "public.prosecutioncasefile.group-submission-succeeded",
                groupSubmissionSucceeded.getGroupId().toString(), eventCreatedTime);
        groupSubmissionSucceededPublicEventProcessor.groupSubmissionSucceeded(updateGroupSubmissionSucceededEnvelope);
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(groupSubmissionSucceeded.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.name()));
    }

    private <T> Envelope<T> testEnvelope(final T payload, final String eventName, final String submissionId, final ZonedDateTime createdAt) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(UUID.fromString(submissionId))
                .createdAt(createdAt);

        return Envelope.envelopeFrom(metadataFrom(createObjectBuilder(
                metadataBuilder.build().asJsonObject()).build())
                .withUserId(randomUUID().toString()).build(), payload);
    }
}
