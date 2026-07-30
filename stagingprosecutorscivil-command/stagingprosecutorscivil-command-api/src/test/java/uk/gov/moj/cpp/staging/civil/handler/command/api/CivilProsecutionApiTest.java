package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.staging.civil.handler.command.api.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCase;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCaseWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SubmitMaterialWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SummonsProsecutionCase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Mock
    private UUIDProducer uuidProducer;

    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @BeforeEach
    public void setup() {
        api.baseResponseURL = "test-base-url/";
        setField(api, "uuidProducer", uuidProducer);
        setField(api, "jsonSchemaValidator", jsonSchemaValidator);
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
                        .withRelatedReferenceNumber("RELREF-1")
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
        List<SummonsProsecutionCase> prosecutionCaseList = new ArrayList();
        prosecutionCaseList.add(
            SummonsProsecutionCase.summonsProsecutionCase()
                .withUrn("urn1")
                .withInformant("Adam")
                .withCaseMarker("caseMarker")
                .withPaymentReference("PAYREF102")
                .withSummonsCode("A")
                .withRelatedReferenceNumber("RELREF-1")
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
        assertThat(receivedSummonProsecution.getProsecutionCases().get(0).getRelatedReferenceNumber(), is("RELREF-1"));
        assertNotNull(urlResponse.getSubmissionId());
    }

    @Test
    public void shouldSubmitMaterial() {
        final UUID materialId = randomUUID();
        final UUID submissionId = randomUUID();
        final String defendantId = randomUUID().toString();
        when(uuidProducer.generateUUID()).thenReturn(submissionId);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("stagingprosecutorscivil.submit-material"),
                createObjectBuilder()
                        .add("material", materialId.toString())
                        .add("caseUrn", "urn1")
                        .add("materialType", "Plea")
                        .add("prosecutingAuthority", "AUTH001")
                        .add("defendantId", defendantId)
                        .build());

        final Envelope<UrlResponse> result = api.submitMaterial(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("stagingprosecutorscivil.command.submit-material"));

        final SubmitMaterialWithSubmissionId payload = (SubmitMaterialWithSubmissionId) capturedEnvelope.payload();
        assertThat(payload.getCaseUrn(), is("urn1"));
        assertThat(payload.getMaterialId(), is(materialId));
        assertThat(payload.getMaterialType(), is("Plea"));
        assertThat(payload.getProsecutingAuthority(), is("AUTH001"));
        assertThat(payload.getDefendantId(), is(defendantId));
        assertThat(payload.getSubmissionId(), is(submissionId));
        assertThat(result.payload().getSubmissionId(), is(submissionId));
    }

    @Test
    public void shouldSubmitMaterialWithoutDefendantId() {
        final UUID materialId = randomUUID();
        final UUID submissionId = randomUUID();
        when(uuidProducer.generateUUID()).thenReturn(submissionId);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("stagingprosecutorscivil.submit-material"),
                createObjectBuilder()
                        .add("material", materialId.toString())
                        .add("caseUrn", "urn1")
                        .add("materialType", "Indictment")
                        .add("prosecutingAuthority", "AUTH001")
                        .build());

        api.submitMaterial(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final SubmitMaterialWithSubmissionId payload = (SubmitMaterialWithSubmissionId) envelopeCaptor.getValue().payload();
        assertThat(payload.getDefendantId(), is(nullValue()));
        assertThat(payload.getSubmissionId(), is(submissionId));
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenSchemaValidationFails() {
        doThrow(new JsonSchemaValidationException("Schema validation failed"))
                .when(jsonSchemaValidator).validate(anyString(), anyString());

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("stagingprosecutorscivil.submit-material"),
                createObjectBuilder()
                        .add("material", randomUUID().toString())
                        .add("caseUrn", "urn1")
                        .add("materialType", "Case Summary")
                        .add("prosecutingAuthority", "AUTH001")
                        .build());

        assertThrows(BadRequestException.class, () -> api.submitMaterial(envelope));
    }
}
