package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutingAuthorityValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutingAuthorityValidationService.class);

    private static final String USERSGROUPS_GET_LOGGED_IN_USER_GROUPS = "usersgroups.get-logged-in-user-groups";
    private static final String GROUPS_FIELD = "groups";
    private static final String USER_ID_FIELD = "userId";
    private static final String GROUP_NAME_FIELD = "groupName";
    private static final String PROSECUTING_AUTHORITY_FIELD = "prosecutingAuthority";

    private static final String REFERENCEDATA_GET_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String OUCODE_FIELD = "oucode";
    private static final String SHORT_NAME_FIELD = "shortName";

    private static final String LEGAL_ADVISERS_GROUP_NAME = "Legal Advisers";
    private static final String COURT_ADMINISTRATORS_GROUP_NAME = "Court Administrators";
    private static final String COURT_ASSOCIATE_GROUP_NAME = "Court Associate";
    private static final Set<String> PROSECUTING_AUTHORITY_CHECK_EXEMPT_GROUP_NAMES =
            Set.of(LEGAL_ADVISERS_GROUP_NAME, COURT_ADMINISTRATORS_GROUP_NAME, COURT_ASSOCIATE_GROUP_NAME);

    @Inject
    @ServiceComponent(Component.COMMAND_API)
    private Requester requester;

    /**
     * CAD-1525 AC1 validation #3, extended by CAD-1613: the prosecuting authority on the uploaded
     * CSV must match the calling user's own organisation, unless the caller belongs to one of the
     * hardcoded HMCTS groups exempted from this check (Legal Advisers, Court Administrators,
     * Court Associate — matched by group name). A caller can belong to multiple groups: the
     * exemption is granted if ANY group's name matches. If the caller is not exempt and none of
     * their groups carry a prosecuting authority at all, the request is rejected outright. If at
     * least one group does, the CSV's own ou code (read from its first row) is resolved to a
     * prosecutor short name via {@code referencedata.query.get.prosecutor.by.oucode}, and that
     * short name must match ANY one of the caller's groups' prosecuting authorities.
     * {@code callingUserId} must already be a validated, non-blank user id (the caller's
     * responsibility).
     */
    public void validateCallingUserBelongsToProsecutingAuthority(final String callingUserId, final String csvOuCode) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(USERSGROUPS_GET_LOGGED_IN_USER_GROUPS)
                .build();
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add(USER_ID_FIELD, callingUserId)
                .build();

        final JsonEnvelope response = requester.request(envelopeFrom(metadata, queryPayload));
        final JsonArray groups = response.payloadAsJsonObject().getJsonArray(GROUPS_FIELD);
        final List<JsonObject> callingUserGroups = groups == null
                ? List.of()
                : groups.stream().map(JsonValue::asJsonObject).toList();

        final boolean callerIsExempt = callingUserGroups.stream()
                .map(group -> group.getString(GROUP_NAME_FIELD, null))
                .anyMatch(PROSECUTING_AUTHORITY_CHECK_EXEMPT_GROUP_NAMES::contains);

        if (callerIsExempt) {
            return;
        }

        final List<String> callingUserProsecutingAuthorities = callingUserGroups.stream()
                .map(group -> group.getString(PROSECUTING_AUTHORITY_FIELD, null))
                .filter(Objects::nonNull)
                .toList();

        if (callingUserProsecutingAuthorities.isEmpty()) {
            rejectProsecutingAuthorityMismatch(csvOuCode, callingUserId, null);
            return;
        }

        final String prosecutorShortName = resolveProsecutorShortName(csvOuCode);

        final boolean authorityMatches = callingUserProsecutingAuthorities.stream()
                .anyMatch(authority -> authority.equalsIgnoreCase(prosecutorShortName));

        if (!authorityMatches) {
            rejectProsecutingAuthorityMismatch(csvOuCode, callingUserId, callingUserProsecutingAuthorities.get(0));
        }
    }

    private String resolveProsecutorShortName(final String ouCode) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_GET_PROSECUTOR_BY_OUCODE)
                .build();
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add(OUCODE_FIELD, ouCode)
                .build();

        final JsonEnvelope response = requester.request(envelopeFrom(metadata, queryPayload));
        return response.payloadAsJsonObject().getString(SHORT_NAME_FIELD, null);
    }

    private void rejectProsecutingAuthorityMismatch(final String csvOuCode, final String callingUserId,
            final String callingUserProsecutingAuthority) {
        LOGGER.warn("Complaints CSV ou code '{}' does not match calling user {}'s organisation '{}'",
                csvOuCode, callingUserId, callingUserProsecutingAuthority);
        throw new BadRequestException("The uploaded complaints file's prosecuting authority ('"
                + csvOuCode + "') does not match the calling user's organisation");
    }
}
