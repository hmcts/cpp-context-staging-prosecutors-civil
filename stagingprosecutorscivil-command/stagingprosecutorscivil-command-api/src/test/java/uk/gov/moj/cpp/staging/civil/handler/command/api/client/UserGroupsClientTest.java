package uk.gov.moj.cpp.staging.civil.handler.command.api.client;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGroupsClientTest {

    private static final String USER_ID = randomUUID().toString();

    @InjectMocks
    private UserGroupsClient userGroupsClient;

    @Mock
    private Requester requester;

    @Test
    void returnsEmptyListWhenGroupsFieldAbsent() {
        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-groups").build(),
                Json.createObjectBuilder().build()));

        assertThat(userGroupsClient.getGroupsForUser(USER_ID), is(empty()));
    }

    @Test
    void returnsParsedGroups() {
        final JsonObject group = Json.createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("groupName", "Charging Lawyers")
                .add("prosecutingAuthority", "GAAAA01")
                .build();
        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-groups").build(),
                Json.createObjectBuilder().add("groups", Json.createArrayBuilder().add(group)).build()));

        final List<JsonObject> groups = userGroupsClient.getGroupsForUser(USER_ID);

        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getString("groupName"), is("Charging Lawyers"));
    }
}
