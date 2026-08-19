package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
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
    private Requester requester;

    @Test
    void throwsWhenGroupsAreAbsentFromResponse() {
        stubCallingUserGroupsResponse(Json.createObjectBuilder().build());

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void throwsWhenGroupsListIsEmpty() {
        stubCallingUserGroups();

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void throwsWhenNoGroupHasAnAuthorityAtAll() {
        // Groups exist but none carry a prosecutingAuthority - reject without ever calling
        // referencedata.
        stubCallingUserGroups(groupJsonWithoutAuthority("Charging Lawyers"));

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void throwsWhenAuthorityDoesNotMatchResolvedShortName() {
        stubCallingUserOrganisation("TFL");
        stubProsecutorShortName("GAAAA01");

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void passesWhenAuthorityMatchesResolvedShortNameCaseInsensitively() {
        stubCallingUserOrganisation("gaaaa01");
        stubProsecutorShortName("GAAAA01");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenGroupIsLegalAdvisers() {
        stubCallingUserGroup("Legal Advisers", "TFL");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenGroupIsCourtAdministrators() {
        stubCallingUserGroup("Court Administrators", "TFL");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenGroupIsCourtAssociate() {
        stubCallingUserGroup("Court Associate", "TFL");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenAnyGroupIsExemptEvenIfNotFirst() {
        // First group is a non-exempt mismatch; second is Legal Advisers - the caller belongs to
        // both, and any exempt group is enough to skip the check (and the referencedata call).
        stubCallingUserGroups(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Legal Advisers", "TFL"));

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void doesNotThrowWhenResolvedShortNameMatchesAnyGroupEvenIfNotFirst() {
        // First group's authority doesn't match the resolved short name; second group's does.
        stubCallingUserGroups(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Charging Lawyers", "GAAAA01"));
        stubProsecutorShortName("GAAAA01");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    @Test
    void throwsWhenNoGroupIsExemptAndNoAuthorityMatchesResolvedShortName() {
        stubCallingUserGroups(
                groupJson("Charging Lawyers", "TFL"),
                groupJson("Charging Lawyers", "OTHER"));
        stubProsecutorShortName("GAAAA01");

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, CSV_OUCODE));
    }

    private void stubCallingUserOrganisation(final String prosecutingAuthority) {
        stubCallingUserGroup("Charging Lawyers", prosecutingAuthority);
    }

    private void stubCallingUserGroup(final String groupName, final String prosecutingAuthority) {
        stubCallingUserGroups(groupJson(groupName, prosecutingAuthority));
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

    private void stubCallingUserGroups(final JsonObject... groups) {
        final JsonArrayBuilder groupsArrayBuilder = Json.createArrayBuilder();
        for (final JsonObject group : groups) {
            groupsArrayBuilder.add(group);
        }
        stubCallingUserGroupsResponse(Json.createObjectBuilder().add("groups", groupsArrayBuilder).build());
    }

    private void stubCallingUserGroupsResponse(final JsonObject payload) {
        final JsonEnvelope groupsResponse = JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-groups").build(),
                payload);
        when(requester.request(argThat(envelope ->
                envelope != null && "usersgroups.get-logged-in-user-groups".equals(envelope.metadata().name()))))
                .thenReturn(groupsResponse);
    }

    private void stubProsecutorShortName(final String shortName) {
        final JsonEnvelope prosecutorResponse = JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode").build(),
                Json.createObjectBuilder().add("shortName", shortName).build());
        when(requester.request(argThat(envelope ->
                envelope != null && "referencedata.query.get.prosecutor.by.oucode".equals(envelope.metadata().name()))))
                .thenReturn(prosecutorResponse);
    }
}
