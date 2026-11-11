package uk.gov.moj.cpp.staging.civil.query.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.staging.civil.query.api.client.ReferenceDataClient;
import uk.gov.moj.cpp.staging.civil.query.api.client.UserGroupsClient;

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
    private static final String OUCODE = "GAFTL00";
    private static final String PROSECUTOR_SHORT_NAME = "DVLA";

    @InjectMocks
    private ProsecutingAuthorityValidationService service;

    @Mock
    private UserGroupsClient userGroupsClient;

    @Mock
    private ReferenceDataClient referenceDataClient;

    @Test
    void returnsFalseWhenGroupsListIsEmpty() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of());

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(false));
    }

    @Test
    void returnsFalseWhenNoGroupHasAnAuthorityAtAll() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJsonWithoutAuthority("Charging Lawyers")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(false));
    }

    @Test
    void returnsFalseWhenAuthorityDoesNotMatchStoredProsecutorShortName() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Charging Lawyers", "TFL")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(false));
    }

    @Test
    void returnsTrueWhenAuthorityMatchesStoredProsecutorShortNameCaseInsensitively() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Charging Lawyers", "dvla")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(true));
        verify(referenceDataClient, never()).getProsecutorShortNameForOuCode(OUCODE);
    }

    @Test
    void resolvesProsecutorShortNameFromOuCodeWhenNotAlreadyPersisted() {
        // regular (non-CSV) submissions never persist prosecutorShortName - only ouCode - so the
        // check must resolve it itself, the same way the CSV upload path does at write time.
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Charging Lawyers", PROSECUTOR_SHORT_NAME)));
        when(referenceDataClient.getProsecutorShortNameForOuCode(OUCODE)).thenReturn(PROSECUTOR_SHORT_NAME);

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, null), is(true));
        verify(referenceDataClient).getProsecutorShortNameForOuCode(OUCODE);
    }

    @Test
    void returnsTrueWhenGroupIsLegalAdvisers() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Legal Advisers", "TFL")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(true));
        verify(referenceDataClient, never()).getProsecutorShortNameForOuCode(OUCODE);
    }

    @Test
    void returnsTrueWhenGroupIsCourtAdministrators() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Court Administrators", "TFL")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(true));
    }

    @Test
    void returnsTrueWhenGroupIsCourtAssociate() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID))
                .thenReturn(List.of(groupJson("Court Associate", "TFL")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(true));
    }

    @Test
    void returnsTrueWhenAnyGroupIsExemptEvenIfNotFirst() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Legal Advisers", "TFL")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(true));
    }

    @Test
    void returnsTrueWhenStoredProsecutorShortNameMatchesAnyGroupEvenIfNotFirst() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Charging Lawyers", PROSECUTOR_SHORT_NAME)));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(true));
    }

    @Test
    void returnsFalseWhenNoGroupIsExemptAndNoAuthorityMatchesStoredProsecutorShortName() {
        when(userGroupsClient.getGroupsForUser(CALLING_USER_ID)).thenReturn(List.of(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Charging Lawyers", "OTHER")));

        assertThat(service.callerBelongsToProsecutingAuthority(CALLING_USER_ID, OUCODE, PROSECUTOR_SHORT_NAME), is(false));
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
