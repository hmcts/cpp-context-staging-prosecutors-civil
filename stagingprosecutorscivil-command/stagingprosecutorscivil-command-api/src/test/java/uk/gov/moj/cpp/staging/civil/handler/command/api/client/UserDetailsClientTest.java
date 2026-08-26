package uk.gov.moj.cpp.staging.civil.handler.command.api.client;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;

import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDetailsClientTest {

    private static final String USER_ID = randomUUID().toString();

    @InjectMocks
    private UserDetailsClient userDetailsClient;

    @Mock
    private Requester requester;

    @Test
    void returnsComposedDisplayNameWhenFirstAndLastNamePresent() {
        when(requester.request(any(), eq(JsonObject.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-details").build(),
                Json.createObjectBuilder()
                        .add("firstName", "Richard")
                        .add("lastName", "Chapman")
                        .add("email", "richard.chapman@acme.com")
                        .build()));

        assertThat(userDetailsClient.getDisplayNameForUser(USER_ID), is(Optional.of("Richard Chapman")));
    }

    @Test
    void returnsEmptyWhenNeitherNamePresent() {
        when(requester.request(any(), eq(JsonObject.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-details").build(),
                Json.createObjectBuilder().build()));

        assertThat(userDetailsClient.getDisplayNameForUser(USER_ID), is(Optional.empty()));
    }

    @Test
    void returnsEmptyWhenResponsePayloadIsNull() {
        when(requester.request(any(), eq(JsonObject.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-details").build(),
                null));

        assertThat(userDetailsClient.getDisplayNameForUser(USER_ID), is(Optional.empty()));
    }
}
