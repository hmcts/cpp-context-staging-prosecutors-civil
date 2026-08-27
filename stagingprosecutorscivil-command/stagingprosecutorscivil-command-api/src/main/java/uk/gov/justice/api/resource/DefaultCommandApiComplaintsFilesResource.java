package uk.gov.justice.api.resource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
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
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvToJsonConverter;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecution;

import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolation;
import uk.gov.justice.services.core.audit.AuditService;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final String SUMMONS_PROSECUTION_SCHEMA_NAME = "stagingprosecutorscivil.summons-prosecution";
    private static final String SUMMONS_PROSECUTION_CSV_ACTION_NAME = "stagingprosecutorscivil.summons-prosecution-csv";
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

    @Override
    public Response postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(
            final MultipartFormDataInput multipartFormDataInput) {

        final SummonsProsecution summonsProsecution = convertCsvToSummonsProsecution(multipartFormDataInput);
        validateAgainstSummonsProsecutionSchema(summonsProsecution);

        final String userId = requireUserIdHeader();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(SUMMONS_PROSECUTION_SCHEMA_NAME)
                .withUserId(userId)
                .build();

        final JsonObject payload = summonsProsecutionAsJsonObject(summonsProsecution);
        final JsonEnvelope commandEnvelope = JsonEnvelope.envelopeFrom(metadata, payload);

        auditService.audit(commandEnvelope, Component.COMMAND_API);

        final String csvOuCode = summonsProsecution.getProsecutingAuthority();
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

        final Envelope<UrlResponse> result =
                civilProsecutionApi.summonsProsecution(
                        envelopeFrom(metadata, summonsProsecution), fileName, submittedByUserName, prosecutorShortName);

        return Response.status(Response.Status.ACCEPTED).entity(result.payload()).build();
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

    private SummonsProsecution convertCsvToSummonsProsecution(final MultipartFormDataInput multipartFormDataInput) {
        final Map<String, List<InputPart>> formParts = multipartFormDataInput.getFormDataMap();
        final List<InputPart> fileParts = formParts.get(FILE_PART_NAME);

        if (fileParts == null || fileParts.isEmpty()) {
            throw new BadRequestException("Missing required multipart form field '" + FILE_PART_NAME + "'");
        }

        try (InputStream csvStream = fileParts.get(0).getBody(InputStream.class, null);
             Reader csvReader = new InputStreamReader(csvStream, UTF_8)) {
            return csvToJsonConverter.convertToObject(csvReader);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.warn("Failed to parse complaints CSV file", e);
            throw new BadRequestException("Unable to parse complaints CSV file: " + e.getMessage(), e);
        }
    }

    private void validateAgainstSummonsProsecutionSchema(final SummonsProsecution summonsProsecution) {
        try {
            final String json = objectMapper.writeValueAsString(summonsProsecution);
            jsonSchemaValidator.validate(json, SUMMONS_PROSECUTION_SCHEMA_NAME);
        } catch (JsonSchemaValidationException e) {
            throw new BadRequestException("Complaints CSV file failed schema validation", e);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Unable to serialise converted complaints submission", e);
        }
    }

    private JsonObject summonsProsecutionAsJsonObject(final SummonsProsecution summonsProsecution) {
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
