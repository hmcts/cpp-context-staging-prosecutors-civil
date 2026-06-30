package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

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
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

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
