package uk.gov.justice.api.resource;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolation;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.core.audit.AuditService;
import uk.gov.moj.cpp.staging.civil.handler.command.api.CivilProsecutionApi;
import uk.gov.moj.cpp.staging.civil.handler.command.api.ProsecutingAuthorityValidationService;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.ReferenceDataClient;
import uk.gov.moj.cpp.staging.civil.handler.command.api.client.UserGroupsClient;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.ComplaintsFileValidationError;
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvToJsonConverter;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.hamcrest.Matcher;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCommandApiComplaintsFilesResourceTest {

    private static final String FULLY_POPULATED_CSV = "summons-prosecution-fully-populated.csv";

    @InjectMocks
    private DefaultCommandApiComplaintsFilesResource resource;

    @Spy
    private final SummonsProsecutionCsvToJsonConverter csvToJsonConverter = new SummonsProsecutionCsvToJsonConverter();

    @Mock
    private CivilProsecutionApi civilProsecutionApi;

    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    @Mock
    private MultipartFormDataInput multipartFormDataInput;

    @Mock
    private InputPart filePart;

    @Mock
    private HttpHeaders headers;

    @Mock
    private AuditService auditService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private ProsecutingAuthorityValidationService prosecutingAuthorityValidationService;

    @Mock
    private UserGroupsClient userGroupsClient;

    @Mock
    private ReferenceDataClient referenceDataClient;

    @Captor
    private ArgumentCaptor<Envelope<Summons>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<String> fileNameCaptor;

    @Captor
    private ArgumentCaptor<List<ComplaintsFileValidationError>> errorsCaptor;

    @Captor
    private ArgumentCaptor<String> submittedByUserNameCaptor;

    @Captor
    private ArgumentCaptor<String> prosecutorShortNameCaptor;

    @Test
    void shouldConvertUploadedCsvAndDelegateToSummonsProsecution() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart("form-data; name=\"file\"; filename=\"summons-batch.csv\"");
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any())).thenReturn(Optional.empty());
        when(referenceDataClient.getProsecutorShortNameForOuCode("GAAAA01")).thenReturn("TVL");
        when(userGroupsClient.getDisplayNameForUser(any())).thenReturn(Optional.of("Richard Chapman"));
        final UUID submissionId = randomUUID();
        final UrlResponse urlResponse = new UrlResponse("https://replace-me.gov.uk/v1/" + submissionId, submissionId);
        when(civilProsecutionApi.summonsProsecution(any(), any(), any(), any()))
                .thenReturn(envelopeFrom(metadataBuilder().withId(randomUUID()).withName("x").build(), urlResponse));

        final Response response = resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        assertThat(response.getEntity(), is(urlResponse));

        verify(civilProsecutionApi).summonsProsecution(envelopeCaptor.capture(),
                fileNameCaptor.capture(), submittedByUserNameCaptor.capture(), prosecutorShortNameCaptor.capture());
        final Summons submitted = envelopeCaptor.getValue().payload();
        assertThat(submitted.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(submitted.getProsecutionCases().size(), is(3));
        assertThat(fileNameCaptor.getValue(), is("summons-batch.csv"));
        assertThat(submittedByUserNameCaptor.getValue(), is("Richard Chapman"));
        assertThat(prosecutorShortNameCaptor.getValue(), is("TVL"));

        // Exactly one reference-data call per request, reused for both the prosecuting-authority
        // check and Submission persistence - not resolved twice.
        verify(referenceDataClient, times(1)).getProsecutorShortNameForOuCode("GAAAA01");
    }

    @Test
    void shouldCaptureNullFileNameWhenContentDispositionHasNoFilename() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart("form-data; name=\"file\"");
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any())).thenReturn(Optional.empty());
        when(referenceDataClient.getProsecutorShortNameForOuCode("GAAAA01")).thenReturn("TVL");
        when(userGroupsClient.getDisplayNameForUser(any())).thenReturn(Optional.of("Richard Chapman"));
        final UUID submissionId = randomUUID();
        final UrlResponse urlResponse = new UrlResponse("https://replace-me.gov.uk/v1/" + submissionId, submissionId);
        when(civilProsecutionApi.summonsProsecution(any(), any(), any(), any()))
                .thenReturn(envelopeFrom(metadataBuilder().withId(randomUUID()).withName("x").build(), urlResponse));

        final Response response = resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));

        verify(civilProsecutionApi).summonsProsecution(any(),
                fileNameCaptor.capture(), any(), any());
        assertThat(fileNameCaptor.getValue(), is(nullValue()));
    }

    @Test
    void shouldCaptureNullSubmittedByUserNameWhenUserDetailsNotFound() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart("form-data; name=\"file\"; filename=\"summons-batch.csv\"");
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any())).thenReturn(Optional.empty());
        when(referenceDataClient.getProsecutorShortNameForOuCode("GAAAA01")).thenReturn("TVL");
        when(userGroupsClient.getDisplayNameForUser(any())).thenReturn(Optional.empty());
        final UUID submissionId = randomUUID();
        final UrlResponse urlResponse = new UrlResponse("https://replace-me.gov.uk/v1/" + submissionId, submissionId);
        when(civilProsecutionApi.summonsProsecution(any(), any(), any(), any()))
                .thenReturn(envelopeFrom(metadataBuilder().withId(randomUUID()).withName("x").build(), urlResponse));

        resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        verify(civilProsecutionApi).summonsProsecution(any(),
                any(), submittedByUserNameCaptor.capture(), any());
        assertThat(submittedByUserNameCaptor.getValue(), is(nullValue()));
    }

    @Test
    void shouldResolveProsecutorShortNameEvenWhenCallingUserIsExempt() throws Exception {
        // The exempt-group path in ProsecutingAuthorityValidationService returns without ever
        // resolving a short name itself - the resource always resolves it upfront, exactly once,
        // regardless of exempt status, so a value is still captured for storage.
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart("form-data; name=\"file\"; filename=\"summons-batch.csv\"");
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any())).thenReturn(Optional.empty());
        when(referenceDataClient.getProsecutorShortNameForOuCode("GAAAA01")).thenReturn("CPS");
        when(userGroupsClient.getDisplayNameForUser(any())).thenReturn(Optional.of("Richard Chapman"));
        final UUID submissionId = randomUUID();
        final UrlResponse urlResponse = new UrlResponse("https://replace-me.gov.uk/v1/" + submissionId, submissionId);
        when(civilProsecutionApi.summonsProsecution(any(), any(), any(), any()))
                .thenReturn(envelopeFrom(metadataBuilder().withId(randomUUID()).withName("x").build(), urlResponse));

        resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        verify(civilProsecutionApi).summonsProsecution(any(),
                any(), any(), prosecutorShortNameCaptor.capture());
        assertThat(prosecutorShortNameCaptor.getValue(), is("CPS"));
        verify(referenceDataClient, times(1)).getProsecutorShortNameForOuCode("GAAAA01");
    }

    @Test
    void shouldThrowBadRequestWhenCallingUserOrganisationDoesNotMatchCsvProsecutingAuthority() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        doThrow(new BadRequestException("prosecuting authority mismatch"))
                .when(prosecutingAuthorityValidationService).validateCallingUserBelongsToProsecutingAuthority(any(), any(), any());

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));

        // CAD-1613: the audit call is ordered before this check, so a rejected upload still leaves
        // an audit record of the attempt.
        verify(auditService).audit(any(), eq(Component.COMMAND_API));
    }

    @Test
    void shouldThrowBadRequestWhenFilePartMissing() {
        when(multipartFormDataInput.getFormDataMap()).thenReturn(emptyMap());

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenFilePartsListIsEmpty() {
        final Map<String, List<InputPart>> formDataMap = new HashMap<>();
        formDataMap.put("file", List.of());
        when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenReadingFilePartThrowsIOException() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenThrow(new IOException("stream unavailable"));
        formDataMapWithFilePart();

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenCsvCannotBeParsed() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream("not,a,valid,complaints,csv".getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart();

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldAcceptUploadAndRecordFailedSubmissionWhenCsvContainsNonIsoDates() throws Exception {
        // every 2022-03-10 becomes 10/03/2022: every row's hearing date and row 3's laid date
        final String csv = fixtureCsv().replace("2022-03-10", "10/03/2022");
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart("form-data; name=\"file\"; filename=\"summons-batch.csv\"");
        final UrlResponse urlResponse = stubAllowedUploadAndRejection();

        final Response response = resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        assertThat(response.getEntity(), is(urlResponse));
        verify(civilProsecutionApi).rejectComplaintsFile(any(), eq("GAAAA01"), eq("summons-batch.csv"),
                eq("Richard Chapman"), eq("TVL"), errorsCaptor.capture());
        assertThat(errorsCaptor.getValue(), contains(
                validationError(1L, "SCIV11111", "INVALID_DATE_FORMAT", "hearingDetails.dateOfHearing", "10/03/2022"),
                validationError(2L, "SCIV22222", "INVALID_DATE_FORMAT", "hearingDetails.dateOfHearing", "10/03/2022"),
                validationError(3L, "SCIV33333", "INVALID_DATE_FORMAT", "hearingDetails.dateOfHearing", "10/03/2022"),
                validationError(3L, "SCIV33333", "INVALID_DATE_FORMAT", "offence.offenceDetails.laidDate", "10/03/2022")));
        verify(civilProsecutionApi, never()).summonsProsecution(any(), any(), any(), any());
        // rows 1 and 2 still convert, so they are schema-validated in the same upload
        verify(jsonSchemaValidator).validate(any(), eq("stagingcivil.summons"));
    }

    @Test
    void shouldReportConversionAndSchemaErrorsTogetherOrderedByRow() throws Exception {
        // row 2 cannot be converted; rows 1 and 3 convert but fail JSON-schema validation (row 3)
        final String csv = fixtureCsv().replace("2012-08-14", "14/08/2012");
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart("form-data; name=\"file\"; filename=\"summons-batch.csv\"");
        stubAllowedUploadAndRejection();
        // a genuine everit violation at #/prosecutionCases/1/urn - index 1 of the converted cases is row 3
        final JSONObject urnMaxLengthSchema = new JSONObject()
                .put("properties", new JSONObject()
                        .put("prosecutionCases", new JSONObject()
                                .put("items", new JSONObject()
                                        .put("properties", new JSONObject()
                                                .put("urn", new JSONObject().put("maxLength", 4))))));
        final JSONObject twoCases = new JSONObject()
                .put("prosecutionCases", new JSONArray()
                        .put(new JSONObject().put("urn", "ok"))
                        .put(new JSONObject().put("urn", "SCIV33333")));
        final ValidationException violation = assertThrows(ValidationException.class,
                () -> SchemaLoader.load(urnMaxLengthSchema).validate(twoCases));
        doThrow(new JsonSchemaValidationException("schema violations", violation))
                .when(jsonSchemaValidator).validate(any(), eq("stagingcivil.summons"));

        resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        verify(civilProsecutionApi).rejectComplaintsFile(any(), any(), any(), any(), any(), errorsCaptor.capture());
        assertThat(errorsCaptor.getValue(), contains(
                validationError(2L, "SCIV22222", "INVALID_DATE_FORMAT", "defendant.individual.dateOfBirth", "14/08/2012"),
                validationError(3L, "SCIV33333", "INVALID_VALUE", "case.urn", "SCIV33333")));
        verify(civilProsecutionApi, never()).summonsProsecution(any(), any(), any(), any());
    }

    @Test
    void shouldReportHearingDetailsErrorsTogetherWithSchemaErrorsFromOtherRowsWithoutDuplicates() throws Exception {
        // only row 1's hearing date is broken - rows 2 and 3's valid dates are not compared with it
        final String csv = fixtureCsv().replaceFirst("2022-03-10", "10/03/2022");
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart("form-data; name=\"file\"; filename=\"summons-batch.csv\"");
        stubAllowedUploadAndRejection();
        // genuine everit violations: the unreadable (so blank) hearing date, and row 3's urn
        final JSONObject schema = new JSONObject()
                .put("properties", new JSONObject()
                        .put("hearingDetails", new JSONObject()
                                .put("required", new JSONArray().put("dateOfHearing")))
                        .put("prosecutionCases", new JSONObject()
                                .put("items", new JSONObject()
                                        .put("properties", new JSONObject()
                                                .put("urn", new JSONObject().put("pattern", "^SCIV[12]"))))));
        final JSONObject submission = new JSONObject()
                .put("hearingDetails", new JSONObject().put("courtHearingLocation", "B01LY01"))
                .put("prosecutionCases", new JSONArray()
                        .put(new JSONObject().put("urn", "SCIV11111"))
                        .put(new JSONObject().put("urn", "SCIV22222"))
                        .put(new JSONObject().put("urn", "SCIV33333")));
        final ValidationException violations = assertThrows(ValidationException.class,
                () -> SchemaLoader.load(schema).validate(submission));
        doThrow(new JsonSchemaValidationException("schema violations", violations))
                .when(jsonSchemaValidator).validate(any(), eq("stagingcivil.summons"));

        resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        verify(civilProsecutionApi).rejectComplaintsFile(any(), any(), any(), any(), any(), errorsCaptor.capture());
        assertThat(errorsCaptor.getValue(), contains(
                validationError(1L, "SCIV11111", "INVALID_DATE_FORMAT", "hearingDetails.dateOfHearing", "10/03/2022"),
                validationError(3L, "SCIV33333", "INVALID_VALUE", "case.urn", "SCIV33333")));
        verify(civilProsecutionApi, never()).summonsProsecution(any(), any(), any(), any());
    }

    @Test
    void shouldNotRecordAFailedSubmissionWhenCallingUserLacksPermissionEvenIfCsvHasErrors() throws Exception {
        final String csv = fixtureCsv().replace("2012-08-14", "14/08/2012");
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any()))
                .thenReturn(Optional.of(new AccessControlViolation("user not permitted")));

        final Response response = resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()));
        verify(civilProsecutionApi, never()).rejectComplaintsFile(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldNotRecordAFailedSubmissionWhenCallingUserBelongsToAnotherProsecutorEvenIfCsvHasErrors() throws Exception {
        final String csv = fixtureCsv().replace("2012-08-14", "14/08/2012");
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        doThrow(new BadRequestException("prosecuting authority mismatch"))
                .when(prosecutingAuthorityValidationService).validateCallingUserBelongsToProsecutingAuthority(any(), any(), any());

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));

        verify(civilProsecutionApi, never()).rejectComplaintsFile(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldThrowBadRequestWhenProsecutingAuthorityIsBlankBecauseTheFileCannotBeAttributed() throws Exception {
        final String csv = fixtureCsv().replace("GAAAA01", "");
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart();

        final BadRequestException exception = assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));

        assertThat(exception.getMessage(), is("Unable to parse complaints CSV file: Row 1: required column 'prosecutingAuthority' is blank"));
        verify(civilProsecutionApi, never()).rejectComplaintsFile(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldThrowBadRequestWhenUserIdHeaderMissing() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenUserIdHeaderIsBlank() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn("   ");

        assertThrows(BadRequestException.class,
                () -> resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldReturnForbiddenWhenAccessControlViolationPresent() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any()))
                .thenReturn(Optional.of(new AccessControlViolation("user not permitted")));

        final Response response = resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()));
        assertThat(response.getEntity().toString(), org.hamcrest.Matchers.containsString("user not permitted"));
        assertThat(response.getEntity().toString(),
                org.hamcrest.Matchers.containsString("stagingcivil.summons-prosecution-csv"));
    }

    @Test
    void shouldAcceptUploadAndRecordFailedSubmissionWhenSchemaValidationFails() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart("form-data; name=\"file\"; filename=\"summons-batch.csv\"");
        final UrlResponse urlResponse = stubAllowedUploadAndRejection();
        doThrow(new JsonSchemaValidationException("Schema validation failed"))
                .when(jsonSchemaValidator).validate(any(), eq("stagingcivil.summons"));

        final Response response = resource.postStagingcivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        assertThat(response.getEntity(), is(urlResponse));
        verify(civilProsecutionApi).rejectComplaintsFile(any(), eq("GAAAA01"), eq("summons-batch.csv"),
                eq("Richard Chapman"), eq("TVL"), errorsCaptor.capture());
        assertThat(errorsCaptor.getValue().size(), is(1));
        verify(civilProsecutionApi, never()).summonsProsecution(any(), any(), any(), any());
        verify(auditService).audit(any(), eq(Component.COMMAND_API));
    }

    private UrlResponse stubAllowedUploadAndRejection() {
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any())).thenReturn(Optional.empty());
        when(referenceDataClient.getProsecutorShortNameForOuCode("GAAAA01")).thenReturn("TVL");
        when(userGroupsClient.getDisplayNameForUser(any())).thenReturn(Optional.of("Richard Chapman"));
        final UUID submissionId = randomUUID();
        final UrlResponse urlResponse = new UrlResponse("https://replace-me.gov.uk/v1/" + submissionId, submissionId);
        when(civilProsecutionApi.rejectComplaintsFile(any(), any(), any(), any(), any(), any()))
                .thenReturn(envelopeFrom(metadataBuilder().withId(randomUUID()).withName("x").build(), urlResponse));
        return urlResponse;
    }

    private String fixtureCsv() throws IOException {
        return new String(csvStream(FULLY_POPULATED_CSV).readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Matcher<ComplaintsFileValidationError> validationError(final long rowNumber, final String caseReference,
                                                                          final String code, final String field,
                                                                          final String value) {
        return allOf(
                hasProperty("rowNumber", is(rowNumber)),
                hasProperty("caseReference", is(caseReference)),
                hasProperty("code", is(code)),
                hasProperty("field", is(field)),
                hasProperty("value", is(value)));
    }

    private void formDataMapWithFilePart() {
        final Map<String, List<InputPart>> formDataMap = new HashMap<>();
        formDataMap.put("file", List.of(filePart));
        when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);
    }

    private void formDataMapWithFilePart(final String contentDisposition) {
        formDataMapWithFilePart();
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Disposition", contentDisposition);
        when(filePart.getHeaders()).thenReturn(headers);
    }

    private InputStream csvStream(final String resourcePath) {
        return getClass().getResourceAsStream("/" + resourcePath);
    }
}
