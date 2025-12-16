package uk.gov.moj.cpp.staging.civil.processor;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.REJECTED;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.staging.civil.processor.converter.ProsecutionCaseToGroupProsecutionConverterForCharge;
import uk.gov.moj.cpp.staging.civil.processor.converter.ProsecutionCaseToGroupProsecutionConverterForSummons;
import uk.gov.moj.cpp.staging.civil.processor.util.ProsecutorCaseReferenceUtil;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;
import uk.gov.moj.cps.prosecutioncasefile.command.api.GroupProsecutions;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateGroupProsecution;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicCivilProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupProsecutionRejected;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionChargedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionChargedEventProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("stagingprosecutorscivil.event.charge-prosecution-received")
    public void processProsecutionCharged(final Envelope<ChargeProsecutionReceived> event) {
        LOGGER.info("Received stagingprosecutorscivil.event.charge-prosecution-received event with SubmissionId {}", event.payload().getSubmissionId());
        processChargeReceivedEvent(event);
    }

    @Handles("stagingprosecutorscivil.event.summons-prosecution-received")
    public void processProsecutionSummons(final Envelope<SummonsProsecutionReceived> event) {
        LOGGER.info("Received stagingprosecutorscivil.event.summons-prosecution-received event with SubmissionId {}", event.payload().getSubmissionId());
        processSummonsReceivedEvent(event);
    }


    @Handles("public.prosecutioncasefile.group-prosecution-rejected")
    public void handleGroupProsecutionRejected(final Envelope<PublicGroupProsecutionRejected> event) {
        LOGGER.info("Received public.prosecutioncasefile.group-prosecution-rejected event with payload for submission id {} ", event.payload().getExternalId());
        updateCivilCaseStatus(event, event.payload().getExternalId().toString(), SubmissionStatus.REJECTED);
    }

    @Handles("public.prosecutioncasefile.civil-prosecution-rejected")
    public void handleCivilProsecutionRejected(final Envelope<PublicCivilProsecutionRejected> event) {
        LOGGER.info("Received public.prosecutioncasefile.group-prosecution-rejected event with payload for submission id {} ", event.payload().getExternalId());
        updateCivilStatus(event, event.payload().getExternalId().toString(), SubmissionStatus.REJECTED);
    }

    private void processChargeReceivedEvent(final Envelope<ChargeProsecutionReceived> event) {
        final ZonedDateTime dateReceived = event.metadata().createdAt().orElse(now());
        final ChargeProsecutionReceived chargeProsecutionReceived = event.payload();
        final List<GroupProsecutions> groupProsecutions = getGroupProsecutionsForCharge(dateReceived, chargeProsecutionReceived, randomUUID());

        initiatePCFCommand(groupProsecutions, chargeProsecutionReceived.getSubmissionId(), event.metadata());
        updateCivilCaseStatus(event, chargeProsecutionReceived.getSubmissionId().toString(), PENDING);

    }

    private void processSummonsReceivedEvent(final Envelope<SummonsProsecutionReceived> event) {
        final ZonedDateTime dateReceived = event.metadata().createdAt().orElse(now());
        final SummonsProsecutionReceived summonsProsecutionReceived = event.payload();
        final List<GroupProsecutions> groupProsecutions = getGroupProsecutionsForSummons(dateReceived, summonsProsecutionReceived, randomUUID());

        initiatePCFCommand(groupProsecutions, summonsProsecutionReceived.getSubmissionId(), event.metadata());
        updateCivilCaseStatus(event, summonsProsecutionReceived.getSubmissionId().toString(), PENDING);

    }

    private List<GroupProsecutions> getGroupProsecutionsForCharge(final ZonedDateTime dateReceived,
                                                                  final ChargeProsecutionReceived chargeProsecutionReceived,
                                                                  final UUID groupId) {
        final Map<String, UUID> caseRefToCaseId = systemIdMapperService.getCppCaseIdMapFor(getProsecutorCaseReferences(chargeProsecutionReceived.getProsecutionCases(), chargeProsecutionReceived.getProsecutingAuthority()));
        final Converter<ProsecutionCase, GroupProsecutions> prosecutionCaseToGroupProsecutionConverter
                = new ProsecutionCaseToGroupProsecutionConverterForCharge(dateReceived, chargeProsecutionReceived, groupId, caseRefToCaseId);

        return chargeProsecutionReceived.getProsecutionCases()
                .stream()
                .map(prosecutionCaseToGroupProsecutionConverter::convert)
                .collect(Collectors.toList());
    }

    private List<GroupProsecutions> getGroupProsecutionsForSummons(final ZonedDateTime dateReceived,
                                                                   final SummonsProsecutionReceived summonsProsecutionReceived,
                                                                   final UUID groupId) {
        final Map<String, UUID> caseRefToCaseId = systemIdMapperService.getCppCaseIdMapFor(getProsecutorCaseReferences(summonsProsecutionReceived.getProsecutionCases(), summonsProsecutionReceived.getProsecutingAuthority()));
        final Converter<ProsecutionCase, GroupProsecutions> prosecutionCaseToGroupProsecutionConverterForSummons
                = new ProsecutionCaseToGroupProsecutionConverterForSummons(dateReceived, summonsProsecutionReceived, groupId, caseRefToCaseId);

        return summonsProsecutionReceived.getProsecutionCases()
                .stream()
                .map(prosecutionCaseToGroupProsecutionConverterForSummons::convert)
                .collect(Collectors.toList());
    }

    private void initiatePCFCommand(final List<GroupProsecutions> groupProsecutions, final UUID submissionId, final Metadata metadataValue) {
        if (groupProsecutions.size() == 1) {
            initiateCCProsecutionCommand(groupProsecutions.get(0), submissionId, metadataValue);
        } else {
            initiateGroupCommand(groupProsecutions, submissionId, metadataValue);
        }

    }

    private void initiateGroupCommand(final List<GroupProsecutions> groupProsecutions, final UUID submissionId, final Metadata metadataValue) {
        selectGroupMaster(groupProsecutions);

        final InitiateGroupProsecution initiateGroupProsecution = InitiateGroupProsecution.initiateGroupProsecution()
                .withGroupProsecutions(groupProsecutions)
                .withChannel(CIVIL)
                .withExternalId(submissionId)
                .build();

        final Metadata metadata = metadataFrom(metadataValue)
                .withName("prosecutioncasefile.command.initiate-group-prosecution")
                .build();

        final Envelope<InitiateGroupProsecution> envelope = envelopeFrom(metadata, initiateGroupProsecution);

        LOGGER.info("Calling prosecutioncasefile.command.initiate-group-prosecution for submission id {} with case count {} ", submissionId, groupProsecutions.size());
        sender.sendAsAdmin(envelope);
    }

    private void initiateCCProsecutionCommand(final GroupProsecutions groupProsecutions, final UUID submissionId, final Metadata metadataValue) {

        final CaseDetails caseDetails = groupProsecutions.getCaseDetails();
        final CaseDetails updatedCaseDetails = CaseDetails.caseDetails().withValuesFrom(caseDetails).withPaymentReference(groupProsecutions.getPaymentReference()).build();
        final InitiateProsecution initiateProsecution = InitiateProsecution.initiateProsecution()
                .withCaseDetails(updatedCaseDetails)
                .withChannel(CIVIL)
                .withIsGroupMaster(false)
                .withIsGroupMember(false)
                .withIsCivil(true)
                .withDefendants(groupProsecutions.getDefendants())
                .withExternalId(submissionId)
                .build();

        final Metadata metadata = metadataFrom(metadataValue)
                .withName("prosecutioncasefile.command.initiate-cc-prosecution")
                .build();

        final Envelope<InitiateProsecution> envelope = envelopeFrom(metadata, initiateProsecution);

        LOGGER.info("Calling prosecutioncasefile.command.initiate-cc-prosecution for submission id {} sa case count is 1 ", submissionId);
        sender.sendAsAdmin(envelope);
    }

    private void selectGroupMaster(final List<GroupProsecutions> groupProsecutions) {
        final GroupProsecutions element = groupProsecutions.get(0);
        groupProsecutions.set(0, GroupProsecutions.groupProsecutions()
                .withCaseDetails(element.getCaseDetails())
                .withIsGroupMaster(true)
                .withIsGroupMember(element.getIsGroupMember())
                .withIsCivil(element.getIsCivil())
                .withGroupId(element.getGroupId())
                .withDefendants(element.getDefendants())
                .withPaymentReference(element.getPaymentReference())
                .build());
    }


    private JsonArray transformErrorsToJsonArray(final Collection<Problem> errorsOrWarnings) {
        if (errorsOrWarnings == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        errorsOrWarnings.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }

    private JsonArray transformDefendantProblemsToJsonArray(final Collection<DefendantProblem> errors) {
        if (errors == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        errors.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }


    private void updateCivilStatus(final Envelope<?> event, final String submissionId, final SubmissionStatus status) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("submissionId", submissionId)
                .add("submissionStatus", status.name());

        if (status == REJECTED) {
            final PublicCivilProsecutionRejected prosecutionRejected = (PublicCivilProsecutionRejected) event.payload();
            jsonObjectBuilder.add("caseErrors", transformErrorsToJsonArray(prosecutionRejected.getCaseErrors()));
            jsonObjectBuilder.add("defendantErrors", transformDefendantProblemsToJsonArray(prosecutionRejected.getDefendantErrors()));
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Calling stagingprosecutorscivil.command.update-civil-case for submission id {} and status {}", submissionId, status);
        }
        updateSubmitionStatus(event, jsonObjectBuilder);
    }

    private void updateSubmitionStatus(final Envelope<?> event, final JsonObjectBuilder jsonObjectBuilder) {
        sender.send(envelop(jsonObjectBuilder.build())
                .withName("stagingprosecutorscivil.command.update-civil-case")
                .withMetadataFrom(event));
    }

    private void updateCivilCaseStatus(final Envelope<?> event, final String submissionId, final SubmissionStatus status) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("submissionId", submissionId)
                .add("submissionStatus", status.name());

        if (status == REJECTED) {
            final PublicGroupProsecutionRejected prosecutionRejected = (PublicGroupProsecutionRejected) event.payload();
            final JsonArray caseErrors = transformErrorsToJsonArray(prosecutionRejected.getCaseErrors());
            final JsonArray groupCaseErrors = transformErrorsToJsonArray(prosecutionRejected.getGroupCaseErrors());
            final JsonArray defendantErrors = transformDefendantProblemsToJsonArray(prosecutionRejected.getDefendantErrors());

            ofNullable(caseErrors).ifPresent(e -> jsonObjectBuilder.add("caseErrors", e));
            ofNullable(groupCaseErrors).ifPresent(e -> jsonObjectBuilder.add("groupCaseErrors", e));
            ofNullable(defendantErrors).ifPresent(e -> jsonObjectBuilder.add("defendantErrors", e));
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Calling stagingprosecutorscivil.command.update-civil-case for submission id {} and status {}", submissionId, status);
        }
        updateSubmitionStatus(event, jsonObjectBuilder);
    }

    private List<String> getProsecutorCaseReferences(final List<ProsecutionCase> prosecutionCases, final String prosecutingAuthority) {
        return prosecutionCases.stream()
                .map(pc -> ProsecutorCaseReferenceUtil.getProsecutorCaseReference(prosecutingAuthority, pc.getUrn())).collect(Collectors.toList());
    }
}