package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.core.accesscontrol.AccessControlViolationException;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponses;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.client.SystemidMapList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SystemIdMapperService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemIdMapperService.class);
    private static final String SOURCE_TYPE = "OU_URN";

    private static final String TARGET_TYPE = "CASE_FILE_ID";

    private static final String SPI_SOURCE_TYPE = "SPI-URN";

    private static final String SPI_TARGET_TYPE = "CASE-ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public UUID getCppCaseIdFor(final String prosecutorCaseReference) {
        LOGGER.info("SystemIdMapperService : calling system-id-mapper with prosecutorCaseReference {} ", prosecutorCaseReference);
        final Optional<SystemIdMapping> mapping = getSystemIdMappingFor(prosecutorCaseReference);
        if (mapping.isPresent()) {
            return mapping.get().getTargetId();
        }

        final Optional<SystemIdMapping> mappingForPtiUrn = getSystemIdMappingForSpiCase(prosecutorCaseReference);
        if (mappingForPtiUrn.isPresent()) {
            return mappingForPtiUrn.get().getTargetId();
        }

        final UUID newCaseId = randomUUID();
        final AdditionResponse additionResponse = attemptAddMappingForURN(newCaseId, prosecutorCaseReference);
        if (additionResponse.isSuccess()) {
            return newCaseId;
        }

        return getSystemIdMappingFor(prosecutorCaseReference)
                .orElseThrow(() -> new IllegalStateException("Error generating case id"))
                .getTargetId();
    }

    private Optional<SystemIdMapping> getSystemIdMappingFor(final String prosecutorCaseReference) {
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();
        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.findBy(prosecutorCaseReference, SOURCE_TYPE, TARGET_TYPE, contextSystemUserId.get());
        }
        return Optional.empty();
    }

    //This mapping is introduced to handle cases created through SPI flow as they have different source and target names defined in the systemIdMapper.
    //To do Solution will be given by architects.
    private Optional<SystemIdMapping> getSystemIdMappingForSpiCase(final String prosecutorCaseReference) {
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();
        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.findBy(prosecutorCaseReference, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, contextSystemUserId.get());
        }
        return Optional.empty();
    }

    private AdditionResponse attemptAddMappingForURN(final UUID caseId, final String prosecutorCaseReference) {
        final SystemIdMap systemIdMap = new SystemIdMap(prosecutorCaseReference, SOURCE_TYPE, caseId, TARGET_TYPE);
        final UUID contextSystemUserId = systemUserProvider.getContextSystemUserId().orElseThrow(() -> new AccessControlViolationException("System user not found"));
        return systemIdMapperClient.add(systemIdMap, contextSystemUserId);
    }

    public Map<String, UUID> getCppCaseIdMapFor(List<String> prosecutorCaseReferences) {
        LOGGER.info("SystemIdMapperService : calling bulk system-ids-mapper for {} prosecutorCaseReference's", prosecutorCaseReferences.size());
        final UUID contextSystemUserId = systemUserProvider.getContextSystemUserId().orElseThrow(() -> new AccessControlViolationException("System user not found"));
        final List<SystemIdMap> systemIdMapList = prosecutorCaseReferences.stream().map(prosecutorCaseReference -> new SystemIdMap(prosecutorCaseReference, SOURCE_TYPE, randomUUID(), TARGET_TYPE)).collect(Collectors.toList());
        final AdditionResponses additionResponses = systemIdMapperClient.addMany(new SystemidMapList(systemIdMapList), contextSystemUserId);

        LOGGER.info("SystemIdMapperService : checking AdditionResponses for bulk system-id-mapper");
        final Map<String, UUID> caseRefToCaseId = new HashMap<>();
        additionResponses.getSystemIdMappings().forEach(systemIdMappings -> {
            if(isNull(systemIdMappings.getError())) {
                caseRefToCaseId.put(systemIdMappings.getSourceId(), systemIdMappings.getTargetId());
            } else {
                LOGGER.error("SystemIdMapperService : Error generating case id: {}", systemIdMappings.getError());
            }
        });
        return caseRefToCaseId;
    }
}