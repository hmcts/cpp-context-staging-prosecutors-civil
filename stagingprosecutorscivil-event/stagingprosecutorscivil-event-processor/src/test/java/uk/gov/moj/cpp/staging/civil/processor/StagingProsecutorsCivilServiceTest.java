package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCivilServiceTest {

    private static final String QUERY_SUBMISSION_DETAILS = "stagingprosecutorscivil.submission-details";

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Mock
    private JsonEnvelope inboundEnvelope;

    @Mock
    private JsonEnvelope requestEnvelope;

    @Mock
    private JsonEnvelope responseEnvelope;

    @InjectMocks
    private StagingProsecutorsCivilService stagingProsecutorsCivilService;

    @Test
    void shouldReturnPayloadWhenResponsePayloadIsPresent() {
        final String submissionId = randomUUID().toString();
        final JsonObject payload = Json.createObjectBuilder().add("submissionId", submissionId).build();

        stubEnveloperAndRequester(submissionId, responseEnvelope);
        when(responseEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final Optional<JsonObject> result = stagingProsecutorsCivilService.submissionExistsById(inboundEnvelope, submissionId);

        assertThat(result, is(Optional.of(payload)));
    }

    @Test
    void shouldReturnEmptyWhenResponsePayloadIsNull() {
        final String submissionId = randomUUID().toString();

        stubEnveloperAndRequester(submissionId, responseEnvelope);
        when(responseEnvelope.payloadAsJsonObject()).thenReturn(null);

        final Optional<JsonObject> result = stagingProsecutorsCivilService.submissionExistsById(inboundEnvelope, submissionId);

        assertThat(result, is(Optional.empty()));
    }

    @SuppressWarnings("unchecked")
    private void stubEnveloperAndRequester(final String submissionId, final JsonEnvelope response) {
        final Function<Object, JsonEnvelope> envelopeFunction = mock(Function.class);

        when(enveloper.withMetadataFrom(inboundEnvelope, QUERY_SUBMISSION_DETAILS)).thenReturn(envelopeFunction);
        when(envelopeFunction.apply(eq(Json.createObjectBuilder().add("submissionId", submissionId).build()))).thenReturn(requestEnvelope);
        when(requester.request(requestEnvelope)).thenReturn(response);
    }
}
