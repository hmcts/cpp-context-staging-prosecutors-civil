package uk.gov.moj.cpp.staging.civil.handler.command.api;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.ReferenceDataClient;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.UserGroupsClient;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutingAuthorityValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutingAuthorityValidationService.class);

    private static final String GROUP_NAME_FIELD = "groupName";
    private static final String PROSECUTING_AUTHORITY_FIELD = "prosecutingAuthority";

    private static final String LEGAL_ADVISERS_GROUP_NAME = "Legal Advisers";
    private static final String COURT_ADMINISTRATORS_GROUP_NAME = "Court Administrators";
    private static final String COURT_ASSOCIATE_GROUP_NAME = "Court Associate";
    private static final Set<String> PROSECUTING_AUTHORITY_CHECK_EXEMPT_GROUP_NAMES =
            Set.of(LEGAL_ADVISERS_GROUP_NAME, COURT_ADMINISTRATORS_GROUP_NAME, COURT_ASSOCIATE_GROUP_NAME);

    @Inject
    private UserGroupsClient userGroupsClient;

    /**
     * CAD-1525 AC1 validation #3, extended by CAD-1613: the prosecuting authority on the uploaded
     * CSV must match the calling user's own organisation, unless the caller belongs to one of the
     * hardcoded HMCTS groups exempted from this check (Legal Advisers, Court Administrators,
     * Court Associate — matched by group name). A caller can belong to multiple groups: the
     * exemption is granted if ANY group's name matches. If the caller is not exempt and none of
     * their groups carry a prosecuting authority at all, the request is rejected outright.
     * Otherwise the caller's own organisation must match ANY one of their groups' prosecuting
     * authorities. {@code callingUserId} must already be a validated, non-blank user id (the
     * caller's responsibility).
     *
     * <p>{@code prosecutorShortName} is the CSV's ou code already resolved by the caller (via
     * {@code ReferenceDataClient}) — this method never resolves it itself, so the caller only
     * ever makes that reference-data call once, regardless of whether this check ends up applying
     * or being skipped for an exempt caller.
     */
    public void validateCallingUserBelongsToProsecutingAuthority(final String callingUserId, final String csvOuCode,
            final String prosecutorShortName) {
        final List<JsonObject> callingUserGroups = userGroupsClient.getGroupsForUser(callingUserId);
        LOGGER.info("User {} belongs to {} group(s): {}", callingUserId, callingUserGroups.size(),
                callingUserGroups.stream().map(group -> group.getString(GROUP_NAME_FIELD, null)).toList());

        final boolean callerIsExempt = callingUserGroups.stream()
                .map(group -> group.getString(GROUP_NAME_FIELD, null))
                .anyMatch(PROSECUTING_AUTHORITY_CHECK_EXEMPT_GROUP_NAMES::contains);

        if (callerIsExempt) {
            LOGGER.info("User {} is exempt from the prosecuting authority check via group membership", callingUserId);
            return;
        }

        final List<String> callingUserProsecutingAuthorities = callingUserGroups.stream()
                .map(group -> group.getString(PROSECUTING_AUTHORITY_FIELD, null))
                .filter(Objects::nonNull)
                .toList();
        LOGGER.info("User {} has prosecuting authorities {}", callingUserId, callingUserProsecutingAuthorities);

        if (callingUserProsecutingAuthorities.isEmpty()) {
            rejectProsecutingAuthorityMismatch(csvOuCode, callingUserId, null);
            return;
        }

        LOGGER.info("Resolved CSV ou code '{}' to prosecutor short name '{}'", csvOuCode, prosecutorShortName);

        final boolean authorityMatches = callingUserProsecutingAuthorities.stream()
                .anyMatch(authority -> authority.equalsIgnoreCase(prosecutorShortName));

        if (!authorityMatches) {
            rejectProsecutingAuthorityMismatch(csvOuCode, callingUserId, callingUserProsecutingAuthorities.get(0));
            return;
        }

        LOGGER.info("Prosecuting authority check passed for user {} against CSV ou code '{}'", callingUserId, csvOuCode);
    }

    private void rejectProsecutingAuthorityMismatch(final String csvOuCode, final String callingUserId,
            final String callingUserProsecutingAuthority) {
        LOGGER.warn("Complaints CSV ou code '{}' does not match calling user {}'s organisation '{}'",
                csvOuCode, callingUserId, callingUserProsecutingAuthority);
        throw new BadRequestException("The uploaded complaints file's prosecuting authority ('"
                + csvOuCode + "') does not match the calling user's organisation");
    }
}

