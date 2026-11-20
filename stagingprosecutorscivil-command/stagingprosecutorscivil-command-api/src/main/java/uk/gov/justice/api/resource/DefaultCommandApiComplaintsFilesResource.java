package uk.gov.justice.api.resource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.civil.handler.command.api.CivilProsecutionApi;
import uk.gov.moj.cpp.staging.civil.handler.command.api.ProsecutingAuthorityValidationService;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.ReferenceDataClient;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.UserGroupsClient;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileConversionResult;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvToJsonConverter;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsSchemaViolationMapper;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;

import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolation;
import uk.gov.justice.services.core.audit.AuditService;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hand-written in place of the framework-generated default: the generated default routes {@code
 * type: file} multipart parts through {@code RestProcessor}/{@code InterceptorChainProcessor},
 * which stores the raw bytes via the framework File Service (FileStorer) and substitutes a
 * reference id before any handler sees the payload. This resource instead reads the uploaded CSV
 * part's bytes directly and never touches the File Service.
 */
@Adapter(Component.COMMAND_API)
public class DefaultCommandApiComplaintsFilesResource implements CommandApiComplaintsFilesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCommandApiComplaintsFilesResource.class);

    private static final String FILE_PART_NAME = "file";
    private static final String SUMMONS_PROSECUTION_SCHEMA_NAME = "stagingcivil.summons";
    private static final String SUMMONS_PROSECUTION_CSV_ACTION_NAME = "stagingcivil.summons-prosecution-csv";
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"?([^\";]+)\"?");

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Inject
    private SummonsProsecutionCsvToJsonConverter csvToJsonConverter;

    @Inject
    private CivilProsecutionApi civilProsecutionApi;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;

    @Inject
    private AuditService auditService;

    @Inject
    private AccessControlService accessControlService;

    @Inject
    private ProsecutingAuthorityValidationService prosecutingAuthorityValidationService;

    @Inject
    private UserGroupsClient userGroupsClient;

    @Inject
    private ReferenceDataClient referenceDataClient;

    @Context
    private HttpHeaders headers;

    /**
     * Accepts a complaints CSV upload.
     * <p>
     * Problems that mean the file cannot be read or attributed to the caller's prosecutor
     * organisation - no file part, unreadable or empty CSV, too many rows, a first row that does not
     * match the template, a blank or foreign {@code prosecutingAuthority}, missing permission - are
     * still rejected synchronously (400/403) because no submission can be tracked for them.
     * <p>
     * Every other schema-level problem (blank required columns, malformed values, JSON-schema
     * violations) is collected for the whole file. If there are any, the upload is accepted (202 with
     * a {@code submissionId}) and recorded as a {@code FAILED} submission whose error report lists
     * them; otherwise the converted submission continues through the normal summons flow.
     */
    @Override
    public Response postStagingcivilSummonsProsecutionCsvComplaintsFiles(
            final MultipartFormDataInput multipartFormDataInput) {

        final ComplaintsFileConversionResult conversionResult = convertCsv(multipartFormDataInput);
        final String userId = requireUserIdHeader();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(SUMMONS_PROSECUTION_SCHEMA_NAME)
                .withUserId(userId)
                .build();

        final JsonObject payload = conversionResult.getSummons()
                .map(this::summonsProsecutionAsJsonObject)
                .orElseGet(() -> Json.createObjectBuilder()
                        .add("prosecutingAuthority", conversionResult.getProsecutingAuthority())
                        .build());
        final JsonEnvelope commandEnvelope = JsonEnvelope.envelopeFrom(metadata, payload);

        auditService.audit(commandEnvelope, Component.COMMAND_API);

        final String csvOuCode = conversionResult.getProsecutingAuthority();
        final String prosecutorShortName = referenceDataClient.getProsecutorShortNameForOuCode(csvOuCode);

        prosecutingAuthorityValidationService.validateCallingUserBelongsToProsecutingAuthority(userId, csvOuCode, prosecutorShortName);

        final Metadata accessControlMetadata = metadataBuilder()
                .withId(randomUUID())
                .withName(SUMMONS_PROSECUTION_CSV_ACTION_NAME)
                .withUserId(userId)
                .build();
        final JsonEnvelope accessControlEnvelope = JsonEnvelope.envelopeFrom(accessControlMetadata, payload);

        final Optional<AccessControlViolation> violation =
                accessControlService.checkAccessControl(Component.COMMAND_API, accessControlEnvelope);
        if (violation.isPresent()) {
            final JsonObject responseErrorMsg = Json.createObjectBuilder()
                    .add("error", errorMessageFrom(accessControlEnvelope, violation.get()))
                    .build();
            return Response.status(Response.Status.FORBIDDEN).entity(responseErrorMsg.toString()).build();
        }

        final String fileName = extractFileName(multipartFormDataInput);
        final String submittedByUserName = userGroupsClient.getDisplayNameForUser(userId).orElse(null);

        final List<ComplaintsFileValidationError> validationErrors = collectValidationErrors(conversionResult);
        if (!validationErrors.isEmpty()) {
            final Envelope<UrlResponse> rejected = civilProsecutionApi.rejectComplaintsFile(
                    metadata, csvOuCode, fileName, submittedByUserName, prosecutorShortName, validationErrors);
            return Response.status(Response.Status.ACCEPTED).entity(rejected.payload()).build();
        }

        final Summons summonsProsecution = conversionResult.getSummons()
                .orElseThrow(() -> new BadRequestException("Unable to parse complaints CSV file: CSV file contains no data rows"));
        final Envelope<UrlResponse> result =
                civilProsecutionApi.summonsProsecution(
                        envelopeFrom(metadata, summonsProsecution), fileName, submittedByUserName, prosecutorShortName);

        return Response.status(Response.Status.ACCEPTED).entity(result.payload()).build();
    }

    /**
     * Row-level conversion errors plus, for the rows that did convert, every JSON-schema violation.
     * A schema violation on a value the conversion already reported (e.g. a hearing date that could
     * not be read, so was left blank) is dropped, so each problem appears once in the error report.
     */
    private List<ComplaintsFileValidationError> collectValidationErrors(final ComplaintsFileConversionResult conversionResult) {
        final List<ComplaintsFileValidationError> errors = new ArrayList<>(conversionResult.getErrors());
        final Set<String> alreadyReported = errors.stream()
                .map(DefaultCommandApiComplaintsFilesResource::rowAndField)
                .collect(toSet());
        conversionResult.getSummons()
                .map(summons -> schemaViolationsOf(summons, conversionResult))
                .ifPresent(violations -> violations.stream()
                        .filter(violation -> !alreadyReported.contains(rowAndField(violation)))
                        .forEach(errors::add));
        errors.sort(Comparator.comparingLong(ComplaintsFileValidationError::getRowNumber));
        return errors;
    }

    private static String rowAndField(final ComplaintsFileValidationError error) {
        return error.getRowNumber() + ":" + error.getField();
    }

    private List<ComplaintsFileValidationError> schemaViolationsOf(final Summons summonsProsecution,
                                                                   final ComplaintsFileConversionResult conversionResult) {
        final String json;
        try {
            json = objectMapper.writeValueAsString(summonsProsecution);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Unable to serialise converted complaints submission", e);
        }
        try {
            jsonSchemaValidator.validate(json, SUMMONS_PROSECUTION_SCHEMA_NAME);
            return List.of();
        } catch (JsonSchemaValidationException e) {
            LOGGER.info("Complaints CSV file failed schema validation: {}", e.getMessage());
            return SummonsSchemaViolationMapper.map(e, conversionResult.getConvertedRows(), json);
        }
    }

    private String extractFileName(final MultipartFormDataInput multipartFormDataInput) {
        final List<InputPart> fileParts = multipartFormDataInput.getFormDataMap().get(FILE_PART_NAME);
        if (fileParts == null || fileParts.isEmpty()) {
            return null;
        }

        final String contentDisposition = fileParts.get(0).getHeaders().getFirst(CONTENT_DISPOSITION_HEADER);
        if (contentDisposition == null) {
            return null;
        }

        final Matcher matcher = FILENAME_PATTERN.matcher(contentDisposition);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String requireUserIdHeader() {
        final String userId = headers.getHeaderString(HeaderConstants.USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("Missing " + HeaderConstants.USER_ID + " header");
        }
        return userId;
    }

    private ComplaintsFileConversionResult convertCsv(final MultipartFormDataInput multipartFormDataInput) {
        final Map<String, List<InputPart>> formParts = multipartFormDataInput.getFormDataMap();
        final List<InputPart> fileParts = formParts.get(FILE_PART_NAME);

        if (fileParts == null || fileParts.isEmpty()) {
            throw new BadRequestException("Missing required multipart form field '" + FILE_PART_NAME + "'");
        }

        try (InputStream csvStream = fileParts.get(0).getBody(InputStream.class, null);
             Reader csvReader = new InputStreamReader(csvStream, UTF_8)) {
            return csvToJsonConverter.convert(csvReader);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.warn("Failed to parse complaints CSV file", e);
            throw new BadRequestException("Unable to parse complaints CSV file: " + e.getMessage(), e);
        }
    }

    private JsonObject summonsProsecutionAsJsonObject(final Summons summonsProsecution) {
        try {
            final String summonsProsecutionJson = objectMapper.writeValueAsString(summonsProsecution);

            try (final JsonReader jsonReader = Json.createReader(new StringReader(summonsProsecutionJson))) {
                return jsonReader.readObject();
            }
        } catch (final JsonProcessingException exception) {
            throw new BadRequestException("Unable to serialise converted complaints submission", exception);
        }
    }

    private String errorMessageFrom(final JsonEnvelope jsonEnvelope, final AccessControlViolation accessControlViolation) {
        return "Access Control failed for json envelope '" + jsonEnvelope.metadata().name()
                + "'. Reason: " + accessControlViolation.getReason();
    }
}
