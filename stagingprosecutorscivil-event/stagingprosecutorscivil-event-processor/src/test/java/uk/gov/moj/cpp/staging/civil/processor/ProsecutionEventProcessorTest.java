package uk.gov.moj.cpp.staging.civil.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.OtherCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.GroupProsecutions;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateGroupProsecution;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicCivilProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupParkedForSummonsApplicationApproval;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupSubmissionApproved;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupSubmissionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicParkedForSummonsApplicationApproval;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicSubmissionApproved;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicSubmissionRejected;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.*;

@ExtendWith(MockitoExtension.class)
public class ProsecutionEventProcessorTest {

    private static final UUID CASE_FILE_ID = randomUUID();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<InitiateGroupProsecution>> groupEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<InitiateProsecution>> singleEnvelopeCaptor;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @InjectMocks
    private ProsecutionEventProcessor target;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Response response;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleOtherCaseReceivedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method("processProsecutionOthers")
                        .thatHandles("stagingprosecutorscivil.event.other-case-received")
                ));
    }

    @Test
    public void shouldHandleSummonsReceivedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method("processProsecutionSummons")
                        .thatHandles("stagingprosecutorscivil.event.summons-received")
                ));
    }

    @Test
    public void shouldhandleGroupProsecutionRejected() {
        final PublicGroupProsecutionRejected updateCivilCaseRejected = updateCivilCaseReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicGroupProsecutionRejected> updateCivilCaseReceivedEnvelope = testEnvelope(updateCivilCaseRejected, "public.prosecutioncasefile.group-prosecution-rejected",
                updateCivilCaseRejected.getExternalId().toString(), eventCreatedTime);
        target.handleGroupProsecutionRejected(updateCivilCaseReceivedEnvelope);
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(updateCivilCaseRejected.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.FAILED.name()));

        assertThat(payload.getJsonArray("caseErrors").getJsonObject(0).getString("prosecutorCaseReference"), is("URN01"));
        assertThat(payload.getJsonArray("caseErrors").getJsonObject(0).getJsonArray("problems").getJsonObject(0).getString("code"), is("ERR01"));
        assertThat(payload.getJsonArray("groupCaseErrors").getJsonObject(0).containsKey("prosecutorCaseReference"), is(false));
        assertThat(payload.getJsonArray("groupCaseErrors").getJsonObject(0).getJsonArray("problems").getJsonObject(0).getString("code"), is("ERR03"));
    }

    @Test
    public void shouldhandleSingleProsecutionRejected() {
        final PublicCivilProsecutionRejected updateCivilProsecutionCaseRejected = updateCivilProsecutionCaseReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicCivilProsecutionRejected> updateCivilProsecutionCaseReceivedEnvelope = testEnvelope(updateCivilProsecutionCaseRejected, "public.prosecutioncasefile.civil-prosecution-rejected",
                updateCivilProsecutionCaseRejected.getExternalId().toString(), eventCreatedTime);
        target.handleCivilProsecutionRejected(updateCivilProsecutionCaseReceivedEnvelope);
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(updateCivilProsecutionCaseRejected.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.FAILED.name()));

        assertThat(payload.getJsonArray("caseErrors").getJsonObject(0).getString("prosecutorCaseReference"), is("URN01"));
        assertThat(payload.getJsonArray("caseErrors").getJsonObject(0).getJsonArray("problems").getJsonObject(0).getString("code"), is("ERR01"));
    }

    @Test
    public void shouldHandleParkedForSummonsApplicationApproval() {
        final PublicParkedForSummonsApplicationApproval parkedForApproval = parkedForSummonsApplicationApproval();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicParkedForSummonsApplicationApproval> envelope = testEnvelope(parkedForApproval,
                "public.prosecutioncasefile.parked-for-summons-application-approval",
                parkedForApproval.getExternalId().toString(), eventCreatedTime);

        target.handleParkedForSummonsApplicationApproval(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(parkedForApproval.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.PENDING_COURT_DECISION.name()));
        assertThat(payload.getString("summonsApplicationId"), is(parkedForApproval.getApplicationId().toString()));
    }

    @Test
    public void shouldHandleGroupParkedForSummonsApplicationApproval() {
        final PublicGroupParkedForSummonsApplicationApproval groupParkedForApproval = groupParkedForSummonsApplicationApproval();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicGroupParkedForSummonsApplicationApproval> envelope = testEnvelope(groupParkedForApproval,
                "public.prosecutioncasefile.group-parked-for-summons-application-approval",
                groupParkedForApproval.getExternalId().toString(), eventCreatedTime);

        target.handleGroupParkedForSummonsApplicationApproval(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(groupParkedForApproval.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.PENDING_COURT_DECISION.name()));
        assertThat(payload.getString("summonsApplicationId"), is(groupParkedForApproval.getApplicationId().toString()));
    }

    @Test
    public void shouldHandleSubmissionApproved() {
        final PublicSubmissionApproved approved = submissionApproved();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicSubmissionApproved> envelope = testEnvelope(approved,
                "public.prosecutioncasefile.submission-approved",
                approved.getExternalId().toString(), eventCreatedTime);

        target.handleSubmissionApproved(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(approved.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.ACCEPTED.name()));
    }

    @Test
    public void shouldHandleGroupSubmissionApproved() {
        final PublicGroupSubmissionApproved approved = groupSubmissionApproved();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicGroupSubmissionApproved> envelope = testEnvelope(approved,
                "public.prosecutioncasefile.group-submission-approved",
                approved.getExternalId().toString(), eventCreatedTime);

        target.handleGroupSubmissionApproved(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(approved.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.ACCEPTED.name()));
    }

    @Test
    public void shouldHandleSubmissionRejected() {
        final PublicSubmissionRejected rejected = submissionRejected();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicSubmissionRejected> envelope = testEnvelope(rejected,
                "public.prosecutioncasefile.submission-rejected",
                rejected.getExternalId().toString(), eventCreatedTime);

        target.handleSubmissionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(rejected.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.REJECTED.name()));
    }

    @Test
    public void shouldHandleGroupSubmissionRejected() {
        final PublicGroupSubmissionRejected rejected = groupSubmissionRejected();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicGroupSubmissionRejected> envelope = testEnvelope(rejected,
                "public.prosecutioncasefile.group-submission-rejected",
                rejected.getExternalId().toString(), eventCreatedTime);

        target.handleGroupSubmissionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(rejected.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.REJECTED.name()));
    }

    @Test
    public void shouldInitiateProsecutionCommandToPCFForOtherCase() {
        final OtherCaseReceived prosecutionReceived = otherCaseReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<OtherCaseReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.other-case-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);
        final UUID caseFileId = UUID.randomUUID();
        final Map<String, UUID> caseRefToCaseId = new HashMap<>();
        caseRefToCaseId.put(prosecutionReceived.getProsecutionCases().get(0).getUrn(), caseFileId);
        when(systemIdMapperService.getCppCaseIdMapFor(any(), any())).thenReturn(caseRefToCaseId);

        target.processProsecutionOthers(prosecutionReceivedEnvelope);

        verify(sender).sendAsAdmin(singleEnvelopeCaptor.capture());
        final InitiateProsecution initiateProsecution = singleEnvelopeCaptor.getValue().payload();

        assertThat(initiateProsecution.getChannel().name(), is("CIVIL"));
        assertThat(initiateProsecution.getExternalId(), is(prosecutionReceived.getSubmissionId()));
        assertThat(initiateProsecution.getCaseDetails().getInitiationCode(), is("O"));
        assertThat(initiateProsecution.getIsCivil(), is(true));
        assertThat(initiateProsecution.getIsGroupMaster(), is(false));
        assertThat(initiateProsecution.getIsGroupMember(), is(false));
        assertThat(initiateProsecution.getCaseDetails().getCaseId(), is(caseFileId));
    }

    @Test
    public void shouldInitiateProsecutionCommandToPCFForSummons() {
        final SummonsReceived prosecutionReceived = summonsReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<SummonsReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.summons-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        final UUID caseFileId = UUID.randomUUID();
        final Map<String, UUID> caseRefToCaseId = new HashMap<>();
        caseRefToCaseId.put(prosecutionReceived.getProsecutionCases().get(0).getUrn(), caseFileId);
        when(systemIdMapperService.getCppCaseIdMapFor(any(), any())).thenReturn(caseRefToCaseId);
        target.processProsecutionSummons(prosecutionReceivedEnvelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(captor.getValue().payload());
        assertThat(jsonObject.getString("channel"), is("CIVIL"));
        assertThat(jsonObject.getString("externalId"), is(prosecutionReceived.getSubmissionId().toString()));
        assertThat(jsonObject.getJsonObject("caseDetails").getString("initiationCode"), is("S"));
        assertThat(jsonObject.getJsonObject("caseDetails").getString("caseId"), is(caseFileId.toString()));
    }

    @Test
    public void shouldInitiateGroupProsecutionCommandToPCFForOtherCase() {
        final OtherCaseReceived prosecutionReceived = groupOtherCaseReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<OtherCaseReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.other-case-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        target.processProsecutionOthers(prosecutionReceivedEnvelope);

        verify(sender).sendAsAdmin(groupEnvelopeCaptor.capture());
        final InitiateGroupProsecution initiateGroupProsecution = groupEnvelopeCaptor.getValue().payload();
        final GroupProsecutions groupProsecution = initiateGroupProsecution.getGroupProsecutions().get(0);

        assertThat(initiateGroupProsecution.getChannel().name(), is("CIVIL"));
        assertThat(initiateGroupProsecution.getExternalId(), is(prosecutionReceived.getSubmissionId()));
        assertThat(groupProsecution.getIsCivil(), is(true));
        assertThat(groupProsecution.getIsGroupMaster(), is(true));
        assertThat(groupProsecution.getIsGroupMember(), is(true));
        assertThat(groupProsecution.getPaymentReference(), is(prosecutionReceived.getProsecutionCases().get(0).getPaymentReference()));
        assertThat(groupProsecution.getCaseDetails().getInitiationCode(), is("O"));
    }

    @Test
    public void shouldInitiateGroupProsecutionCommandToPCFForSummons() {
        final SummonsReceived prosecutionReceived = groupSummonsReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<SummonsReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.summons-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        target.processProsecutionSummons(prosecutionReceivedEnvelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(captor.getValue().payload());

        assertThat(jsonObject.getString("channel"), is("CIVIL"));
        assertThat(jsonObject.getString("externalId"), is(prosecutionReceived.getSubmissionId().toString()));
        assertThat(jsonObject.getJsonArray("groupProsecutions").getJsonObject(0).getBoolean("isCivil"), is(true));
        assertThat(jsonObject.getJsonArray("groupProsecutions").getJsonObject(0).getBoolean("isGroupMaster"), is(true));
        assertThat(jsonObject.getJsonArray("groupProsecutions").getJsonObject(0).getBoolean("isGroupMember"), is(true));
        assertThat(jsonObject.getJsonArray("groupProsecutions").getJsonObject(0).getString("paymentReference"), is(prosecutionReceived.getProsecutionCases().get(0).getPaymentReference()));
        assertThat(jsonObject.getJsonArray("groupProsecutions").getJsonObject(0).getJsonObject("caseDetails").getString("initiationCode"), is("S"));
    }

    @Disabled("This is rebased from main but seems like redundant")
    @Test
    public void shouldSendUpdateCivilCaseCommandWithPendingStatusOnReceipt() {
        final OtherCaseReceived prosecutionReceived = groupOtherCaseReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<OtherCaseReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.other-case-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        target.processProsecutionOthers(prosecutionReceivedEnvelope);

        verify(sender).sendAsAdmin(groupEnvelopeCaptor.capture());

        final ArgumentCaptor<Envelope> updateCivilCaseCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(updateCivilCaseCaptor.capture());
        final JsonObject result = (JsonObject) updateCivilCaseCaptor.getValue().payload();
        assertThat(result.getString("submissionId"), is(prosecutionReceived.getSubmissionId().toString()));
        assertThat(result.getString("submissionStatus"), is(SubmissionStatus.PENDING.name()));
    }

    @Test
    public void shouldIncludeProblemValuesInCaseErrorsForCivilRejected() {
        final Problem problemWithValues = Problem.problem()
                .withCode("ERR_CODE")
                .withValues(singletonList(ProblemValue.problemValue()
                        .withKey("dob")
                        .withValue("2050-01-01")
                        .build()))
                .build();
        final CaseProblem caseProblem = CaseProblem.caseProblem()
                .withProblems(singletonList(problemWithValues))
                .withProsecutorCaseReference("CASE_URN")
                .build();
        final PublicCivilProsecutionRejected payload = PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                .withExternalId(randomUUID())
                .withCaseErrors(singletonList(caseProblem))
                .build();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicCivilProsecutionRejected> envelope = testEnvelope(payload,
                "public.prosecutioncasefile.civil-prosecution-rejected",
                payload.getExternalId().toString(), eventCreatedTime);

        target.handleCivilProsecutionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject result = (JsonObject) captor.getValue().payload();
        assertThat(result.getString("submissionStatus"), is(SubmissionStatus.FAILED.name()));
        final JsonObject firstError = result
                .getJsonArray("caseErrors")
                .getJsonObject(0)
                .getJsonArray("problems")
                .getJsonObject(0);
        assertThat(firstError.getString("code"), is("ERR_CODE"));
        assertThat(firstError.getJsonArray("values").getJsonObject(0).getString("key"), is("dob"));
        assertThat(firstError.getJsonArray("values").getJsonObject(0).getString("value"), is("2050-01-01"));
    }

    @Test
    public void shouldIncludeProblemValueIdWhenPresentInCaseErrors() {
        final Problem problemWithId = Problem.problem()
                .withCode("ERR_WITH_ID")
                .withValues(singletonList(ProblemValue.problemValue()
                        .withId("some-id")
                        .withKey("field")
                        .withValue("badValue")
                        .build()))
                .build();
        final CaseProblem caseProblem = CaseProblem.caseProblem()
                .withProblems(singletonList(problemWithId))
                .withProsecutorCaseReference("CASE_URN")
                .build();
        final PublicCivilProsecutionRejected payload = PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                .withExternalId(randomUUID())
                .withCaseErrors(singletonList(caseProblem))
                .build();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicCivilProsecutionRejected> envelope = testEnvelope(payload,
                "public.prosecutioncasefile.civil-prosecution-rejected",
                payload.getExternalId().toString(), eventCreatedTime);

        target.handleCivilProsecutionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payloadError = (JsonObject) captor.getValue().payload();
        assertThat(payloadError.getString("submissionId"), is(payload.getExternalId().toString()));
        assertThat(payloadError.getString("submissionStatus"), is(SubmissionStatus.FAILED.name()));
        final JsonObject firstValue = payloadError
                .getJsonArray("caseErrors")
                .getJsonObject(0)
                .getJsonArray("problems")
                .getJsonObject(0).getJsonArray("values").getJsonObject(0);
        assertThat(firstValue.getString("id"), is("some-id"));
        assertThat(firstValue.getString("key"), is("field"));
    }

    @Test
    public void shouldIncludeDefendantReferenceInDefendantErrors() {
        final DefendantProblem dpWithRef = DefendantProblem.defendantProblem()
                .withProsecutorDefendantReference("DEF-REF-001")
                .withProblems(singletonList(Problem.problem().withCode("ERR").build()))
                .build();
        final PublicCivilProsecutionRejected payload = PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                .withExternalId(randomUUID())
                .withDefendantErrors(singletonList(dpWithRef))
                .build();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicCivilProsecutionRejected> envelope = testEnvelope(payload,
                "public.prosecutioncasefile.civil-prosecution-rejected",
                payload.getExternalId().toString(), eventCreatedTime);

        target.handleCivilProsecutionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject firstDefendant = ((JsonObject) captor.getValue().payload())
                .getJsonArray("defendantErrors").getJsonObject(0);
        assertThat(firstDefendant.getString("prosecutorDefendantReference"), is("DEF-REF-001"));
    }

    @Test
    public void shouldOmitCaseAndDefendantErrorsFromCommandWhenNull() {
        final PublicCivilProsecutionRejected payload = PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                .withExternalId(randomUUID())
                .build();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicCivilProsecutionRejected> envelope = testEnvelope(payload,
                "public.prosecutioncasefile.civil-prosecution-rejected",
                payload.getExternalId().toString(), eventCreatedTime);

        target.handleCivilProsecutionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject result = (JsonObject) captor.getValue().payload();
        assertThat(result.getString("submissionStatus"), is(SubmissionStatus.FAILED.name()));
        assertThat(result.containsKey("caseErrors"), is(false));
        assertThat(result.containsKey("defendantErrors"), is(false));
    }

    private <T> Envelope<T> testEnvelope(final T payload, final String eventName, final String submissionId, final ZonedDateTime createdAt) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(createdAt);

        return Envelope.envelopeFrom(metadataFrom(createObjectBuilder(
                metadataBuilder.build().asJsonObject()).build())
                .withUserId(randomUUID().toString()).build(), payload);
    }
}