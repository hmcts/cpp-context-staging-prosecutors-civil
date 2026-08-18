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

import java.util.Set;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CAD-1613: deliberately NOT an {@code @Adapter} class. A {@code Requester} injected from an
 * {@code @Adapter} class always resolves to the LOCAL dispatcher
 * ({@code ServiceComponentLocation.componentLocationFrom(InjectionPoint)} keys on the declaring
 * class carrying {@code @Adapter}/{@code @CustomAdapter}/{@code @DirectAdapter}), but the
 * generated {@code usersgroups} REST client is registered under REMOTE (it carries
 * {@code @Remote}). Only a plain, non-adapter class's {@code Requester} resolves to REMOTE and can
 * actually reach it — mirrors {@code cpp-context-progression}'s {@code UserGroupQueryService}.
 */
public class ProsecutingAuthorityValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutingAuthorityValidationService.class);

    private static final String USERSGROUPS_GET_LOGGED_IN_USER_GROUPS = "usersgroups.get-logged-in-user-groups";
    private static final String GROUPS_FIELD = "groups";
    private static final String USER_ID_FIELD = "userId";
    private static final String GROUP_ID_FIELD = "groupId";
    private static final String PROSECUTING_AUTHORITY_FIELD = "prosecutingAuthority";

    private static final String LEGAL_ADVISERS_GROUP_ID = "63cae459-0e51-4d60-bcf8-c5324be50ba4";
    private static final String COURT_ADMIN_GROUP_ID = "53292fc8-d164-4a6c-8722-cdbc795cf83a";
    private static final String COURT_ASSOCIATE_GROUP_ID = "ebcfdd9c-9605-4fbf-b9f3-85f8cfdd11bb";
    private static final Set<String> PROSECUTING_AUTHORITY_CHECK_EXEMPT_GROUP_IDS =
            Set.of(LEGAL_ADVISERS_GROUP_ID, COURT_ADMIN_GROUP_ID, COURT_ASSOCIATE_GROUP_ID);

    @Inject
    @ServiceComponent(Component.COMMAND_API)
    private Requester requester;

    /**
     * CAD-1525 AC1 validation #3, extended by CAD-1613: the prosecuting authority on the uploaded
     * CSV must match the calling user's own organisation, unless the caller belongs to one of the
     * hardcoded HMCTS groups exempted from this check (Legal Advisers, Court Admin, Court
     * Associate). Only the caller's first returned group is consulted, for both the exemption
     * check and the prosecuting-authority comparison. {@code callingUserId} must already be a
     * validated, non-blank user id (the caller's responsibility).
     */
    public void validateCallingUserBelongsToProsecutingAuthority(final String callingUserId, final String csvProsecutingAuthority) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(USERSGROUPS_GET_LOGGED_IN_USER_GROUPS)
                .build();
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add(USER_ID_FIELD, callingUserId)
                .build();

        final JsonEnvelope response = requester.request(envelopeFrom(metadata, queryPayload));
        final JsonArray groups = response.payloadAsJsonObject().getJsonArray(GROUPS_FIELD);

        if (groups == null || groups.isEmpty()) {
            rejectProsecutingAuthorityMismatch(csvProsecutingAuthority, callingUserId, null);
            return;
        }

        final JsonObject callingUserFirstGroup = groups.getJsonObject(0);
        final String callingUserGroupId = callingUserFirstGroup.getString(GROUP_ID_FIELD, null);

        if (PROSECUTING_AUTHORITY_CHECK_EXEMPT_GROUP_IDS.contains(callingUserGroupId)) {
            return;
        }

        final String callingUserProsecutingAuthority =
                callingUserFirstGroup.getString(PROSECUTING_AUTHORITY_FIELD, null);

        if (callingUserProsecutingAuthority == null
                || !callingUserProsecutingAuthority.equalsIgnoreCase(csvProsecutingAuthority)) {
            rejectProsecutingAuthorityMismatch(csvProsecutingAuthority, callingUserId, callingUserProsecutingAuthority);
        }
    }

    private void rejectProsecutingAuthorityMismatch(final String csvProsecutingAuthority, final String callingUserId,
            final String callingUserProsecutingAuthority) {
        LOGGER.warn("Complaints CSV prosecuting authority '{}' does not match calling user {}'s organisation '{}'",
                csvProsecutingAuthority, callingUserId, callingUserProsecutingAuthority);
        throw new BadRequestException("The uploaded complaints file's prosecuting authority ('"
                + csvProsecutingAuthority + "') does not match the calling user's organisation");
    }
}
