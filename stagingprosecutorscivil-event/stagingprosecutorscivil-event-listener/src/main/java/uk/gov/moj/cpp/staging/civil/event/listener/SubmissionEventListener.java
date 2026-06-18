package uk.gov.moj.cpp.staging.civil.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.SUCCESS;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.entity.SubmissionType;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

import org.slf4j.Logger;

@ServiceComponent(EVENT_LISTENER)
public class SubmissionEventListener {

    private static final Logger LOGGER = getLogger(SubmissionEventListener.class);

    @Inject
    private SubmissionRepository submissionRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("stagingprosecutorscivil.event.charge-prosecution-received")
    public void chargeProsecutionReceived(final Envelope<ChargeProsecutionReceived> event) {
        LOGGER.info("stagingprosecutorscivil.event.charge-prosecution-received event received in Listener for SubmissionId {}", event.payload().getSubmissionId());

        final ChargeProsecutionReceived chargeProsecutionReceived = event.payload();

        final Set<CaseDetail> caseDetails = new HashSet<>();
        chargeProsecutionReceived.getProsecutionCases().forEach(prosecutionCase -> caseDetails.add(CaseDetail
                .builder()
                .withId(randomUUID())
                .withCaseUrn(prosecutionCase.getUrn())
                .build()));

        final Submission submission = Submission.builder()
                .withSubmissionId(chargeProsecutionReceived.getSubmissionId())
                .withSubmissionStatus(chargeProsecutionReceived.getSubmissionStatus().name())
                .withOuCode(chargeProsecutionReceived.getProsecutingAuthority())
                .withReceivedAt(extractCreatedAt(event.metadata()))
                .withCaseDetail(caseDetails)
                .withErrors(null)
                .withWarnings(null)
                .withType(SubmissionType.PROSECUTION)
                .build();

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutorscivil.event.summons-prosecution-received")
    public void summonsProsecutionReceived(final Envelope<SummonsProsecutionReceived> event) {
        LOGGER.info("stagingprosecutorscivil.event.summons-prosecution-received event received in Listener  for SubmissionId {}", event.payload().getSubmissionId());

        final SummonsProsecutionReceived summonsProsecutionReceived = event.payload();

        final Set<CaseDetail> caseDetails = new HashSet<>();
        summonsProsecutionReceived.getProsecutionCases().forEach(prosecutionCase -> caseDetails.add(CaseDetail
                .builder()
                .withId(randomUUID())
                .withCaseUrn(prosecutionCase.getUrn())
                .build()));

        final Submission submission = Submission.builder()
                .withSubmissionId(summonsProsecutionReceived.getSubmissionId())
                .withSubmissionStatus(summonsProsecutionReceived.getSubmissionStatus().name())
                .withOuCode(summonsProsecutionReceived.getProsecutingAuthority())
                .withReceivedAt(extractCreatedAt(event.metadata()))
                .withCaseDetail(caseDetails)
                .withErrors(null)
                .withWarnings(null)
                .build();

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutorscivil.event.update-civil-case-received")
    public void updatedCivilCaseReceived(final Envelope<UpdateCivilCaseReceived> event) {
        LOGGER.info("stagingprosecutorscivil.event.update-civil-case-received event received in Listener for SubmissionId {}", event.payload().getSubmissionId());
        final UpdateCivilCaseReceived updatedCivilCaseReceived = event.payload();
        final Submission submission = submissionRepository.findBy(updatedCivilCaseReceived.getSubmissionId());
        if (SubmissionStatus.REJECTED.equals(updatedCivilCaseReceived.getSubmissionStatus())) {
            submission.setErrors(transformErrorsToJsonArray(updatedCivilCaseReceived.getCaseErrors()));
            submission.setGroupCaseErrors(transformErrorsToJsonArray(updatedCivilCaseReceived.getGroupCaseErrors()));
            submission.setDefendantErrors(transformDefendantProblemsToJsonArray(updatedCivilCaseReceived.getDefendantErrors()));
        } else if (SubmissionStatus.SUCCESS_WITH_WARNINGS.equals(updatedCivilCaseReceived.getSubmissionStatus())) {
            submission.setWarnings(transformErrorsToJsonArray(updatedCivilCaseReceived.getWarnings()));
            submission.setCaseWarnings(transformErrorsToJsonArray(updatedCivilCaseReceived.getCaseWarnings()));
            submission.setDefendantWarnings(transformDefendantProblemsToJsonArray(updatedCivilCaseReceived.getDefendantWarnings()));
        }
        submission.setSubmissionStatus(updatedCivilCaseReceived.getSubmissionStatus().name());
        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutorscivil.event.material-submitted")
    public void materialSubmitted(final Envelope<MaterialSubmitted> envelope) {
        LOGGER.info("stagingprosecutorscivil.event.material-submitted event received in Listener for SubmissionId {}", envelope.payload().getSubmissionId());
        final MaterialSubmitted materialSubmitted = envelope.payload();

        final Set<CaseDetail> caseDetails = new HashSet<>();
        caseDetails.add(CaseDetail
                .builder()
                .withId(randomUUID())
                .withCaseUrn(materialSubmitted.getCaseUrn())
                .build());

        final Submission submission = Submission.builder()
                .withSubmissionId(materialSubmitted.getSubmissionId())
                .withSubmissionStatus(materialSubmitted.getSubmissionStatus().name())
                .withOuCode(materialSubmitted.getProsecutingAuthority())
                .withReceivedAt(extractCreatedAt(envelope.metadata()))
                .withCaseDetail(caseDetails)
                .withErrors(null)
                .withWarnings(null)
                .withType(SubmissionType.MATERIAL)
                .build();

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutorscivil.event.material-submission-rejected")
    public void materialSubmissionRejected(final Envelope<MaterialSubmissionRejected> envelope) {
        final MaterialSubmissionRejected submissionRejected = envelope.payload();
        LOGGER.info("stagingprosecutorscivil.event.material-submission-rejected event received in Listener for SubmissionId {}", submissionRejected.getSubmissionId());

        submissionRejected(submissionRejected.getSubmissionId(), submissionRejected.getErrors(), submissionRejected.getWarnings(), extractCreatedAt(envelope.metadata()));
    }

    @Handles("stagingprosecutorscivil.event.material-submission-successful")
    public void materialSubmissionSuccessfulReceived(final Envelope<MaterialSubmissionSuccessful> envelope) {
        LOGGER.info("stagingprosecutorscivil.event.material-submission-successful event received in Listener for SubmissionId {}", envelope.payload().getSubmissionId());
        final MaterialSubmissionSuccessful materialSubmissionSuccessful = envelope.payload();
        final Submission submission = submissionRepository.findBy(materialSubmissionSuccessful.getSubmissionId());

        submission.setCompletedAt(extractCreatedAt(envelope.metadata()));
        submission.setSubmissionStatus(SUCCESS.toString());
        submissionRepository.save(submission);
    }

    private void submissionRejected(final UUID submissionId, final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem> errors, final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem> warnings, final ZonedDateTime timestamp) {
        final JsonArray submissionErrors = transformErrorsOrWarningsToJsonArray(errors);
        final JsonArray submissionWarnings = transformErrorsOrWarningsToJsonArray(warnings);
        final Submission submission = submissionRepository.findBy(submissionId);

        LOGGER.info("---------------SubmissionId {}", submission.getSubmissionId());

        submission.setSubmissionStatus(SubmissionStatus.REJECTED.toString());
        submission.setCompletedAt(timestamp);
        submission.setErrors(submissionErrors);
        submission.setWarnings(submissionWarnings);
        submissionRepository.save(submission);
        LOGGER.info("stagingprosecutorscivil.event.material-submission-rejected event processed in Listener for SubmissionId {} with errors: {} and warnings: {}", submissionId, submissionErrors, submissionWarnings);
    }

    private ZonedDateTime extractCreatedAt(final Metadata metadata) {
        return metadata.createdAt().orElseThrow(() -> new IllegalArgumentException("metadata createdAt is not present"));
    }

    private JsonArray transformErrorsToJsonArray(final Collection<Problem> errorsOrWarnings) {
        if (errorsOrWarnings == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        errorsOrWarnings.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }

    private JsonArray transformDefendantProblemsToJsonArray(final Collection<DefendantProblem> errors) {
        if (errors == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        errors.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }

    private JsonArray transformErrorsOrWarningsToJsonArray(final Collection<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem> errorsOrWarnings) {
        if (errorsOrWarnings == null) {
            return null;
        }

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();

        errorsOrWarnings.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);

        return arrayBuilder.build();
    }
}

