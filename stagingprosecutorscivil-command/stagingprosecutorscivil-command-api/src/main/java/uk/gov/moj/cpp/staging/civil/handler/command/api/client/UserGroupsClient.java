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

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Reusable cross-context client for {@code usersgroups.get-logged-in-user-groups} - mirrors
 * {@code cpp-context-progression}'s {@code UserGroupQueryService} and
 * {@code cpp-context-results}' {@code ReferenceDataService} conventions: build the request via
 * {@code JsonEnvelope.envelopeFrom} (not {@code Envelope.envelopeFrom} - the latter's runtime
 * type does not implement {@code JsonEnvelope}) and read the typed response via the
 * {@code Requester.request(envelope, Class)} overload. Deliberately not an {@code @Adapter}
 * class - a {@code Requester} injected from an {@code @Adapter} always resolves to the
 * framework's LOCAL dispatcher regardless of {@code @ServiceComponent} qualifier, while the
 * generated cross-context REST client is registered under REMOTE (CAD-1613 - see the KB
 * workspace's ADR-002 for the full framework-bytecode evidence). Not "AsAdmin": this query is
 * inherently scoped to the calling user's own identity, unlike a general reference-data lookup.
 */
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
        final JsonEnvelope requestEnvelope = envelopeFrom(metadata, queryPayload);

        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        final JsonArray groups = response.payload().getJsonArray(GROUPS_FIELD);

        return groups == null ? List.of() : groups.stream().map(JsonValue::asJsonObject).toList();
    }
}
