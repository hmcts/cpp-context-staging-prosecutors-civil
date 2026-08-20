package uk.gov.justice.api.resource;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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
import uk.gov.moj.cpp.staging.civil.handler.command.api.csv.SummonsProsecutionCsvToJsonConverter;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecution;

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
import javax.ws.rs.core.Response;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
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

    @Captor
    private ArgumentCaptor<Envelope<SummonsProsecution>> envelopeCaptor;

    @Test
    void shouldConvertUploadedCsvAndDelegateToSummonsProsecution() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any())).thenReturn(Optional.empty());
        final UUID submissionId = randomUUID();
        final UrlResponse urlResponse = new UrlResponse("https://replace-me.gov.uk/v1/" + submissionId, submissionId);
        when(civilProsecutionApi.summonsProsecution(any()))
                .thenReturn(envelopeFrom(metadataBuilder().withId(randomUUID()).withName("x").build(), urlResponse));

        final Response response = resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        assertThat(response.getEntity(), is(urlResponse));

        verify(civilProsecutionApi).summonsProsecution(envelopeCaptor.capture());
        final SummonsProsecution submitted = envelopeCaptor.getValue().payload();
        assertThat(submitted.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(submitted.getProsecutionCases().size(), is(3));
    }

    @Test
    void shouldThrowBadRequestWhenCallingUserOrganisationDoesNotMatchCsvProsecutingAuthority() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        doThrow(new BadRequestException("prosecuting authority mismatch"))
                .when(prosecutingAuthorityValidationService).validateCallingUserBelongsToProsecutingAuthority(any(), any());

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));

        // CAD-1613: the audit call is ordered before this check, so a rejected upload still leaves
        // an audit record of the attempt.
        verify(auditService).audit(any(), eq(Component.COMMAND_API));
    }

    @Test
    void shouldThrowBadRequestWhenFilePartMissing() {
        when(multipartFormDataInput.getFormDataMap()).thenReturn(emptyMap());

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenFilePartsListIsEmpty() {
        final Map<String, List<InputPart>> formDataMap = new HashMap<>();
        formDataMap.put("file", List.of());
        when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenReadingFilePartThrowsIOException() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenThrow(new IOException("stream unavailable"));
        formDataMapWithFilePart();

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenCsvCannotBeParsed() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull()))
                .thenReturn(new ByteArrayInputStream("not,a,valid,complaints,csv".getBytes(StandardCharsets.UTF_8)));
        formDataMapWithFilePart();

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenUserIdHeaderMissing() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldThrowBadRequestWhenUserIdHeaderIsBlank() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn("   ");

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    @Test
    void shouldReturnForbiddenWhenAccessControlViolationPresent() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        when(headers.getHeaderString(HeaderConstants.USER_ID)).thenReturn(randomUUID().toString());
        when(accessControlService.checkAccessControl(any(), any()))
                .thenReturn(Optional.of(new AccessControlViolation("user not permitted")));

        final Response response = resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput);

        assertThat(response.getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()));
        assertThat(response.getEntity().toString(), org.hamcrest.Matchers.containsString("user not permitted"));
        assertThat(response.getEntity().toString(),
                org.hamcrest.Matchers.containsString("stagingprosecutorscivil.summons-prosecution"));
    }

    @Test
    void shouldThrowBadRequestWhenSchemaValidationFails() throws Exception {
        when(filePart.getBody(eq(InputStream.class), isNull())).thenReturn(csvStream(FULLY_POPULATED_CSV));
        formDataMapWithFilePart();
        doThrow(new JsonSchemaValidationException("Schema validation failed"))
                .when(jsonSchemaValidator).validate(any(), eq("stagingprosecutorscivil.summons-prosecution"));

        assertThrows(BadRequestException.class,
                () -> resource.postStagingprosecutorscivilSummonsProsecutionCsvComplaintsFiles(multipartFormDataInput));
    }

    private void formDataMapWithFilePart() {
        final Map<String, List<InputPart>> formDataMap = new HashMap<>();
        formDataMap.put("file", List.of(filePart));
        when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);
    }

    private InputStream csvStream(final String resourcePath) {
        return getClass().getResourceAsStream("/" + resourcePath);
    }
}
