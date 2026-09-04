package uk.gov.moj.cpp.staging.civil.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.REJECTED;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.SUCCESS_WITH_WARNINGS;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.entity.SubmissionType;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.OtherCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmissionEventListenerTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private SubmissionEventListener submissionEventListener;

    @Captor
    private ArgumentCaptor<Submission> argumentCaptor;

    @Test
    public void shouldOtherCase() {

        final UUID submissionId = randomUUID();
        final String prosecutingAuthority = "GAAAA01";
        final String urn = "urn_value";
        final OtherCaseReceived otherCaseReceived = OtherCaseReceived.otherCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority(prosecutingAuthority)
                .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase().withUrn(urn).build()))
                .build();

        final Envelope<OtherCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.other-case-received", otherCaseReceived);

        submissionEventListener.otherCaseReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.name()));
        assertThat(submission.getOuCode(), is(prosecutingAuthority));
        assertThat(submission.getCaseDetail().stream().findFirst().get().getCaseUrn(), is(urn));
    }

    @Test
    public void shouldSummons() {

        final UUID submissionId = randomUUID();
        final String prosecutingAuthority = "GAAAA01";
        final String urn = "urn_value";
        final SummonsReceived summonsReceived = SummonsReceived.summonsReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority(prosecutingAuthority)
                .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase().withUrn(urn).build()))
                .build();

        final Envelope<SummonsReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-received", summonsReceived);

        submissionEventListener.summonsReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.name()));
        assertThat(submission.getOuCode(), is(prosecutingAuthority));
        assertThat(submission.getCaseDetail().stream().findFirst().get().getCaseUrn(), is(urn));
    }

    @Test
    public void shouldUpdateCaseFile() {
        final UUID submissionId = randomUUID();
        final UpdateCivilCaseReceived summonsReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .build();

        Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING.name())
                .build();

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-received", summonsReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.name()));
    }

    @Test
    void shouldUpdateCaseFileForRejectedStatus() {
        final UUID submissionId = randomUUID();
        final UpdateCivilCaseReceived summonsProsecutionReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED)
                .withCaseErrors(Collections.EMPTY_LIST)
                .withGroupCaseErrors(Collections.EMPTY_LIST)
                .withDefendantErrors(Collections.EMPTY_LIST)
                .build();

        Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED.name())
                .build();

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", summonsProsecutionReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(REJECTED.name()));
    }

    @Test
    void shouldUpdateCaseFileForSuccessWithWarningsStatus() {
        final UUID submissionId = randomUUID();
        final UpdateCivilCaseReceived summonsProsecutionReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SUCCESS_WITH_WARNINGS)
                .withWarnings(Collections.EMPTY_LIST)
                .withCaseWarnings(Collections.EMPTY_LIST)
                .withDefendantWarnings(Collections.EMPTY_LIST)
                .build();

        Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SUCCESS_WITH_WARNINGS.name())
                .build();

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", summonsProsecutionReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(SUCCESS_WITH_WARNINGS.name()));
    }

    @Test
    void shouldIgnoreStalePendingUpdateWhenSubmissionAlreadyInTerminalStatus() {
        final UUID submissionId = randomUUID();
        final UpdateCivilCaseReceived stalePendingUpdate = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .build();

        Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SUCCESS.name())
                .build();

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.update-civil-case-received", stalePendingUpdate);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository, org.mockito.Mockito.never()).save(any());
        assertThat(inputSubmission.getSubmissionStatus(), is(SUCCESS.name()));
    }

    @Test
    public void shouldSubmitMaterial() {

        final UUID submissionId = randomUUID();
        final String prosecutingAuthority = "GAAAA01";
        final String caseUrn = "urn_value";
        final MaterialSubmitted materialSubmitted = MaterialSubmitted.materialSubmitted()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority(prosecutingAuthority)
                .withCaseUrn(caseUrn)
                .build();

        final Envelope<MaterialSubmitted> envelope = newEnvelope("stagingprosecutorscivil.event.material-submitted", materialSubmitted);

        submissionEventListener.materialSubmitted(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.name()));
        assertThat(submission.getOuCode(), is(prosecutingAuthority));
        assertThat(submission.getCaseDetail().stream().findFirst().get().getCaseUrn(), is(caseUrn));
        assertThat(submission.getType(), is(SubmissionType.MATERIAL));
    }

    @Test
    public void shouldMaterialSubmissionRejectedWithErrorsAndWarnings() {
        final UUID submissionId = randomUUID();
        final Problem error = Problem.problem().withCode("ERR001").build();
        final Problem warning = Problem.problem().withCode("WARN001").build();

        final MaterialSubmissionRejected materialSubmissionRejected = MaterialSubmissionRejected.materialSubmissionRejected()
                .withSubmissionId(submissionId)
                .withErrors(Collections.singletonList(error))
                .withWarnings(Collections.singletonList(warning))
                .build();

        final Submission existingSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING.name())
                .build();

        final Envelope<MaterialSubmissionRejected> envelope = newEnvelope("stagingprosecutorscivil.event.material-submission-rejected", materialSubmissionRejected);

        when(submissionRepository.findBy(submissionId)).thenReturn(existingSubmission);
        when(objectToJsonObjectConverter.convert(error)).thenReturn(Json.createObjectBuilder().add("code", "ERR001").build());
        when(objectToJsonObjectConverter.convert(warning)).thenReturn(Json.createObjectBuilder().add("code", "WARN001").build());

        submissionEventListener.materialSubmissionRejected(envelope);

        assertThat(existingSubmission.getSubmissionStatus(), is(REJECTED.toString()));
        assertThat(existingSubmission.getCompletedAt(), is(notNullValue()));
        assertThat(existingSubmission.getErrors(), is(notNullValue()));
        assertThat(existingSubmission.getWarnings(), is(notNullValue()));
    }

    @Test
    public void shouldMaterialSubmissionRejectedWithNullErrorsAndWarnings() {
        final UUID submissionId = randomUUID();

        final MaterialSubmissionRejected materialSubmissionRejected = MaterialSubmissionRejected.materialSubmissionRejected()
                .withSubmissionId(submissionId)
                .withErrors(null)
                .withWarnings(null)
                .build();

        final Submission existingSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING.name())
                .build();

        final Envelope<MaterialSubmissionRejected> envelope = newEnvelope("stagingprosecutorscivil.event.material-submission-rejected", materialSubmissionRejected);

        when(submissionRepository.findBy(submissionId)).thenReturn(existingSubmission);

        submissionEventListener.materialSubmissionRejected(envelope);

        assertThat(existingSubmission.getSubmissionStatus(), is(REJECTED.toString()));
        assertThat(existingSubmission.getCompletedAt(), is(notNullValue()));
        assertThat(existingSubmission.getErrors(), is(nullValue()));
        assertThat(existingSubmission.getWarnings(), is(nullValue()));
    }

    @Test
    public void shouldMaterialSubmissionSuccessfulReceived() {
        final UUID submissionId = randomUUID();

        final MaterialSubmissionSuccessful materialSubmissionSuccessful = MaterialSubmissionSuccessful.materialSubmissionSuccessful()
                .withSubmissionId(submissionId)
                .build();

        final Submission existingSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING.name())
                .build();

        final Envelope<MaterialSubmissionSuccessful> envelope = newEnvelope("stagingprosecutorscivil.event.material-submission-successful", materialSubmissionSuccessful);

        when(submissionRepository.findBy(submissionId)).thenReturn(existingSubmission);

        submissionEventListener.materialSubmissionSuccessfulReceived(envelope);

        assertThat(existingSubmission.getSubmissionStatus(), is(SUCCESS.toString()));
        assertThat(existingSubmission.getCompletedAt(), is(notNullValue()));
        verify(submissionRepository).save(existingSubmission);
    }

    private <T> Envelope<T> newEnvelope(final String name, T payload) {
        return envelopeFrom(metadataWithRandomUUID(name).createdAt(ZonedDateTime.now(UTC)), payload);
    }
}
