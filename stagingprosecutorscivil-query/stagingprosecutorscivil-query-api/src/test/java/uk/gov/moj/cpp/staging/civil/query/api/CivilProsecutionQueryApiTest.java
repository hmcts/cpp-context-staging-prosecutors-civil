package uk.gov.moj.cpp.staging.civil.query.api;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.civil.query.CivilProsecutionQueryView;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilProsecutionQueryApiTest {

    @Mock
    private JsonEnvelope expectedEnvelope;

    @Mock
    private CivilProsecutionQueryView civilProsecutionQueryView;

    @InjectMocks
    private CivilProsecutionQueryApi civilProsecutionQueryApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Test
    public void shouldReturnSubmissionDetails() {

        final String submissionId = UUID.randomUUID().toString();
        JsonEnvelope envelope = createEnvelope("stagingprosecutorscivil.query.submission-details",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .build()
        );

        civilProsecutionQueryApi.getSubmissionDetails(envelope);
        verify(civilProsecutionQueryView).querySubmission(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("stagingprosecutorscivil.query.submission-details"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("submissionId"), is(submissionId));
    }
}
