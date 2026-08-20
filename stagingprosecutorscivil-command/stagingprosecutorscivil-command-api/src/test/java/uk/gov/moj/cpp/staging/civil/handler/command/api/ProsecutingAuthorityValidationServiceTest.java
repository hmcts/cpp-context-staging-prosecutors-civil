package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.ProsecutorReferenceDataClient;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.UserGroupsClient;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProsecutingAuthorityValidationServiceTest {

    private static final String CALLING_USER_ID = randomUUID().toString();
    private static final String CSV_OUCODE = "GAFTL00";

    @InjectMocks
    private ProsecutingAuthorityValidationService service;

    @Mock
    private UserGroupsClient userGroupsClient;

    @Mock
    private ProsecutorReferenceDataClient prosecutorReferenceDataClient;

    @Test
    void throwsWhenGroupsListIsEmpty() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void throwsWhenNoGroupHasAnAuthorityAtAll() {
        // Groups exist but none carry a prosecutingAuthority - reject without ever calling
        // referencedata.
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJsonWithoutAuthority("Charging Lawyers")));

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void throwsWhenAuthorityDoesNotMatchResolvedShortName() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Charging Lawyers", "TFL")));
        when(prosecutorReferenceDataClient.getProsecutorShortNameForOuCode(CSV_OUCODE)).thenReturn("GAAAA01");

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void passesWhenAuthorityMatchesResolvedShortNameCaseInsensitively() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Charging Lawyers", "gaaaa01")));
        when(prosecutorReferenceDataClient.getProsecutorShortNameForOuCode(CSV_OUCODE)).thenReturn("GAAAA01");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenGroupIsLegalAdvisers() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Legal Advisers", "TFL")));

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenGroupIsCourtAdministrators() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Court Administrators", "TFL")));

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenGroupIsCourtAssociate() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Court Associate", "TFL")));

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenAnyGroupIsExemptEvenIfNotFirst() {
        // First group is a non-exempt mismatch; second is Legal Advisers - the caller belongs to
        // both, and any exempt group is enough to skip the check (and the referencedata call).
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Legal Advisers", "TFL")));

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenResolvedShortNameMatchesAnyGroupEvenIfNotFirst() {
        // First group's authority doesn't match the resolved short name; second group's does.
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Charging Lawyers", "GAAAA01")));
        when(prosecutorReferenceDataClient.getProsecutorShortNameForOuCode(CSV_OUCODE)).thenReturn("GAAAA01");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void throwsWhenNoGroupIsExemptAndNoAuthorityMatchesResolvedShortName() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Charging Lawyers", "OTHER")));
        when(prosecutorReferenceDataClient.getProsecutorShortNameForOuCode(CSV_OUCODE)).thenReturn("GAAAA01");

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    private JsonObject groupJson(final String groupName, final String prosecutingAuthority) {
        return Json.createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("groupName", groupName)
                .add("prosecutingAuthority", prosecutingAuthority)
                .build();
    }

    private JsonObject groupJsonWithoutAuthority(final String groupName) {
        return Json.createObjectBuilder()
                .add("groupId", randomUUID().toString())
                .add("groupName", groupName)
                .build();
    }
}
