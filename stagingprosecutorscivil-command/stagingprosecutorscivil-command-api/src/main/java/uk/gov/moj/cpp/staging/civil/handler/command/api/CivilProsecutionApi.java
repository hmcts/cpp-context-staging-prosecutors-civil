package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.util.CommandApiUtil.buildHearingDetails;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.util.CommandApiUtil.buildProsecutionCase;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.util.CommandApiUtil.getCsvParserWithRightConfig;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.civil.handler.command.api.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.ChargeProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.ChargeProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SubmitMaterialWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDateCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Organisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianNameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianOrganisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class CivilProsecutionApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(CivilProsecutionApi.class);
    @Inject
    @Value(key = "stagingprosecutorscivil.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";

    private final Sender sender;
    @Inject
    private UUIDProducer uuidProducer;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    public CivilProsecutionApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles("stagingprosecutorscivil.charge-prosecution")
    public Envelope<UrlResponse> chargeProsecution(final Envelope<ChargeProsecution> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final ChargeProsecution chargeProsecution = envelope.payload();

        final ChargeProsecutionWithSubmissionId chargeProsecutionWithSubmissionId
                = ChargeProsecutionWithSubmissionId.chargeProsecutionWithSubmissionId()
                .withProsecutionCases(chargeProsecution.getProsecutionCases())
                .withProsecutingAuthority(chargeProsecution.getProsecutingAuthority())
                .withHearingDetails(chargeProsecution.getHearingDetails())
                .withSubmissionId(submissionId)
                .build();

        LOGGER.info("Received submission at stagingprosecutorscivil.charge-prosecution  with submissionId {}",submissionId);
        sender.send(envelop(chargeProsecutionWithSubmissionId)
                .withName("stagingprosecutorscivil.command.charge-prosecution")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                .withSubmissionId(submissionId).build());


    }

    @Handles("stagingprosecutorscivil.summons-prosecution")
    public Envelope<UrlResponse> summonsProsecution(final Envelope<SummonsProsecution> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final SummonsProsecution summonsProsecution = envelope.payload();
        final SummonsProsecutionWithSubmissionId summonsProsecutionWithSubmissionId
                = SummonsProsecutionWithSubmissionId.summonsProsecutionWithSubmissionId()
                .withProsecutionCases(summonsProsecution.getProsecutionCases())
                .withProsecutingAuthority(summonsProsecution.getProsecutingAuthority())
                .withHearingDetails(summonsProsecution.getHearingDetails())
                .withSubmissionId(submissionId)
                .build();
        LOGGER.info("Received submission at  stagingprosecutorscivil.summons-prosecution with submissionId {}",submissionId);
        sender.send(envelop(summonsProsecutionWithSubmissionId)
                .withName("stagingprosecutorscivil.command.summons-prosecution")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                        .withSubmissionId(submissionId).build());

    }

    @Handles("stagingprosecutorscivil.submit-material")
    public Envelope<UrlResponse> submitMaterial(final JsonEnvelope envelope) {
        LOGGER.info("Received submission at stagingprosecutorscivil.submit-material");
        final String defendantIdField = "defendantId";
        final JsonObject requestPayload = envelope.payloadAsJsonObject();

        try {
            jsonSchemaValidator.validate(requestPayload.toString(), envelope.metadata().name());
        } catch (JsonSchemaValidationException e) {
            throw new BadRequestException("Error submitting material, request has schema violations", e);
        }

        final UUID submissionId = uuidProducer.generateUUID();
        final SubmitMaterialWithSubmissionId.Builder submitMaterialWithSubmissionIdBuilder = SubmitMaterialWithSubmissionId.submitMaterialWithSubmissionId()
                .withSubmissionId(submissionId)
                .withMaterialId(UUID.fromString(requestPayload.getString("material")))
                .withCaseUrn(requestPayload.getString("caseUrn"))
                .withMaterialType(requestPayload.getString("materialType"))
                .withProsecutingAuthority(requestPayload.getString("prosecutingAuthority"));

        if (requestPayload.containsKey(defendantIdField)) {
            submitMaterialWithSubmissionIdBuilder.withDefendantId(requestPayload.getString(defendantIdField));
        }

        sender.send(envelop(submitMaterialWithSubmissionIdBuilder.build())
                .withName("stagingprosecutorscivil.command.submit-material")
                .withMetadataFrom(envelope));

        return envelopeFrom(
                envelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId, submissionId));
    }

    @Handles("stagingprosecutorscivil.upload-bulk-prosecution")
    public Envelope<UrlResponse> uploadBulkProsecution(final JsonEnvelope envelope) {
        LOGGER.info("Received submission at stagingprosecutorscivil.upload-bulk-prosecution");
        final String csvContent = envelope.payloadAsJsonObject().getString("file");

        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        String prosecutingAuthority = null;
        HearingDetails hearingDetails = null;

        try (CSVParser parser = getCsvParserWithRightConfig(csvContent)) {
            for (final CSVRecord row : parser) {
                if (prosecutingAuthority == null) {
                    prosecutingAuthority = row.get("prosecutingAuthority");
                    hearingDetails = buildHearingDetails(row);
                }
                prosecutionCases.add(buildProsecutionCase(row));
            }
        } catch (final IOException e) {
            throw new BadRequestException("Failed to parse CSV", e);
        }

        final ChargeProsecution chargeProsecution = ChargeProsecution.chargeProsecution()
                .withProsecutingAuthority(prosecutingAuthority)
                .withHearingDetails(hearingDetails)
                .withProsecutionCases(prosecutionCases)
                .build();

        try {
            final String chargeJson = objectToJsonObjectConverter.convert(chargeProsecution).toString();
            jsonSchemaValidator.validate(chargeJson, "stagingprosecutorscivil.charge-prosecution");
        } catch (final JsonSchemaValidationException e) {
            throw new BadRequestException("Charge prosecution schema validation failed", e);
        }

        return chargeProsecution(Envelope.envelopeFrom(envelope.metadata(), chargeProsecution));
    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }
}
