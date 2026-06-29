package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.chargeProsecutionReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.groupChargeProsecutionReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.groupSummonsProsecutionReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.summonsProsecutionReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.updateCivilCaseReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.updateCivilProsecutionCaseReceived;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.PENDING;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.GroupProsecutions;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateGroupProsecution;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicCivilProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupProsecutionRejected;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionChargedEventProcessorTest {

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
    private ProsecutionChargedEventProcessor target;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Response response;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleChargeProsecutionReceivedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method("processProsecutionCharged")
                        .thatHandles("stagingprosecutorscivil.event.charge-prosecution-received")
                ));
    }

    @Test
    public void shouldHandleSummonsProsecutionReceivedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method("processProsecutionSummons")
                        .thatHandles("stagingprosecutorscivil.event.summons-prosecution-received")
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
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.REJECTED.name()));
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
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.REJECTED.name()));
    }

    @Test
    public void shouldInitiateProsecutionCommandToPCFForChargeProsecution() {
        final ChargeProsecutionReceived prosecutionReceived = chargeProsecutionReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<ChargeProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.charge-prosecution-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);
        final UUID caseFileId = UUID.randomUUID();
        final Map<String, UUID> caseRefToCaseId = new HashMap<>();
        caseRefToCaseId.put(prosecutionReceived.getProsecutionCases().get(0).getUrn(), caseFileId);
        when(systemIdMapperService.getCppCaseIdMapFor(any())).thenReturn(caseRefToCaseId);

        target.processProsecutionCharged(prosecutionReceivedEnvelope);

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
    public void shouldInitiateProsecutionCommandToPCFForSummonsProsecution() {
        final SummonsProsecutionReceived prosecutionReceived = summonsProsecutionReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<SummonsProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.summons-prosecution-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        final UUID caseFileId = UUID.randomUUID();
        final Map<String, UUID> caseRefToCaseId = new HashMap<>();
        caseRefToCaseId.put(prosecutionReceived.getProsecutionCases().get(0).getUrn(), caseFileId);
        when(systemIdMapperService.getCppCaseIdMapFor(any())).thenReturn(caseRefToCaseId);
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
    public void shouldInitiateGroupProsecutionCommandToPCFForChargeProsecution() {
        final ChargeProsecutionReceived prosecutionReceived = groupChargeProsecutionReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<ChargeProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.charge-prosecution-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        target.processProsecutionCharged(prosecutionReceivedEnvelope);

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
    public void shouldInitiateGroupProsecutionCommandToPCFForSummonsProsecution() {
        final SummonsProsecutionReceived prosecutionReceived = groupSummonsProsecutionReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<SummonsProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.summons-prosecution-received",
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

    @Test
    public void shouldCallCommandToUpdateCivilCase() {
        final ChargeProsecutionReceived prosecutionReceived = groupChargeProsecutionReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<ChargeProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, "stagingprosecutorscivil.event.charge-prosecution-received",
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        target.processProsecutionCharged(prosecutionReceivedEnvelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(captor.getValue().payload());

        assertThat(jsonObject.getString("submissionId"), is(notNullValue()));
        assertThat(jsonObject.getString("submissionStatus"), is(PENDING.name()));
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
        final PublicCivilProsecutionRejected payload = PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                .withExternalId(randomUUID())
                .withCaseErrors(singletonList(problemWithValues))
                .build();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicCivilProsecutionRejected> envelope = testEnvelope(payload,
                "public.prosecutioncasefile.civil-prosecution-rejected",
                payload.getExternalId().toString(), eventCreatedTime);

        target.handleCivilProsecutionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject result = (JsonObject) captor.getValue().payload();
        assertThat(result.getString("submissionStatus"), is(SubmissionStatus.REJECTED.name()));
        final JsonObject firstError = result.getJsonArray("caseErrors").getJsonObject(0);
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
        final PublicCivilProsecutionRejected payload = PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                .withExternalId(randomUUID())
                .withCaseErrors(singletonList(problemWithId))
                .build();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<PublicCivilProsecutionRejected> envelope = testEnvelope(payload,
                "public.prosecutioncasefile.civil-prosecution-rejected",
                payload.getExternalId().toString(), eventCreatedTime);

        target.handleCivilProsecutionRejected(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject firstValue = ((JsonObject) captor.getValue().payload())
                .getJsonArray("caseErrors").getJsonObject(0)
                .getJsonArray("values").getJsonObject(0);
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
        assertThat(result.getString("submissionStatus"), is(SubmissionStatus.REJECTED.name()));
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