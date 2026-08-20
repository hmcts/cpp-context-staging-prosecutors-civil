package uk.gov.moj.cpp.staging.civil.handler.command.api.client;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProsecutorReferenceDataClientTest {

    private static final String OUCODE = "GAFTL00";

    @InjectMocks
    private ProsecutorReferenceDataClient prosecutorReferenceDataClient;

    @Mock
    private Requester requester;

    @Test
    void returnsShortNameFromResponse() {
        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode").build(),
                Json.createObjectBuilder().add("shortName", "GAAAA01").build()));

        assertThat(prosecutorReferenceDataClient.getProsecutorShortNameForOuCode(OUCODE), is("GAAAA01"));
    }

    @Test
    void returnsNullWhenShortNameAbsentFromResponse() {
        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode").build(),
                Json.createObjectBuilder().build()));

        assertThat(prosecutorReferenceDataClient.getProsecutorShortNameForOuCode(OUCODE), is(nullValue()));
    }
}
