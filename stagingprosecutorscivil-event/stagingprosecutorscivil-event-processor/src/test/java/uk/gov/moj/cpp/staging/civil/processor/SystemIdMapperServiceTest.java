package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.accesscontrol.AccessControlViolationException;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponses;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMappings;
import uk.gov.moj.cpp.systemidmapper.client.SystemidMapList;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemIdMapperServiceTest {

    private static final String PROSECUTOR_CASE_REFERENCE = "12AB0000000";
    private static final String PROSECUTING_AUTHORITY = "CPS";
    private static final String SOURCE_TYPE = "OU_URN";
    private static final String TARGET_TYPE = "CASE_FILE_ID";
    private static final String SPI_SOURCE_TYPE = "SPI-URN";
    private static final String SPI_TARGET_TYPE = "CASE-ID";

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @InjectMocks
    private SystemIdMapperService target;

    @Test
    void shouldReturnCaseIdWhenOuUrnMappingFound() {
        final UUID userId = randomUUID();
        final UUID expectedCaseId = randomUUID();
        final SystemIdMapping mapping = new SystemIdMapping(randomUUID(), PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, expectedCaseId, TARGET_TYPE, ZonedDateTime.now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.of(mapping));

        final UUID result = target.getCppCaseIdFor(PROSECUTOR_CASE_REFERENCE, PROSECUTING_AUTHORITY);

        assertThat(result, is(expectedCaseId));
    }

    @Test
    void shouldReturnCaseIdWhenSpiMappingFoundAndOuUrnNotFound() {
        final UUID userId = randomUUID();
        final UUID expectedCaseId = randomUUID();
        final SystemIdMapping spiMapping = new SystemIdMapping(randomUUID(), PROSECUTOR_CASE_REFERENCE, SPI_SOURCE_TYPE, expectedCaseId, SPI_TARGET_TYPE, ZonedDateTime.now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.of(spiMapping));

        final UUID result = target.getCppCaseIdFor(PROSECUTOR_CASE_REFERENCE, PROSECUTING_AUTHORITY);

        assertThat(result, is(expectedCaseId));
    }

    @Test
    void shouldReturnCaseIdFromCombinedReferenceWhenOuUrnAndSpiMappingsNotFound() {
        final UUID userId = randomUUID();
        final UUID expectedCaseId = randomUUID();
        final String combinedRef = PROSECUTING_AUTHORITY + ":" + PROSECUTOR_CASE_REFERENCE;
        final SystemIdMapping combinedMapping = new SystemIdMapping(randomUUID(), combinedRef, SOURCE_TYPE, expectedCaseId, TARGET_TYPE, ZonedDateTime.now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(combinedRef, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.of(combinedMapping));

        final UUID result = target.getCppCaseIdFor(PROSECUTOR_CASE_REFERENCE, PROSECUTING_AUTHORITY);

        assertThat(result, is(expectedCaseId));
    }

    @Test
    void shouldGenerateNewCaseIdAndAddMappingWhenNoExistingMappingFound() {
        final UUID userId = randomUUID();
        final String combinedRef = PROSECUTING_AUTHORITY + ":" + PROSECUTOR_CASE_REFERENCE;
        final AdditionResponse successResponse = new AdditionResponse(randomUUID(), ResultCode.OK, Optional.empty());
        final ArgumentCaptor<SystemIdMap> systemIdMapCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(combinedRef, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(any(SystemIdMap.class), eq(userId))).thenReturn(successResponse);

        final UUID result = target.getCppCaseIdFor(PROSECUTOR_CASE_REFERENCE, PROSECUTING_AUTHORITY);

        verify(systemIdMapperClient).add(systemIdMapCaptor.capture(), eq(userId));
        final SystemIdMap capturedMap = systemIdMapCaptor.getValue();
        assertThat(result, is(capturedMap.getTargetId()));
        assertThat(capturedMap.getSourceId(), is(PROSECUTOR_CASE_REFERENCE));
        assertThat(capturedMap.getSourceType(), is(SOURCE_TYPE));
        assertThat(capturedMap.getTargetType(), is(TARGET_TYPE));
    }

    @Test
    void shouldReturnCaseIdFromRetryLookupWhenAddMappingFails() {
        final UUID userId = randomUUID();
        final UUID expectedCaseId = randomUUID();
        final String combinedRef = PROSECUTING_AUTHORITY + ":" + PROSECUTOR_CASE_REFERENCE;
        final AdditionResponse failureResponse = new AdditionResponse(randomUUID(), ResultCode.CONFLICT, Optional.of("conflict"));
        final SystemIdMapping retryMapping = new SystemIdMapping(randomUUID(), PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, expectedCaseId, TARGET_TYPE, ZonedDateTime.now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, TARGET_TYPE, userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(retryMapping));
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(combinedRef, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(any(SystemIdMap.class), eq(userId))).thenReturn(failureResponse);

        final UUID result = target.getCppCaseIdFor(PROSECUTOR_CASE_REFERENCE, PROSECUTING_AUTHORITY);

        assertThat(result, is(expectedCaseId));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenAddMappingFailsAndRetryLookupReturnsEmpty() {
        final UUID userId = randomUUID();
        final String combinedRef = PROSECUTING_AUTHORITY + ":" + PROSECUTOR_CASE_REFERENCE;
        final AdditionResponse failureResponse = new AdditionResponse(randomUUID(), ResultCode.CONFLICT, Optional.of("conflict"));

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(PROSECUTOR_CASE_REFERENCE, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(combinedRef, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(any(SystemIdMap.class), eq(userId))).thenReturn(failureResponse);

        assertThrows(IllegalStateException.class, () -> target.getCppCaseIdFor(PROSECUTOR_CASE_REFERENCE, PROSECUTING_AUTHORITY));
    }

    @Test
    void shouldThrowAccessControlViolationExceptionWhenNoSystemUserContextForGetCppCaseIdFor() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.empty());

        assertThrows(AccessControlViolationException.class, () -> target.getCppCaseIdFor(PROSECUTOR_CASE_REFERENCE, PROSECUTING_AUTHORITY));
    }

    @Test
    void shouldReturnCaseIdMapForAllSuccessfulMappings() {
        final UUID userId = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final SystemIdMappings mapping1 = new SystemIdMappings(null, false, randomUUID(), "ref1", caseId1);
        final SystemIdMappings mapping2 = new SystemIdMappings(null, false, randomUUID(), "ref2", caseId2);
        final AdditionResponses responses = new AdditionResponses(List.of(mapping1, mapping2));

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.addMany(any(SystemidMapList.class), eq(userId))).thenReturn(responses);

        final Map<String, UUID> result = target.getCppCaseIdMapFor(List.of("ref1", "ref2"), null);

        assertThat(result.size(), is(2));
        assertThat(result.get("ref1"), is(caseId1));
        assertThat(result.get("ref2"), is(caseId2));
    }

    @Test
    void shouldExcludeErrorMappingsFromReturnedCaseIdMap() {
        final UUID userId = randomUUID();
        final UUID caseId1 = randomUUID();
        final SystemIdMappings successMapping = new SystemIdMappings(null, false, randomUUID(), "ref1", caseId1);
        final SystemIdMappings errorMapping = new SystemIdMappings("some error", true, null, "ref2", null);
        final AdditionResponses responses = new AdditionResponses(List.of(successMapping, errorMapping));

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.addMany(any(SystemidMapList.class), eq(userId))).thenReturn(responses);

        final Map<String, UUID> result = target.getCppCaseIdMapFor(List.of("ref1", "ref2"), null);

        assertThat(result.size(), is(1));
        assertThat(result.get("ref1"), is(caseId1));
        assertThat(result.containsKey("ref2"), is(false));
    }

    @Test
    void shouldThrowAccessControlViolationExceptionWhenNoSystemUserContextForGetCppCaseIdMapFor() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.empty());

        assertThrows(AccessControlViolationException.class, () -> target.getCppCaseIdMapFor(List.of("ref1"), null));
    }

    @Test
    void shouldReturnEmptyMapWhenNoProsecutorCaseReferencesProvided() {
        final UUID userId = randomUUID();
        final AdditionResponses responses = new AdditionResponses(List.of());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.addMany(any(SystemidMapList.class), eq(userId))).thenReturn(responses);

        final Map<String, UUID> result = target.getCppCaseIdMapFor(List.of(), null);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldReturnCaseIdMapUsingPrefixedReferencesWhenProsecutingAuthorityIsProvided() {
        final UUID userId = randomUUID();
        final String prefixedRef = PROSECUTING_AUTHORITY + ":ref1";
        final ArgumentCaptor<SystemidMapList> captor = ArgumentCaptor.forClass(SystemidMapList.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.addMany(captor.capture(), eq(userId)))
                .thenReturn(new AdditionResponses(List.of()))
                .thenAnswer(invocation -> {
                    final UUID targetId = ((SystemidMapList) invocation.getArgument(0)).getSystemIds().get(0).getTargetId();
                    return new AdditionResponses(List.of(new SystemIdMappings(null, false, randomUUID(), prefixedRef, targetId)));
                });

        final Map<String, UUID> result = target.getCppCaseIdMapFor(List.of("ref1"), PROSECUTING_AUTHORITY);

        final List<SystemidMapList> allCaptured = captor.getAllValues();
        assertThat(allCaptured.size(), is(2));
        final UUID firstCallTargetId = allCaptured.get(0).getSystemIds().get(0).getTargetId();
        final UUID secondCallTargetId = allCaptured.get(1).getSystemIds().get(0).getTargetId();
        assertThat(firstCallTargetId, is(secondCallTargetId));
        assertThat(result.size(), is(1));
        assertThat(result.get(prefixedRef), is(secondCallTargetId));
    }

    @Test
    void shouldPassCorrectSystemIdMapsToAddManyCall() {
        final UUID userId = randomUUID();
        final SystemIdMappings mapping = new SystemIdMappings(null, false, randomUUID(), "ref1", randomUUID());
        final AdditionResponses responses = new AdditionResponses(List.of(mapping));
        final ArgumentCaptor<SystemidMapList> captor = ArgumentCaptor.forClass(SystemidMapList.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.addMany(captor.capture(), eq(userId))).thenReturn(responses);

        target.getCppCaseIdMapFor(List.of("ref1"), null);

        assertThat(captor.getValue().getSystemIds().size(), is(1));
        assertThat(captor.getValue().getSystemIds().get(0).getSourceId(), is("ref1"));
        assertThat(captor.getValue().getSystemIds().get(0).getSourceType(), is(SOURCE_TYPE));
        assertThat(captor.getValue().getSystemIds().get(0).getTargetType(), is(TARGET_TYPE));
        assertThat(captor.getValue().getSystemIds().get(0).getTargetId(), is(notNullValue()));
    }
}
