package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ProsecutingAuthorityValidationService service;

    @Mock
    private Requester requester;

    @Test
    void throwsWhenGroupsAreAbsentFromResponse() {
        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-groups").build(),
                Json.createObjectBuilder().build()));

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    @Test
    void throwsWhenGroupsListIsEmpty() {
        stubCallingUserGroups();

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    @Test
    void throwsWhenAuthorityDoesNotMatch() {
        stubCallingUserOrganisation("TFL");

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    @Test
    void passesWhenAuthorityMatchesCaseInsensitively() {
        stubCallingUserOrganisation("gaaaa01");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    @Test
    void doesNotThrowWhenGroupIsLegalAdvisers() {
        stubCallingUserGroup("63cae459-0e51-4d60-bcf8-c5324be50ba4", "TFL");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    @Test
    void doesNotThrowWhenGroupIsCourtAdmin() {
        stubCallingUserGroup("53292fc8-d164-4a6c-8722-cdbc795cf83a", "TFL");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    @Test
    void doesNotThrowWhenGroupIsCourtAssociate() {
        stubCallingUserGroup("ebcfdd9c-9605-4fbf-b9f3-85f8cfdd11bb", "TFL");

        assertDoesNotThrow(() -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    @Test
    void onlyConsidersFirstGroupWhenCallerHasMultipleGroups() {
        // First group is a non-exempt mismatch; second is Legal Advisers with a matching
        // authority - only the first is consulted, so this must still throw.
        stubCallingUserGroups(
                groupJson(randomUUID().toString(), "TFL"),
                groupJson("63cae459-0e51-4d60-bcf8-c5324be50ba4", "GAAAA01"));

        assertThrows(BadRequestException.class,
                () -> service.validateCallingUserBelongsToProsecutingAuthority(CALLING_USER_ID, "GAAAA01"));
    }

    private void stubCallingUserOrganisation(final String prosecutingAuthority) {
        stubCallingUserGroup(randomUUID().toString(), prosecutingAuthority);
    }

    private void stubCallingUserGroup(final String groupId, final String prosecutingAuthority) {
        stubCallingUserGroups(groupJson(groupId, prosecutingAuthority));
    }

    private JsonObject groupJson(final String groupId, final String prosecutingAuthority) {
        return Json.createObjectBuilder()
                .add("groupId", groupId)
                .add("groupName", "Charging Lawyers")
                .add("prosecutingAuthority", prosecutingAuthority)
                .build();
    }

    private void stubCallingUserGroups(final JsonObject... groups) {
        final JsonArrayBuilder groupsArrayBuilder = Json.createArrayBuilder();
        for (final JsonObject group : groups) {
            groupsArrayBuilder.add(group);
        }
        final JsonEnvelope groupsResponse = JsonEnvelope.envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-groups").build(),
                Json.createObjectBuilder().add("groups", groupsArrayBuilder).build());
        when(requester.request(any())).thenReturn(groupsResponse);
    }
}
