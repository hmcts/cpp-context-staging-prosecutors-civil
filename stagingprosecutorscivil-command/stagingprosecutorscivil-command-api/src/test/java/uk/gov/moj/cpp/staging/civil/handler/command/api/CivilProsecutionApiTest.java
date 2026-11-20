package uk.gov.moj.cpp.staging.civil.handler.command.api;

import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError;
import javax.json.JsonArray;
import javax.json.JsonObject;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.staging.civil.handler.command.api.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.civil.handler.command.api.validators.OffenceValidator;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCase;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCaseWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SubmitMaterialWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDateRangeDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SummonsProsecutionCase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

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

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    private final OffenceValidator offenceValidator = new OffenceValidator();

    @BeforeEach
    public void setup() {
        api.baseResponseURL = "test-base-url/";
        setField(api, "uuidProducer", uuidProducer);
        setField(api, "jsonSchemaValidator", jsonSchemaValidator);
        setField(api, "offenceValidator", offenceValidator);
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
                        .withOffences(new ArrayList<>())
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
        assertThat(receivedOtherCase.getProsecutionCases().get(0).getRelatedReferenceNumber(), is("RELREF-1"));
        assertNotNull(urlResponse.getSubmissionId());
    }

    @Test
    public void shouldHandleOtherCaseWithValidHearingDateRange() {
        final OtherCase otherCase = otherCaseWithHearingDateRange(
                LocalDate.now(), LocalDate.now().plusDays(5));

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
    public void shouldRejectOtherCaseWhenStartDateRangeOfHearingIsInThePast() {
        final OtherCase otherCase = otherCaseWithHearingDateRange(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

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
    public void shouldRejectOtherCaseWhenStartDateRangeOfHearingIsMoreThan31DaysInThePast() {
        final OtherCase otherCase = otherCaseWithHearingDateRange(
                LocalDate.now().minusDays(32), LocalDate.now().plusDays(1));

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
    public void shouldRejectOtherCaseWhenDefendantHasOffencesWithDuplicateOffenceSequenceNumbers() {
        final Offence offence1 = Offence.offence()
                .withOffenceDetails(OffenceDetails.offenceDetails()
                        .withCjsOffenceCode("CA03013")
                        .withOffenceSequenceNo(1)
                        .build())
                .build();
        final Offence offence2 = Offence.offence()
                .withOffenceDetails(OffenceDetails.offenceDetails()
                        .withCjsOffenceCode("CA03014")
                        .withOffenceSequenceNo(1)
                        .build())
                .build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withDefendantDetails(DefendantDetails.defendantDetails().withAsn("GAAAA01").build())
                .withOffences(asList(offence1, offence2))
                .build());

        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(ProsecutionCase.prosecutionCase()
                .withUrn("urn-duplicate-offence-seq")
                .withInformant("Adam")
                .withDefendants(defendants)
                .build());

        final OtherCase otherCase = OtherCase
                .otherCase()
                .withProsecutionCases(prosecutionCaseList)
                .withProsecutingAuthority("GAAAA01")
                .build();

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
    public void shouldHandleOtherCaseWhenDefendantHasOffencesWithUniqueOffenceSequenceNumbers() {
        final Offence offence1 = Offence.offence()
                .withOffenceDetails(OffenceDetails.offenceDetails()
                        .withCjsOffenceCode("CA03013")
                        .withOffenceSequenceNo(1)
                        .build())
                .build();
        final Offence offence2 = Offence.offence()
                .withOffenceDetails(OffenceDetails.offenceDetails()
                        .withCjsOffenceCode("CA03014")
                        .withOffenceSequenceNo(2)
                        .build())
                .build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withDefendantDetails(DefendantDetails.defendantDetails().withAsn("GAAAA01").build())
                .withOffences(asList(offence1, offence2))
                .build());

        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(ProsecutionCase.prosecutionCase()
                .withUrn("urn-unique-offence-seq")
                .withInformant("Adam")
                .withDefendants(defendants)
                .build());

        final OtherCase otherCase = OtherCase
                .otherCase()
                .withProsecutionCases(prosecutionCaseList)
                .withProsecutingAuthority("GAAAA01")
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.other-case")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        api.otherCase(Envelope.envelopeFrom(metadata, otherCase));

        verify(sender).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldSendRejectComplaintsFileCommandSplittingCaseAndDefendantErrorsAndReturnStatusUrl() {
        final Metadata metadata = metadataBuilder()
                .withName("stagingcivil.summons")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
        final List<ComplaintsFileValidationError> errors = List.of(
                new ComplaintsFileValidationError(1L, "SCIV00001", "DEF00001", "REQUIRED_FIELD_MISSING",
                        "defendant.individual.nameDetails.surname", "", "Row 1: required column ... is blank"),
                new ComplaintsFileValidationError(1L, "SCIV00001", "DEF00001", "INVALID_DATE_FORMAT",
                        "offence.offenceDetails.laidDate", "31/02/2022", "Row 1: ... invalid date"),
                new ComplaintsFileValidationError(1L, "SCIV00001", null, "INVALID_DATE_FORMAT",
                        "hearingDetails.dateOfHearing", "04/02/2022", "Row 1: ... invalid date"),
                new ComplaintsFileValidationError(2L, "SCIV00002", "DEF00002", "INVALID_VALUE",
                        "case.summonsCode", "AB", "Row 2: ... invalid value"),
                new ComplaintsFileValidationError(4L, null, null, "INVALID_ROW", null, "", "Row 4 has 2 columns ..."));

        final Envelope<UrlResponse> result = api.rejectComplaintsFile(metadata, "GAAAA01", "complaints.csv",
                "Richard Chapman", "TVL", errors);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        final JsonEnvelope sent = jsonEnvelopeCaptor.getValue();
        assertThat(sent.metadata().name(), is("stagingcivil.command.reject-complaints-file"));
        assertThat(sent.metadata().userId(), is(metadata.userId()));

        final JsonObject payload = sent.payloadAsJsonObject();
        final UUID submissionId = UUID.fromString(payload.getString("submissionId"));
        assertThat(payload.getString("prosecutingAuthority"), is("GAAAA01"));
        assertThat(payload.getString("fileName"), is("complaints.csv"));
        assertThat(payload.getString("submittedByUserName"), is("Richard Chapman"));
        assertThat(payload.getString("prosecutorShortName"), is("TVL"));

        final JsonArray caseErrors = payload.getJsonArray("caseErrors");
        assertThat(caseErrors.size(), is(3));
        assertThat(caseErrors.getJsonObject(0).getString("prosecutorCaseReference"), is("SCIV00001"));
        assertThat(caseErrors.getJsonObject(0).getJsonArray("problems").getJsonObject(0)
                .getJsonArray("values").getJsonObject(0).getString("key"), is("hearingDetails.dateOfHearing"));
        assertThat(caseErrors.getJsonObject(1).getString("prosecutorCaseReference"), is("SCIV00002"));
        final JsonObject invalidRow = caseErrors.getJsonObject(2);
        assertThat(invalidRow.containsKey("prosecutorCaseReference"), is(false));
        assertThat(invalidRow.getJsonArray("problems").getJsonObject(0).getJsonArray("values").getJsonObject(0).getString("key"), is("row"));

        final JsonArray defendantErrors = payload.getJsonArray("defendantErrors");
        assertThat(defendantErrors.size(), is(1));
        final JsonObject defendant = defendantErrors.getJsonObject(0);
        assertThat(defendant.getString("prosecutorDefendantReference"), is("DEF00001"));
        assertThat(defendant.getJsonArray("problems").size(), is(2));
        final JsonObject missingSurname = defendant.getJsonArray("problems").getJsonObject(0);
        assertThat(missingSurname.getString("code"), is("REQUIRED_FIELD_MISSING"));
        assertThat(missingSurname.getJsonArray("values").getJsonObject(0).getString("key"), is("defendant.individual.nameDetails.surname"));
        assertThat(missingSurname.getJsonArray("values").getJsonObject(0).getString("value"), is(""));
        final JsonObject badLaidDate = defendant.getJsonArray("problems").getJsonObject(1);
        assertThat(badLaidDate.getJsonArray("values").getJsonObject(0).getString("key"), is("offence.offenceDetails.laidDate"));

        assertThat(result.payload().getSubmissionId(), is(submissionId));
        assertThat(result.payload().getStatusURL(), is("test-base-url/" + submissionId));
    }

    @Test
    public void shouldKeepRowsApartWhenTheyShareADefendantReferenceOrHaveNoReference() {
        final Metadata metadata = metadataBuilder().withName("stagingcivil.summons").withId(randomUUID()).build();

        api.rejectComplaintsFile(metadata, "GAAAA01", "complaints.csv", null, null, List.of(
                new ComplaintsFileValidationError(1L, "SCIV00001", "D1", "INVALID_VALUE", "defendant.asn", "X", "msg"),
                new ComplaintsFileValidationError(2L, "SCIV00002", "D1", "INVALID_VALUE", "defendant.asn", "Y", "msg"),
                new ComplaintsFileValidationError(3L, null, null, "REQUIRED_FIELD_MISSING", "case.urn", "", "msg"),
                new ComplaintsFileValidationError(7L, null, null, "REQUIRED_FIELD_MISSING", "case.urn", "", "msg"),
                new ComplaintsFileValidationError(8L, "SCIV00008", "D8", "INVALID_VALUE", "offence", "", "msg")));

        verify(sender).send(jsonEnvelopeCaptor.capture());
        final JsonObject payload = jsonEnvelopeCaptor.getValue().payloadAsJsonObject();
        final JsonArray defendantErrors = payload.getJsonArray("defendantErrors");
        assertThat(defendantErrors.size(), is(3));
        assertThat(defendantErrors.getJsonObject(0).getString("prosecutorDefendantReference"), is("D1"));
        assertThat(defendantErrors.getJsonObject(1).getString("prosecutorDefendantReference"), is("D1"));
        assertThat(defendantErrors.getJsonObject(2).getString("prosecutorDefendantReference"), is("D8"));
        final JsonArray caseErrors = payload.getJsonArray("caseErrors");
        assertThat(caseErrors.size(), is(2));
        assertThat(caseErrors.getJsonObject(0).containsKey("prosecutorCaseReference"), is(false));
        assertThat(caseErrors.getJsonObject(1).containsKey("prosecutorCaseReference"), is(false));
    }

    @Test
    public void shouldOmitCaseErrorsAndDefendantReferenceWhenAbsent() {
        final Metadata metadata = metadataBuilder().withName("stagingcivil.summons").withId(randomUUID()).build();

        api.rejectComplaintsFile(metadata, "GAAAA01", "complaints.csv", null, null, List.of(
                new ComplaintsFileValidationError(1L, "SCIV00001", null, "REQUIRED_FIELD_MISSING",
                        "defendant.prosecutorDefendantId", "", "msg")));

        verify(sender).send(jsonEnvelopeCaptor.capture());
        final JsonObject payload = jsonEnvelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(payload.containsKey("caseErrors"), is(false));
        assertThat(payload.getJsonArray("defendantErrors").getJsonObject(0).containsKey("prosecutorDefendantReference"), is(false));
    }

    @Test
    public void shouldOmitOptionalUploadDetailsFromRejectComplaintsFileCommandWhenAbsent() {
        final Metadata metadata = metadataBuilder().withName("stagingcivil.summons").withId(randomUUID()).build();

        api.rejectComplaintsFile(metadata, "GAAAA01", null, null, null, List.of(
                new ComplaintsFileValidationError(1L, "SCIV00001", null, "INVALID_VALUE", "case.summonsCode", "AB", "msg")));

        verify(sender).send(jsonEnvelopeCaptor.capture());
        final JsonObject payload = jsonEnvelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(payload.containsKey("fileName"), is(false));
        assertThat(payload.containsKey("submittedByUserName"), is(false));
        assertThat(payload.containsKey("prosecutorShortName"), is(false));
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
    public void shouldSubmitMaterial() {
        final UUID materialId = randomUUID();
        final UUID submissionId = randomUUID();
        final String defendantId = randomUUID().toString();
        when(uuidProducer.generateUUID()).thenReturn(submissionId);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("stagingcivil.submit-material"),
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
        assertThat(capturedEnvelope.metadata().name(), is("stagingcivil.command.submit-material"));

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
                metadataWithRandomUUID("stagingcivil.submit-material"),
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
                metadataWithRandomUUID("stagingcivil.submit-material"),
                createObjectBuilder()
                        .add("material", randomUUID().toString())
                        .add("caseUrn", "urn1")
                        .add("materialType", "Case Summary")
                        .add("prosecutingAuthority", "AUTH001")
                        .build());

        assertThrows(BadRequestException.class, () -> api.submitMaterial(envelope));
    }
}
