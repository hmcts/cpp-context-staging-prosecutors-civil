package uk.gov.moj.cpp.staging.civil.query.api;

import uk.gov.moj.cpp.staging.civil.query.api.client.ReferenceDataClient;
import uk.gov.moj.cpp.staging.civil.query.api.client.UserGroupsClient;

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

    @Inject
    private ReferenceDataClient referenceDataClient;

    /**
     * @param callingUserId       must already be a validated, non-blank user id (the caller's
     *                            responsibility)
     * @param ouCode              the submission's persisted {@code Submission.ouCode}
     * @param prosecutorShortName the submission's persisted {@code Submission.prosecutorShortName},
     *                            or {@code null} if not captured at submission time (resolved from
     *                            {@code ouCode} in that case)
     * @return {@code true} if the caller may view the submission: either exempt via group
     * membership, or belonging to the prosecuting authority the submission was made for
     */
    public boolean callerBelongsToProsecutingAuthority(final String callingUserId, final String ouCode,
            final String prosecutorShortName) {
        final List<JsonObject> callingUserGroups = userGroupsClient.getGroupsForUser(callingUserId);
        LOGGER.info("User {} belongs to {} group(s): {}", callingUserId, callingUserGroups.size(),
                callingUserGroups.stream().map(group -> group.getString(GROUP_NAME_FIELD, null)).toList());

        final boolean callerIsExempt = callingUserGroups.stream()
                .map(group -> group.getString(GROUP_NAME_FIELD, null))
                .anyMatch(PROSECUTING_AUTHORITY_CHECK_EXEMPT_GROUP_NAMES::contains);

        if (callerIsExempt) {
            LOGGER.info("User {} is exempt from the prosecuting authority check via group membership", callingUserId);
            return true;
        }

        final List<String> callingUserProsecutingAuthorities = callingUserGroups.stream()
                .map(group -> group.getString(PROSECUTING_AUTHORITY_FIELD, null))
                .filter(Objects::nonNull)
                .toList();

        if (callingUserProsecutingAuthorities.isEmpty()) {
            LOGGER.warn("User {} has no prosecuting authority in any group membership - denying access to submission ou code '{}'",
                    callingUserId, ouCode);
            return false;
        }

        final String resolvedProsecutorShortName = prosecutorShortName != null
                ? prosecutorShortName
                : referenceDataClient.getProsecutorShortNameForOuCode(ouCode);
        LOGGER.info("Resolved ou code '{}' to prosecutor short name '{}'", ouCode, resolvedProsecutorShortName);

        final boolean authorityMatches = callingUserProsecutingAuthorities.stream()
                .anyMatch(authority -> authority.equalsIgnoreCase(resolvedProsecutorShortName));

        if (!authorityMatches) {
            LOGGER.warn("User {}'s organisation(s) {} do not match submission ou code '{}' (resolved '{}') - denying access",
                    callingUserId, callingUserProsecutingAuthorities, ouCode, resolvedProsecutorShortName);
            return false;
        }

        LOGGER.info("Prosecuting authority check passed for user {} against ou code '{}'", callingUserId, ouCode);
        return true;
    }
}
