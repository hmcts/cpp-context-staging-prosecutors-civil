package uk.gov.moj.cpp.staging.civil.handler.command.api.client;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class UserGroupsClient {

    private static final String USERSGROUPS_GET_LOGGED_IN_USER_GROUPS = "usersgroups.get-logged-in-user-groups";
    private static final String USERSGROUPS_GET_LOGGED_IN_USER_DETAILS = "usersgroups.get-logged-in-user-details";
    private static final String GROUPS_FIELD = "groups";
    private static final String USER_ID_FIELD = "userId";
    private static final String FIRST_NAME_FIELD = "firstName";
    private static final String LAST_NAME_FIELD = "lastName";

    @Inject
    @ServiceComponent(Component.COMMAND_API)
    private Requester requester;

    public List<JsonObject> getGroupsForUser(final String userId) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(USERSGROUPS_GET_LOGGED_IN_USER_GROUPS)
                .withUserId(userId)
                .build();
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add(USER_ID_FIELD, userId)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadata, queryPayload);

        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        final JsonArray groups = response.payload().getJsonArray(GROUPS_FIELD);

        return groups == null ? List.of() : groups.stream().map(JsonValue::asJsonObject).toList();
    }

    public Optional<String> getDisplayNameForUser(final String userId) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(USERSGROUPS_GET_LOGGED_IN_USER_DETAILS)
                .withUserId(userId)
                .build();
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add(USER_ID_FIELD, userId)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadata, queryPayload);

        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        final JsonObject userDetails = response.payload();

        if (userDetails == null) {
            return Optional.empty();
        }

        final String firstName = userDetails.getString(FIRST_NAME_FIELD, null);
        final String lastName = userDetails.getString(LAST_NAME_FIELD, null);

        return composeDisplayName(firstName, lastName);
    }

    private Optional<String> composeDisplayName(final String firstName, final String lastName) {
        final String trimmedFirstName = firstName == null ? "" : firstName.trim();
        final String trimmedLastName = lastName == null ? "" : lastName.trim();

        final String displayName = (trimmedFirstName + " " + trimmedLastName).trim();

        return displayName.isEmpty() ? Optional.empty() : Optional.of(displayName);
    }
}
