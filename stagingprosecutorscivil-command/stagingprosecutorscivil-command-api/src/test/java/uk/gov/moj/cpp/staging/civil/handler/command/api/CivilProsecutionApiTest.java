package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.ChargeProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.ChargeProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilProsecutionApiTest {
    @InjectMocks
    private CivilProsecutionApi api;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @BeforeEach
    public void setup() {
        api.baseResponseURL = "test-base-url/";
    }

    @Test
    public void shouldHandleChargeProsecution() {

        final ChargeProsecution chargeProsecution = ChargeProsecution
                .chargeProsecution()
                .withProsecutionCases(prosecutionCases("urn1"))
                .withProsecutingAuthority("GAAAA01")
                .withHearingDetails(validHearingDetails())
                .build();


        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutorscivil.charge-prosecution")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<UrlResponse> resultEnvelop = api.chargeProsecution(Envelope.envelopeFrom(metadata, chargeProsecution));
        final UrlResponse urlResponse = resultEnvelop.payload();
        verify(sender).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        final ChargeProsecutionWithSubmissionId receivedChargeProsecution = (ChargeProsecutionWithSubmissionId) capturedEnvelope.payload();
        assertThat(capturedEnvelope.metadata().name(), is("stagingprosecutorscivil.command.charge-prosecution"));
        assertThat(receivedChargeProsecution.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(receivedChargeProsecution.getProsecutionCases().get(0).getUrn(), is("urn1"));
        assertThat(receivedChargeProsecution.getProsecutionCases().get(0).getRelatedReferenceNumber(), is("RELREF-1"));
        assertThat(receivedChargeProsecution.getHearingDetails().getHearingType(), is("FINAL"));
        assertNotNull(urlResponse.getSubmissionId());
    }

    @Test
    public void shouldThrowWhenChargeHearingDetailsIsNull() {
        final ChargeProsecution chargeProsecution = ChargeProsecution
                .chargeProsecution()
                .withProsecutionCases(prosecutionCases("urn1"))
                .withProsecutingAuthority("GAAAA01")
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutorscivil.charge-prosecution")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<ChargeProsecution> envelope = Envelope.envelopeFrom(metadata, chargeProsecution);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> api.chargeProsecution(envelope));
        assertThat(ex.getMessage(), is("hearingDetails.dateOfHearing is required"));
        verifyNoInteractions(sender);
    }

    @Test
    public void shouldThrowWhenChargeDateOfHearingIsNull() {
        final ChargeProsecution chargeProsecution = ChargeProsecution
                .chargeProsecution()
                .withProsecutionCases(prosecutionCases("urn1"))
                .withProsecutingAuthority("GAAAA01")
                .withHearingDetails(HearingDetails.hearingDetails()
                        .withCourtHearingLocation("CRT0001")
                        .build())
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutorscivil.charge-prosecution")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<ChargeProsecution> envelope = Envelope.envelopeFrom(metadata, chargeProsecution);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> api.chargeProsecution(envelope));
        assertThat(ex.getMessage(), is("hearingDetails.dateOfHearing is required"));
        verifyNoInteractions(sender);
    }

    @Test
    public void shouldHandleSummonsProsecution() {

        final SummonsProsecution summonsProsecution = SummonsProsecution
                .summonsProsecution()
                .withProsecutionCases(prosecutionCases("urn1"))
                .withProsecutingAuthority("GAAAA01")
                .withHearingDetails(validHearingDetails())
                .build();


        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutorscivil.summons-prosecution")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<SummonsProsecution> commandEnvelope = Envelope.envelopeFrom(metadata, summonsProsecution);

        final Envelope<UrlResponse> resultEnvelop = api.summonsProsecution(commandEnvelope);
        final UrlResponse urlResponse = resultEnvelop.payload();
        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("stagingprosecutorscivil.command.summons-prosecution"));
        assertNotNull(urlResponse.getSubmissionId());

        final SummonsProsecutionWithSubmissionId receivedSummonProsecution = (SummonsProsecutionWithSubmissionId) capturedEnvelope.payload();
        assertThat(receivedSummonProsecution.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(receivedSummonProsecution.getProsecutionCases().get(0).getUrn(), is("urn1"));
        assertThat(receivedSummonProsecution.getProsecutionCases().get(0).getRelatedReferenceNumber(), is("RELREF-1"));
        assertThat(receivedSummonProsecution.getHearingDetails().getHearingType(), is("FINAL"));
        assertNotNull(urlResponse.getSubmissionId());
    }

    @Test
    public void shouldThrowWhenSummonsHearingDetailsIsNull() {
        final SummonsProsecution summonsProsecution = SummonsProsecution
                .summonsProsecution()
                .withProsecutionCases(prosecutionCases("urn1"))
                .withProsecutingAuthority("GAAAA01")
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutorscivil.summons-prosecution")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<SummonsProsecution> envelope = Envelope.envelopeFrom(metadata, summonsProsecution);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> api.summonsProsecution(envelope));
        assertThat(ex.getMessage(), is("hearingDetails.dateOfHearing is required"));
        verifyNoInteractions(sender);
    }

    @Test
    public void shouldThrowWhenSummonsDateOfHearingIsNull() {
        final SummonsProsecution summonsProsecution = SummonsProsecution
                .summonsProsecution()
                .withProsecutionCases(prosecutionCases("urn1"))
                .withProsecutingAuthority("GAAAA01")
                .withHearingDetails(HearingDetails.hearingDetails()
                        .withCourtHearingLocation("CRT0001")
                        .build())
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutorscivil.summons-prosecution")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<SummonsProsecution> envelope = Envelope.envelopeFrom(metadata, summonsProsecution);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> api.summonsProsecution(envelope));
        assertThat(ex.getMessage(), is("hearingDetails.dateOfHearing is required"));
        verifyNoInteractions(sender);
    }

    private static List<ProsecutionCase> prosecutionCases(final String urn) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(
                Defendant.defendant()
                        .withDefendantDetails(
                                DefendantDetails.defendantDetails()
                                        .withAsn("GAAAA01")
                                        .build()
                        )
                        .build()
        );
        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(
                ProsecutionCase.prosecutionCase()
                        .withUrn(urn)
                        .withInformant("Adam")
                        .withCaseMarker("caseMarker")
                        .withPaymentReference("PAYREF102")
                        .withRelatedReferenceNumber("RELREF-1")
                        .withSummonsCode("FIRST")
                        .withDefendants(defendants)
                        .build()
        );
        return prosecutionCaseList;
    }

    private static HearingDetails validHearingDetails() {
        return HearingDetails.hearingDetails()
                .withCourtHearingLocation("CRT0001")
                .withDateOfHearing(LocalDate.of(2026, 6, 1))
                .withTimeOfHearing("09:30:00")
                .withHearingType("FINAL")
                .build();
    }
}
