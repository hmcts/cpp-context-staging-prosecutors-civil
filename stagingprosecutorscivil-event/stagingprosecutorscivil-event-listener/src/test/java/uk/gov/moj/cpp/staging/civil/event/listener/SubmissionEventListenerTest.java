package uk.gov.moj.cpp.staging.civil.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.entity.SubmissionType;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SummonsProsecutionCase;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import javax.json.Json;

import liquibase.repackaged.net.sf.jsqlparser.expression.NullValue;
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

    @Mock
    private UtcClock utcClock;

    @InjectMocks
    private SubmissionEventListener submissionEventListener;

    @Captor
    private ArgumentCaptor<Submission> argumentCaptor;

    @Test
    public void shouldChargeProsecution() {

        final UUID submissionId = randomUUID();
        final String prosecutingAuthority = "GAAAA01";
        final String urn = "urn_value";
        final ChargeProsecutionReceived chargeProsecutionReceived = ChargeProsecutionReceived.chargeProsecutionReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority(prosecutingAuthority)
                .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase().withUrn(urn).build()))
                .build();

        final Envelope<ChargeProsecutionReceived> envelope = newEnvelope("stagingprosecutorscivil.event.charge-prosecution-received", chargeProsecutionReceived);

        submissionEventListener.chargeProsecutionReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.name()));
        assertThat(submission.getOuCode(), is(prosecutingAuthority));
        assertThat(submission.getCaseDetail().stream().findFirst().get().getCaseUrn(), is(urn));
    }

    @Test
    public void shouldSummonsProsecution() {

        final UUID submissionId = randomUUID();
        final String prosecutingAuthority = "GAAAA01";
        final String urn = "urn_value";
        final SummonsProsecutionReceived summonsProsecutionReceived = SummonsProsecutionReceived.summonsProsecutionReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority(prosecutingAuthority)
                .withProsecutionCases(Collections.singletonList(SummonsProsecutionCase.summonsProsecutionCase().withUrn(urn).build()))
                .build();

        final Envelope<SummonsProsecutionReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", summonsProsecutionReceived);

        submissionEventListener.summonsProsecutionReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.name()));
        assertThat(submission.getOuCode(), is(prosecutingAuthority));
        assertThat(submission.getCaseDetail().stream().findFirst().get().getCaseUrn(), is(urn));
        assertThat(submission.getType(), is(SubmissionType.PROSECUTION));
    }

    @Test
    public void shouldUpdateCaseFile() {
        final UUID submissionId = randomUUID();
        final UpdateCivilCaseReceived summonsProsecutionReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .build();

        Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING.name())
                .build();

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", summonsProsecutionReceived);
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
        final CaseProblem caseError = CaseProblem.caseProblem()
                .withProsecutorCaseReference("URN01")
                .withProblems(Collections.singletonList(uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem().withCode("CASE_ERR").build()))
                .build();
        final CaseProblem groupCaseError = CaseProblem.caseProblem()
                .withProsecutorCaseReference(null)
                .withProblems(Collections.singletonList(uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem().withCode("GROUP_ERR").build()))
                .build();

        final UpdateCivilCaseReceived summonsProsecutionReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED)
                .withCaseErrors(Collections.singletonList(caseError))
                .withGroupCaseErrors(Collections.singletonList(groupCaseError))
                .withDefendantErrors(Collections.EMPTY_LIST)
                .build();

        Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED.name())
                .build();

        final javax.json.JsonObject caseErrorJson = Json.createObjectBuilder().add("prosecutorCaseReference", "URN01").build();
        // no prosecutorCaseReference key: group-level problems aren't tied to one case, and the real
        // converter (NON_ABSENT inclusion) omits the field entirely rather than emitting null
        final javax.json.JsonObject groupCaseErrorJson = Json.createObjectBuilder().add("problems", Json.createArrayBuilder()).build();
        when(objectToJsonObjectConverter.convert(caseError)).thenReturn(caseErrorJson);
        when(objectToJsonObjectConverter.convert(groupCaseError)).thenReturn(groupCaseErrorJson);
        when(utcClock.now()).thenReturn(ZonedDateTime.now(UTC));

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", summonsProsecutionReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(REJECTED.name()));
        assertThat(submission.getErrors(), is(nullValue()));
        assertThat(submission.getGroupCaseErrors(), contains(caseErrorJson, groupCaseErrorJson));
        assertThat(submission.getCompletedAt(), is(notNullValue()));
    }

    @Test
    void shouldUpdateCaseFileForRejectedStatusWithOnlyCaseLevelErrors() {
        final UUID submissionId = randomUUID();
        final CaseProblem caseError = CaseProblem.caseProblem()
                .withProsecutorCaseReference("URN01")
                .withProblems(Collections.singletonList(uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem().withCode("CASE_ERR").build()))
                .build();

        final UpdateCivilCaseReceived updateCivilCaseReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED)
                .withCaseErrors(Collections.singletonList(caseError))
                .withDefendantErrors(Collections.EMPTY_LIST)
                .build();

        final Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED.name())
                .build();

        final javax.json.JsonObject caseErrorJson = Json.createObjectBuilder().add("prosecutorCaseReference", "URN01").build();
        when(objectToJsonObjectConverter.convert(caseError)).thenReturn(caseErrorJson);

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", updateCivilCaseReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getGroupCaseErrors(), contains(caseErrorJson));
    }

    @Test
    void shouldPersistEmptyGroupCaseErrorsWhenRejectedWithNeitherCaseNorGroupLevelErrors() {
        final UUID submissionId = randomUUID();

        final UpdateCivilCaseReceived updateCivilCaseReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED)
                .withDefendantErrors(Collections.EMPTY_LIST)
                .build();

        final Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(REJECTED.name())
                .build();

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", updateCivilCaseReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();

        // deliberate: an empty JsonArray, not null — the JsonArrayConverter normalises a null column
        // to an empty array on read anyway, so null vs [] is not a distinct wire shape downstream
        assertThat(submission.getGroupCaseErrors(), is(notNullValue()));
        assertThat(submission.getGroupCaseErrors().isEmpty(), is(true));
    }

    @Test
    void shouldUpdateCaseFileForSuccessWithWarningsStatus() {
        final UUID submissionId = randomUUID();
        final CaseProblem caseWarning = CaseProblem.caseProblem()
                .withProsecutorCaseReference("URN01")
                .withProblems(Collections.singletonList(
                        uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem().withCode("CASE_WARN").build()))
                .build();

        final UpdateCivilCaseReceived summonsProsecutionReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SUCCESS_WITH_WARNINGS)
                .withWarnings(Collections.EMPTY_LIST)
                .withCaseWarnings(Collections.singletonList(caseWarning))
                .withDefendantWarnings(Collections.EMPTY_LIST)
                .build();

        Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SUCCESS_WITH_WARNINGS.name())
                .build();

        // caseWarnings is CaseProblem-shaped, exactly like caseErrors/groupCaseErrors
        final javax.json.JsonObject caseWarningJson = Json.createObjectBuilder().add("prosecutorCaseReference", "URN01").build();
        when(objectToJsonObjectConverter.convert(caseWarning)).thenReturn(caseWarningJson);

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", summonsProsecutionReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        when(utcClock.now()).thenReturn(ZonedDateTime.now(UTC));
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(SUCCESS_WITH_WARNINGS.name()));
        assertThat(submission.getCompletedAt(), is(notNullValue()));
        assertThat(submission.getCaseWarnings(), contains(caseWarningJson));
        assertThat(submission.getWarnings(), is(nullValue()));
        assertThat(submission.getDefendantWarnings(), is(notNullValue()));
        assertThat(submission.getDefendantWarnings().isEmpty(), is(true));
    }

    @Test
    void shouldUpdateCaseFileForSuccessStatus() {
        final UUID submissionId = randomUUID();
        final UpdateCivilCaseReceived updateCivilCaseReceived = UpdateCivilCaseReceived.updateCivilCaseReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SUCCESS)
                .build();

        final Submission inputSubmission = Submission.builder()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING.name())
                .build();

        final Envelope<UpdateCivilCaseReceived> envelope = newEnvelope("stagingprosecutorscivil.event.summons-prosecution-received", updateCivilCaseReceived);
        when(submissionRepository.findBy(any())).thenReturn(inputSubmission);
        when(utcClock.now()).thenReturn(ZonedDateTime.now(UTC));
        submissionEventListener.updatedCivilCaseReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(SUCCESS.name()));
        assertThat(submission.getCompletedAt(), is(notNullValue()));
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
