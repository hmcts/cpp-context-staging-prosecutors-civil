package uk.gov.moj.cpp.staging.civil.query.api;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.civil.query.CivilProsecutionQueryView;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilProsecutionQueryApiTest {

    private static final String USER_ID = randomUUID().toString();

    @Mock
    private CivilProsecutionQueryView civilProsecutionQueryView;

    @Mock
    private ProsecutingAuthorityValidationService prosecutingAuthorityValidationService;

    @Mock
    private Submission submission;

    @InjectMocks
    private CivilProsecutionQueryApi civilProsecutionQueryApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Optional<Submission>> submissionOptionalArgumentCaptor;

    @Test
    public void shouldPassSubmissionToQueryViewWhenCallerBelongsToProsecutingAuthority() {

        final UUID submissionId = randomUUID();
        when(submission.getOuCode()).thenReturn("GAAAA01");
        when(submission.getProsecutorShortName()).thenReturn("DVLA");
        when(civilProsecutionQueryView.findSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(prosecutingAuthorityValidationService.callerBelongsToProsecutingAuthority(USER_ID, "GAAAA01", "DVLA"))
                .thenReturn(true);

        civilProsecutionQueryApi.getSubmissionDetails(envelopeWithSubmissionId(
                "stagingprosecutorscivil.submission-details", submissionId, USER_ID));

        verify(civilProsecutionQueryView).buildSubmissionDetailsResponse(jsonEnvelopeArgumentCaptor.capture(),
                submissionOptionalArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), is("stagingprosecutorscivil.query.submission-details"));
        assertThat(submissionOptionalArgumentCaptor.getValue(), is(Optional.of(submission)));
    }

    @Test
    public void shouldThrowBadRequestWhenCallerDoesNotBelongToProsecutingAuthority() {

        final UUID submissionId = randomUUID();
        when(submission.getOuCode()).thenReturn("GAAAA01");
        when(submission.getProsecutorShortName()).thenReturn("DVLA");
        when(civilProsecutionQueryView.findSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(prosecutingAuthorityValidationService.callerBelongsToProsecutingAuthority(USER_ID, "GAAAA01", "DVLA"))
                .thenReturn(false);

        final JsonEnvelope envelope = envelopeWithSubmissionId(
                "stagingprosecutorscivil.submission-details", submissionId, USER_ID);

        assertThrows(BadRequestException.class, () -> civilProsecutionQueryApi.getSubmissionDetails(envelope));
        verify(civilProsecutionQueryView, never()).buildSubmissionDetailsResponse(any(), any());
    }

    @Test
    public void shouldPassEmptyOptionalToQueryViewWhenSubmissionNotFound() {

        final UUID submissionId = randomUUID();
        when(civilProsecutionQueryView.findSubmission(submissionId)).thenReturn(Optional.empty());

        civilProsecutionQueryApi.getSubmissionDetails(envelopeWithSubmissionId(
                "stagingprosecutorscivil.submission-details", submissionId, USER_ID));

        verify(civilProsecutionQueryView).buildSubmissionDetailsResponse(any(), submissionOptionalArgumentCaptor.capture());
        assertThat(submissionOptionalArgumentCaptor.getValue(), is(Optional.empty()));
        verify(prosecutingAuthorityValidationService, never()).callerBelongsToProsecutingAuthority(any(), any(), any());
    }

    @Test
    public void shouldReturnSubmissionErrorDetailsCsvWhenCallerBelongsToProsecutingAuthority() {

        final UUID submissionId = randomUUID();
        when(submission.getOuCode()).thenReturn("GAAAA01");
        when(submission.getProsecutorShortName()).thenReturn("DVLA");
        when(civilProsecutionQueryView.findSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(prosecutingAuthorityValidationService.callerBelongsToProsecutingAuthority(USER_ID, "GAAAA01", "DVLA"))
                .thenReturn(true);

        civilProsecutionQueryApi.getSubmissionErrorDetailsCsv(envelopeWithSubmissionId(
                "stagingprosecutorscivil.submission-error-details", submissionId, USER_ID));

        verify(civilProsecutionQueryView).buildSubmissionErrorDetailsCsvResponse(jsonEnvelopeArgumentCaptor.capture(),
                submissionOptionalArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), is("stagingprosecutorscivil.query.submission-error-details-csv"));
        assertThat(submissionOptionalArgumentCaptor.getValue(), is(Optional.of(submission)));
    }

    @Test
    public void shouldThrowBadRequestForCsvWhenCallerDoesNotBelongToProsecutingAuthority() {

        final UUID submissionId = randomUUID();
        when(submission.getOuCode()).thenReturn("GAAAA01");
        when(submission.getProsecutorShortName()).thenReturn("DVLA");
        when(civilProsecutionQueryView.findSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(prosecutingAuthorityValidationService.callerBelongsToProsecutingAuthority(USER_ID, "GAAAA01", "DVLA"))
                .thenReturn(false);

        final JsonEnvelope envelope = envelopeWithSubmissionId(
                "stagingprosecutorscivil.submission-error-details", submissionId, USER_ID);

        assertThrows(BadRequestException.class, () -> civilProsecutionQueryApi.getSubmissionErrorDetailsCsv(envelope));
        verify(civilProsecutionQueryView, never()).buildSubmissionErrorDetailsCsvResponse(any(), any());
    }

    @Test
    public void shouldRejectRequestWhenUserIdMissingFromMetadata() {

        final UUID submissionId = randomUUID();
        final JsonEnvelope envelopeWithoutUserId = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("stagingprosecutorscivil.submission-details").build(),
                requestPayload(submissionId));

        assertThrows(BadRequestException.class, () -> civilProsecutionQueryApi.getSubmissionDetails(envelopeWithoutUserId));
    }

    @Test
    public void shouldRejectSubmissionIdThatIsNotAValidUuid() {

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("stagingprosecutorscivil.submission-details")
                        .withUserId(USER_ID).build(),
                createObjectBuilder().add("submissionId", "not-a-uuid").build());

        assertThrows(BadRequestException.class, () -> civilProsecutionQueryApi.getSubmissionDetails(envelope));
    }

    private JsonObject requestPayload(final UUID submissionId) {
        return createObjectBuilder().add("submissionId", submissionId.toString()).build();
    }

    private JsonEnvelope envelopeWithSubmissionId(final String name, final UUID submissionId, final String userId) {
        final Metadata metadata = metadataBuilder().withId(randomUUID()).withName(name).withUserId(userId).build();
        return envelopeFrom(metadata, requestPayload(submissionId));
    }
}
