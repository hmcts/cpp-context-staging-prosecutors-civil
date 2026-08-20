package uk.gov.moj.cpp.staging.civil.handler.command.api.client;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class UserGroupsClient {

    private static final String USERSGROUPS_GET_LOGGED_IN_USER_GROUPS = "usersgroups.get-logged-in-user-groups";
    private static final String GROUPS_FIELD = "groups";
    private static final String USER_ID_FIELD = "userId";

    @Inject
    @ServiceComponent(Component.COMMAND_API)
    private Requester requester;

    public List<JsonObject> getGroupsForUser(final String userId) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(USERSGROUPS_GET_LOGGED_IN_USER_GROUPS)
                .build();
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add(USER_ID_FIELD, userId)
                .build();

        final JsonEnvelope response = requester.request(envelopeFrom(metadata, queryPayload));
        final JsonArray groups = response.payloadAsJsonObject().getJsonArray(GROUPS_FIELD);

        return groups == null ? List.of() : groups.stream().map(JsonValue::asJsonObject).toList();
    }
}
