package uk.gov.moj.cpp.staging.civil.handler.command.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError;
import uk.gov.moj.cpp.staging.civil.handler.command.api.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.civil.handler.command.api.validators.OffenceValidator;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.*;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDateRangeDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

@ServiceComponent(COMMAND_API)
public class CivilProsecutionApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(CivilProsecutionApi.class);
    @Inject
    @Value(key = "stagingprosecutorscivil.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";
    private static final String REJECT_COMPLAINTS_FILE_COMMAND = "stagingcivil.command.reject-complaints-file";

    private final Sender sender;
    @Inject
    private UUIDProducer uuidProducer;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;

    @Inject
    private OffenceValidator offenceValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public CivilProsecutionApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles("stagingcivil.other-case")
    public Envelope<UrlResponse> otherCase(final Envelope<OtherCase> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final OtherCase otherCase = envelope.payload();

        validateHearingDateRangeDetails(otherCase.getHearingDateRangeDetails());
        validateOffences(otherCase);

        final OtherCaseWithSubmissionId otherCaseWithSubmissionId
                = OtherCaseWithSubmissionId.otherCaseWithSubmissionId()
                .withProsecutionCases(otherCase.getProsecutionCases())
                .withProsecutingAuthority(otherCase.getProsecutingAuthority())
                .withHearingDetails(otherCase.getHearingDetails())
                .withHearingDateRangeDetails(otherCase.getHearingDateRangeDetails())
                .withRelatedReferenceNumber(otherCase.getRelatedReferenceNumber())
                .withSubmissionId(submissionId)
                .build();

        LOGGER.info("Received submission at stagingcivil.other-case with submissionId {}",submissionId);
        sender.send(envelop(otherCaseWithSubmissionId)
                .withName("stagingcivil.command.other-case")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                .withSubmissionId(submissionId).build());


    }

    @Handles("stagingcivil.summons")
    public Envelope<UrlResponse> summons(final Envelope<Summons> envelope) {
        return summonsProsecution(envelope, null, null, null);
    }

    /**
     * Overload taking {@code fileName}, {@code submittedByUserName} and {@code prosecutorShortName}
     * — called directly by {@code DefaultCommandApiComplaintsFilesResource} for the complaints CSV
     * upload path, which resolves those values server-side (they are never part of the public
     * {@code stagingcivil.summons} request schema). The framework's
     * reflection-based dispatch of the direct JSON {@code stagingcivil.summons}
     * submission still finds the single-{@code Envelope}-parameter {@link #summons(Envelope)}
     * via its {@code @Handles} annotation, so this overload is invisible to it.
     */
    public Envelope<UrlResponse> summonsProsecution(final Envelope<Summons> envelope,
                                                      final String fileName,
                                                      final String submittedByUserName,
                                                      final String prosecutorShortName) {
        final UUID submissionId = UUID.randomUUID();
        final Summons summons = envelope.payload();
        final SummonsWithSubmissionId summonsWithSubmissionId
                = SummonsWithSubmissionId.summonsWithSubmissionId()
                .withProsecutionCases(summons.getProsecutionCases())
                .withProsecutingAuthority(summons.getProsecutingAuthority())
                .withHearingDetails(summons.getHearingDetails())
                .withSubmissionId(submissionId)
                .withFileName(fileName)
                .withSubmittedByUserName(submittedByUserName)
                .withProsecutorShortName(prosecutorShortName)
                .build();
        LOGGER.info("Received submission at stagingcivil.summons with submissionId {}", submissionId);
        sender.send(envelop(summonsWithSubmissionId)
                .withName("stagingcivil.command.summons")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                        .withSubmissionId(submissionId).build());
    }

    /**
     * Records a complaints CSV upload that could not be turned into a valid summons submission
     * because of schema-level problems (blank required columns, malformed values, JSON-schema
     * violations). The upload is still accepted - the caller gets a {@code submissionId} like any
     * other upload - and the submission is created directly in {@code FAILED} status with the
     * problems stored as case errors, so they appear in the submission's error report alongside
     * the business-validation errors raised later by Prosecution Case File.
     */
    public Envelope<UrlResponse> rejectComplaintsFile(final Metadata metadata,
                                                     final String prosecutingAuthority,
                                                     final String fileName,
                                                     final String submittedByUserName,
                                                     final String prosecutorShortName,
                                                     final List<ComplaintsFileValidationError> errors) {
        final UUID submissionId = UUID.randomUUID();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("prosecutingAuthority", prosecutingAuthority)
                .add("caseErrors", toCaseProblems(errors));
        addIfPresent(payload, "fileName", fileName);
        addIfPresent(payload, "submittedByUserName", submittedByUserName);
        addIfPresent(payload, "prosecutorShortName", prosecutorShortName);

        LOGGER.info("Complaints file for submissionId {} failed schema validation with {} error(s)", submissionId, errors.size());
        sender.send(JsonEnvelope.envelopeFrom(
                metadataFrom(metadata).withId(UUID.randomUUID()).withName(REJECT_COMPLAINTS_FILE_COMMAND),
                payload.build()));

        return Envelope.envelopeFrom(metadata,
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId)
                        .withSubmissionId(submissionId).build());
    }

    /**
     * Groups the errors into Prosecution Case File {@code case-problem} entries - one per case
     * reference, in file order - the same shape PCF uses for business-validation case errors.
     */
    private static JsonArray toCaseProblems(final List<ComplaintsFileValidationError> errors) {
        final Map<String, JsonArrayBuilder> problemsByReference = new LinkedHashMap<>();
        errors.forEach(error -> problemsByReference
                .computeIfAbsent(error.getCaseReference(), reference -> createArrayBuilder())
                .add(createObjectBuilder()
                        .add("code", error.getCode())
                        .add("values", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("key", error.getField() != null ? error.getField() : "row")
                                        .add("value", error.getValue())))));

        final JsonArrayBuilder caseProblems = createArrayBuilder();
        problemsByReference.forEach((reference, problems) -> {
            final JsonObjectBuilder caseProblem = createObjectBuilder().add("problems", problems);
            addIfPresent(caseProblem, "prosecutorCaseReference", reference);
            caseProblems.add(caseProblem);
        });
        return caseProblems.build();
    }

    private static void addIfPresent(final JsonObjectBuilder builder, final String name, final String value) {
        if (value != null) {
            builder.add(name, value);
        }
    }

    @Handles("stagingcivil.submit-material")
    public Envelope<UrlResponse> submitMaterial(final JsonEnvelope envelope) {
        LOGGER.info("Received submission at stagingcivil.submit-material");
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
                .withName("stagingcivil.command.submit-material")
                .withMetadataFrom(envelope));

        return envelopeFrom(
                envelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId, submissionId));
    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }

    @SuppressWarnings("squid:S1166")
    private void validateHearingDateRangeDetails(final HearingDateRangeDetails hearingDateRangeDetails) {
        if (hearingDateRangeDetails == null) {
            return;
        }

        final LocalDate startDateRangeOfHearing = hearingDateRangeDetails.getStartDateRangeOfHearing();
        final LocalDate endDateRangeOfHearing = hearingDateRangeDetails.getEndDateRangeOfHearing();

        if (startDateRangeOfHearing.isBefore(LocalDate.now())) {
            throw new BadRequestException(format(
                    "startDateRangeOfHearing %s must not be before today's date", startDateRangeOfHearing));
        }

        if (endDateRangeOfHearing.isBefore(startDateRangeOfHearing)) {
            throw new BadRequestException(format(
                    "endDateRangeOfHearing %s must not be before startDateRangeOfHearing %s",
                    endDateRangeOfHearing, startDateRangeOfHearing));
        }
    }

    private void validateOffences(final OtherCase otherCase) {
        final Map<String, List<String>> violations = new HashMap<>();

        otherCase.getProsecutionCases().forEach(prosecutionCase ->
                prosecutionCase.getDefendants().forEach(defendant ->
                        offenceValidator.validate(
                                defendant.getOffences().stream()
                                        .map(Offence::getOffenceDetails)
                                        .collect(toList()),
                                violations)));

        if (!violations.isEmpty()) {
            throwBadRequestException(violations);
        }
    }

    private void throwBadRequestException(final Map<String, List<String>> violations) {
        try {
            throw new BadRequestException(objectMapper.writeValueAsString(violations));
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to serialize violations json object", e);
            throw new BadRequestException("Business validations failed");
        }
    }

}
