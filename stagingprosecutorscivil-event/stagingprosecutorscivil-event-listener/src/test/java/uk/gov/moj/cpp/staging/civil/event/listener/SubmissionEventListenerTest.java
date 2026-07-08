package uk.gov.moj.cpp.staging.civil.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.PENDING;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.OtherCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

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

    private <T> Envelope<T> newEnvelope(final String name, T payload) {
        return envelopeFrom(metadataWithRandomUUID(name).createdAt(ZonedDateTime.now(UTC)), payload);
    }
}
