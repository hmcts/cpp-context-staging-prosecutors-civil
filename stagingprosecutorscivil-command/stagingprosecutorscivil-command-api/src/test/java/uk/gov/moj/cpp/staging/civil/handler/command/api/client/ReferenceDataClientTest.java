package uk.gov.moj.cpp.staging.civil.handler.command.api.client;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceDataClientTest {

    private static final String OUCODE = "GAFTL00";

    @InjectMocks
    private ReferenceDataClient referenceDataClient;

    @Mock
    private Requester requester;

    @Test
    void returnsShortNameFromResponse() {
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode").build(),
                Json.createObjectBuilder().add("shortName", "GAAAA01").build()));

        assertThat(referenceDataClient.getProsecutorShortNameForOuCode(OUCODE), is("GAAAA01"));
    }

    @Test
    void returnsNullWhenShortNameAbsentFromResponse() {
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode").build(),
                Json.createObjectBuilder().build()));

        assertThat(referenceDataClient.getProsecutorShortNameForOuCode(OUCODE), is(nullValue()));
    }

    @Test
    void returnsNullWhenResponsePayloadIsNull() {
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode").build(),
                null));

        assertThat(referenceDataClient.getProsecutorShortNameForOuCode(OUCODE), is(nullValue()));
    }
}
