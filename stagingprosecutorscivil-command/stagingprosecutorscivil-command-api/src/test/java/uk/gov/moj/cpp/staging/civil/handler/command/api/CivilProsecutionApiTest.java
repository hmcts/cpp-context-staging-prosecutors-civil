package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCase;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCaseWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDateRangeDetails;
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
    public void shouldHandleOtherCase() {

        List<Defendant> defendants = new ArrayList<>();
        defendants.add(
                Defendant.defendant()
                        .withDefendantDetails(
                                DefendantDetails.defendantDetails()
                                        .withAsn("GAAAA01")
                                        .build()
                        )
                        .build()
        );
        List<ProsecutionCase> prosecutionCaseList = new ArrayList();
        prosecutionCaseList.add(
                ProsecutionCase.prosecutionCase()
                        .withUrn("urn1")
                        .withInformant("Adam")
                        .withCaseMarker("caseMarker")
                        .withPaymentReference("PAYREF102")
                        .withSummonsCode("FIRST")
                        .withDefendants(defendants)
                        .build()
        );
        OtherCase otherCase = OtherCase
                .otherCase()
                .withProsecutionCases(prosecutionCaseList)
                .withProsecutingAuthority("GAAAA01")
                .build();


        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.other-case")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        Envelope<UrlResponse> resultEnvelop = api.otherCase(Envelope.envelopeFrom(metadata, otherCase));
        UrlResponse urlResponse = resultEnvelop.payload();
        verify(sender).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        OtherCaseWithSubmissionId receivedOtherCase = (OtherCaseWithSubmissionId) capturedEnvelope.payload();
        assertThat(capturedEnvelope.metadata().name(), is("stagingcivil.command.other-case"));
        assertThat(receivedOtherCase.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(receivedOtherCase.getProsecutionCases().get(0).getUrn(), is("urn1"));
        assertNotNull(urlResponse.getSubmissionId());
    }

    @Test
    public void shouldHandleOtherCaseWithEnforcementFields() {
        List<ProsecutionCase> prosecutionCaseList = new ArrayList();
        prosecutionCaseList.add(
                ProsecutionCase.prosecutionCase()
                        .withUrn("urn-enforcement-1")
                        .withDefendants(new ArrayList<>())
                        .build()
        );
        OtherCase otherCase = OtherCase
                .otherCase()
                .withProsecutionCases(prosecutionCaseList)
                .withProsecutingAuthority("GAAAA01")
                .withRelatedReferenceNumber("GOB123456789")
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.other-case")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        Envelope<UrlResponse> resultEnvelop = api.otherCase(Envelope.envelopeFrom(metadata, otherCase));
        UrlResponse urlResponse = resultEnvelop.payload();
        verify(sender).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        OtherCaseWithSubmissionId receivedOtherCase = (OtherCaseWithSubmissionId) capturedEnvelope.payload();
        assertThat(capturedEnvelope.metadata().name(), is("stagingcivil.command.other-case"));
        assertThat(receivedOtherCase.getRelatedReferenceNumber(), is("GOB123456789"));
        assertThat(receivedOtherCase.getProsecutionCases().get(0).getUrn(), is("urn-enforcement-1"));
        assertNotNull(urlResponse.getSubmissionId());
    }

    @Test
    public void shouldHandleOtherCaseWithValidHearingDateRange() {
        final OtherCase otherCase = otherCaseWithHearingDateRange(
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(5));

        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.other-case")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        api.otherCase(Envelope.envelopeFrom(metadata, otherCase));

        verify(sender).send(envelopeCaptor.capture());
        final OtherCaseWithSubmissionId receivedOtherCase =
                (OtherCaseWithSubmissionId) envelopeCaptor.getValue().payload();
        assertThat(receivedOtherCase.getHearingDateRangeDetails().getCourtHearingLocation(), is("B01LY01"));
    }

    @Test
    public void shouldHandleOtherCaseWithHearingDateRangeOnBoundaryOf31DaysInThePast() {
        final OtherCase otherCase = otherCaseWithHearingDateRange(
                LocalDate.now().minusDays(31), LocalDate.now().minusDays(31));

        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.other-case")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        api.otherCase(Envelope.envelopeFrom(metadata, otherCase));

        verify(sender).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldRejectOtherCaseWhenEndDateRangeOfHearingIsBeforeStartDateRangeOfHearing() {
        final OtherCase otherCase = otherCaseWithHearingDateRange(
                LocalDate.now(), LocalDate.now().minusDays(1));

        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.other-case")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        assertThrows(BadRequestException.class,
                () -> api.otherCase(Envelope.envelopeFrom(metadata, otherCase)));

        verifyNoInteractions(sender);
    }

    @Test
    public void shouldHandleOtherCaseWhenStartDateRangeOfHearingIsMoreThan31DaysInThePast() {
        final OtherCase otherCase = otherCaseWithHearingDateRange(
                LocalDate.now().minusDays(32), LocalDate.now().plusDays(1));

        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.other-case")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        api.otherCase(Envelope.envelopeFrom(metadata, otherCase));

        verify(sender).send(envelopeCaptor.capture());
    }

    private OtherCase otherCaseWithHearingDateRange(final LocalDate startDate, final LocalDate endDate) {
        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(
                ProsecutionCase.prosecutionCase()
                        .withUrn("urn-enforcement-range-1")
                        .withDefendants(new ArrayList<>())
                        .build()
        );
        return OtherCase
                .otherCase()
                .withProsecutionCases(prosecutionCaseList)
                .withProsecutingAuthority("GAAAA01")
                .withRelatedReferenceNumber("GOB123456789")
                .withHearingDateRangeDetails(
                        HearingDateRangeDetails.hearingDateRangeDetails()
                                .withStartDateRangeOfHearing(startDate)
                                .withEndDateRangeOfHearing(endDate)
                                .withCourtHearingLocation("B01LY01")
                                .build())
                .build();
    }

    @Test
    public void shouldHandleSummons() {

        List<Defendant> defendants = new ArrayList<>();
        defendants.add(
            Defendant.defendant()
                .withDefendantDetails(
                    DefendantDetails.defendantDetails()
                        .withAsn("ASN123")
                        .build()
                )
                .build()
        );
        List<ProsecutionCase> prosecutionCaseList = new ArrayList();
        prosecutionCaseList.add(
            ProsecutionCase.prosecutionCase()
                .withUrn("urn1")
                .withInformant("Adam")
                .withCaseMarker("caseMarker")
                .withPaymentReference("PAYREF102")
                .withSummonsCode("FIRST")
                .withDefendants(defendants)
                .build()
        );
        Summons summons = Summons
                .summons()
                .withProsecutionCases(prosecutionCaseList)
                .withProsecutingAuthority("GAAAA01")
                .build();


        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.summons")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope commandEnvelope = Envelope.envelopeFrom(metadata, summons);

        Envelope<UrlResponse> resultEnvelop = api.summons(commandEnvelope);
        UrlResponse urlResponse = resultEnvelop.payload();
        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("stagingcivil.command.summons"));
        assertNotNull(urlResponse.getSubmissionId());

        SummonsWithSubmissionId receivedSummonProsecution = (SummonsWithSubmissionId) capturedEnvelope.payload();
        assertThat(receivedSummonProsecution.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(receivedSummonProsecution.getProsecutionCases().get(0).getUrn(), is("urn1"));
        assertNotNull(urlResponse.getSubmissionId());
    }
}
