package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
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

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
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
                .withMaterial(UUID.fromString(requestPayload.getString("material")))
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

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }

}
