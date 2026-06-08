package uk.gov.moj.cpp.staging.civil.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.civil.aggregate.MaterialSubmission;
import uk.gov.moj.cpp.staging.civil.aggregate.ProsecutionSubmissionAggregate;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.ChargeProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.SubmitMaterialCommand;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.SummonsProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.UpdateCivilCase;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilProsecutionHandlerTest {

    private static final String PRIVATE_COMMAND_CHARGE_PROSECUTION = "stagingprosecutorscivil.command.charge-prosecution";
    private static final String PRIVATE_EVENT_CHARGE_PROSECUTION_RECEIVED = "stagingprosecutorscivil.event.charge-prosecution-received";
    private static final String PRIVATE_COMMAND_SUMMONS_PROSECUTION = "stagingprosecutorscivil.command.summons-prosecution";
    private static final String PRIVATE_EVENT_SUMMONS_PROSECUTION_RECEIVED = "stagingprosecutorscivil.event.summons-prosecution-received";
    private static final String PRIVATE_COMMAND_UPDATE_CASE_PROFILE = "stagingprosecutorscivil.command.update-civil-case";
    private static final String PRIVATE_EVENT_UPDATE_CASE_FILE_RECEIVED = "stagingprosecutorscivil.event.update-civil-case-received";
    private static final String PRIVATE_COMMAND_SUBMIT_MATERIAL = "stagingprosecutorscivil.command.submit-material";
    private static final String PRIVATE_EVENT_MATERIAL_SUBMITTED = "stagingprosecutorscivil.event.material-submitted";


    @InjectMocks
    private CivilProsecutionHandler civilProsecutionHandler;

    private static final UUID USER_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(ChargeProsecutionReceived.class, SummonsProsecutionReceived.class, UpdateCivilCaseReceived.class, MaterialSubmitted.class);

    @Test
    public void shouldHandleChargeProsecutionCommand() {

        assertThat(civilProsecutionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleChargeProsecution")
                        .thatHandles(PRIVATE_COMMAND_CHARGE_PROSECUTION)));

    }

    @Test
    public void shouldRaiseChargeProsecutionReceivedPrivateEvent() throws Exception {


        final Envelope<ChargeProsecution> envelope = buildChargeProsecutionEnvelope();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class)).thenReturn(new ProsecutionSubmissionAggregate());

        civilProsecutionHandler.handleChargeProsecution(envelope);

        verifyChargeProsecutionReceivedPrivateEvent();

    }

    @Test
    public void shouldHandleSummonsProsecutionCommand() {

        assertThat(civilProsecutionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleSummonsProsecution")
                        .thatHandles(PRIVATE_COMMAND_SUMMONS_PROSECUTION)));

    }

    @Test
    public void shouldRaiseSummonsProsecutionReceivedPrivateEvent() throws Exception {

        final Envelope<SummonsProsecution> envelope = buildSummonsProsecutionEnvelope();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class)).thenReturn(new ProsecutionSubmissionAggregate());

        civilProsecutionHandler.handleSummonsProsecution(envelope);

        verifySummonsProsecutionReceivedPrivateEvent();;

    }

    @Test
    public void shouldRaiseUpdateCivilCaseReceivedPrivateEvent() throws Exception {

        final Envelope<UpdateCivilCase> envelope = buildUpdateCaseFileEnvelope();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class)).thenReturn(new ProsecutionSubmissionAggregate());

        civilProsecutionHandler.handleCivilCaseUpdate(envelope);
        verifyUpdateCaseFileReceivedPrivateEvent();
    }

    private void verifyChargeProsecutionReceivedPrivateEvent() throws EventStreamException {

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PRIVATE_EVENT_CHARGE_PROSECUTION_RECEIVED),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutingAuthority", is("THREE RIVER")),
                                withJsonPath("$.submissionId", notNullValue()),
                                withJsonPath("$.prosecutionCases[0].caseMarker", is("Markers")),
                                withJsonPath("$.prosecutionCases[0].paymentReference", is("PAYREF123")),
                                withJsonPath("$.prosecutionCases[0].defendants[0]", notNullValue()),
                                withJsonPath("$.prosecutionCases[0].defendants[0].offences[0]", notNullValue()),
                                withJsonPath("$.prosecutionCases[0].defendants[0].offences[0].arrestDate", is(LocalDate.now().toString())))
                                )
                        ))
        );
    }

    private void verifySummonsProsecutionReceivedPrivateEvent() throws EventStreamException {

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PRIVATE_EVENT_SUMMONS_PROSECUTION_RECEIVED),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutingAuthority", is("THREE RIVER")),
                                withJsonPath("$.submissionId", notNullValue()),
                                withJsonPath("$.prosecutionCases[0].caseMarker", is("Markers")),
                                withJsonPath("$.prosecutionCases[0].paymentReference", is("PAYREF123")),
                                withJsonPath("$.prosecutionCases[0].summonsCode", is("FIRST")),
                                withJsonPath("$.prosecutionCases[0].defendants[0]", notNullValue()),
                                withJsonPath("$.prosecutionCases[0].defendants[0].offences[0]", notNullValue()),
                                withJsonPath("$.prosecutionCases[0].defendants[0].offences[0].arrestDate", is(LocalDate.now().toString())))
                        )
                ))
        );

    }

    private void verifyUpdateCaseFileReceivedPrivateEvent() throws EventStreamException {

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PRIVATE_EVENT_UPDATE_CASE_FILE_RECEIVED),
                        payload().isJson(allOf(
                                withJsonPath("$.submissionId", notNullValue()),
                                withJsonPath("$.submissionStatus", is(SubmissionStatus.PENDING.name())))
                        )
                ))
        );

    }

    private Envelope<ChargeProsecution> buildChargeProsecutionEnvelope() {

        final ChargeProsecution chargeProsecution = ChargeProsecution.chargeProsecution()
                .withHearingDetails(HearingDetails.hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .build())
                .withProsecutingAuthority("THREE RIVER")
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withCaseMarker("Markers")
                        .withPaymentReference("PAYREF123")
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withOffences(Arrays.asList(Offence.offence()
                                        .withArrestDate(LocalDate.now())
                                        .build()))
                                .build()))
                        .build()))
                .withSubmissionId(UUID.fromString("ce1c9255-725f-4669-a7e5-78c07252c82d"))
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString())
                        .withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(chargeProsecution)
                .withName(PRIVATE_COMMAND_CHARGE_PROSECUTION)
                .withMetadataFrom(requestEnvelope);

    }

    private Envelope<SummonsProsecution> buildSummonsProsecutionEnvelope() {

        final SummonsProsecution summonsProsecution = SummonsProsecution.summonsProsecution()
                .withHearingDetails(HearingDetails.hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .build())
                .withProsecutingAuthority("THREE RIVER")
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withCaseMarker("Markers")
                        .withPaymentReference("PAYREF123")
                        .withSummonsCode("FIRST")
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withOffences(Arrays.asList(Offence.offence()
                                        .withArrestDate(LocalDate.now())
                                        .build()))
                                .build()))
                        .build()))
                .withSubmissionId(UUID.fromString("ce1c9255-725f-4669-a7e5-78c07252c82d"))
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString())
                        .withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(summonsProsecution)
                .withName(PRIVATE_COMMAND_CHARGE_PROSECUTION)
                .withMetadataFrom(requestEnvelope);

    }
    private Envelope<UpdateCivilCase> buildUpdateCaseFileEnvelope() {
        final UpdateCivilCase updateCivilCase = UpdateCivilCase.updateCivilCase()
                .withSubmissionId(UUID.randomUUID())
                .withSubmissionStatus(SubmissionStatus.PENDING.name())
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString())
                        .withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(updateCivilCase)
                .withName(PRIVATE_COMMAND_UPDATE_CASE_PROFILE)
                .withMetadataFrom(requestEnvelope);

    }

    @Test
    public void shouldHandleSubmitMaterialCommand() {

        assertThat(civilProsecutionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleSubmitMaterial")
                        .thatHandles(PRIVATE_COMMAND_SUBMIT_MATERIAL)));

    }

    @Test
    public void shouldRaiseMaterialSubmittedPrivateEvent() throws Exception {

        final UUID submissionId = randomUUID();
        final Envelope<SubmitMaterialCommand> envelope = buildSubmitMaterialEnvelope(submissionId, null);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(new MaterialSubmission());

        civilProsecutionHandler.handleSubmitMaterial(envelope);

        verifyMaterialSubmittedPrivateEvent(null);

    }

    @Test
    public void shouldRaiseMaterialSubmittedPrivateEventWithDefendantId() throws Exception {

        final UUID submissionId = randomUUID();
        final Envelope<SubmitMaterialCommand> envelope = buildSubmitMaterialEnvelope(submissionId, "defendant-123");
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(new MaterialSubmission());

        civilProsecutionHandler.handleSubmitMaterial(envelope);

        verifyMaterialSubmittedPrivateEvent("defendant-123");

    }

    private void verifyMaterialSubmittedPrivateEvent(final String expectedDefendantId) throws EventStreamException {

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        if (expectedDefendantId != null) {
            assertThat(envelopeStream, streamContaining(
                    jsonEnvelope(
                            metadata()
                                    .withName(PRIVATE_EVENT_MATERIAL_SUBMITTED),
                            payload().isJson(allOf(
                                    withJsonPath("$.submissionId", notNullValue()),
                                    withJsonPath("$.caseUrn", is("T20217654")),
                                    withJsonPath("$.prosecutingAuthority", is("THREE RIVER")),
                                    withJsonPath("$.materialType", is("CCTV_FOOTAGE")),
                                    withJsonPath("$.submissionStatus", is(SubmissionStatus.PENDING.name())),
                                    withJsonPath("$.defendantId", is(expectedDefendantId))))
                    ))
            );
        } else {
            assertThat(envelopeStream, streamContaining(
                    jsonEnvelope(
                            metadata()
                                    .withName(PRIVATE_EVENT_MATERIAL_SUBMITTED),
                            payload().isJson(allOf(
                                    withJsonPath("$.submissionId", notNullValue()),
                                    withJsonPath("$.caseUrn", is("T20217654")),
                                    withJsonPath("$.prosecutingAuthority", is("THREE RIVER")),
                                    withJsonPath("$.materialType", is("CCTV_FOOTAGE")),
                                    withJsonPath("$.submissionStatus", is(SubmissionStatus.PENDING.name()))))
                    ))
            );
        }

    }

    private Envelope<SubmitMaterialCommand> buildSubmitMaterialEnvelope(final UUID submissionId, final String defendantId) {

        final SubmitMaterialCommand submitMaterialCommand = SubmitMaterialCommand.submitMaterialCommand()
                .withSubmissionId(submissionId)
                .withMaterialId(randomUUID())
                .withCaseUrn("T20217654")
                .withProsecutingAuthority("THREE RIVER")
                .withMaterialType("CCTV_FOOTAGE")
                .withDefendantId(defendantId)
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString())
                        .withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(submitMaterialCommand)
                .withName(PRIVATE_COMMAND_SUBMIT_MATERIAL)
                .withMetadataFrom(requestEnvelope);

    }

}
