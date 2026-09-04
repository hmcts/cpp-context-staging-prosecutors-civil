package uk.gov.moj.cpp.staging.civil.event.listener;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
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
import uk.gov.moj.cpp.staging.prosecutors.civil.event.OtherCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
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

    @Handles("stagingprosecutorscivil.event.other-case-received")
    public void otherCaseReceived(final Envelope<OtherCaseReceived> event) {
        LOGGER.info("stagingprosecutorscivil.event.other-case-received event received in Listener for SubmissionId {}", event.payload().getSubmissionId());

        final OtherCaseReceived otherCaseReceived = event.payload();

        final Set<CaseDetail> caseDetails = new HashSet<>();
        otherCaseReceived.getProsecutionCases().forEach(prosecutionCase -> caseDetails.add(CaseDetail
                .builder()
                .withId(randomUUID())
                .withCaseUrn(prosecutionCase.getUrn())
                .build()));

        final Submission submission = Submission.builder()
                .withSubmissionId(otherCaseReceived.getSubmissionId())
                .withSubmissionStatus(otherCaseReceived.getSubmissionStatus().name())
                .withOuCode(otherCaseReceived.getProsecutingAuthority())
                .withReceivedAt(extractCreatedAt(event.metadata()))
                .withCaseDetail(caseDetails)
                .withErrors(null)
                .withWarnings(null)
                .withType(SubmissionType.PROSECUTION)
                .build();

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutorscivil.event.summons-received")
    public void summonsReceived(final Envelope<SummonsReceived> event) {
        LOGGER.info("stagingprosecutorscivil.event.summons-received event received in Listener  for SubmissionId {}", event.payload().getSubmissionId());

        final SummonsReceived summonsReceived = event.payload();

        final Set<CaseDetail> caseDetails = new HashSet<>();
        summonsReceived.getProsecutionCases().forEach(prosecutionCase -> caseDetails.add(CaseDetail
                .builder()
                .withId(randomUUID())
                .withCaseUrn(prosecutionCase.getUrn())
                .build()));

        final Submission submission = Submission.builder()
                .withSubmissionId(summonsReceived.getSubmissionId())
                .withSubmissionStatus(summonsReceived.getSubmissionStatus().name())
                .withOuCode(summonsReceived.getProsecutingAuthority())
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

        if (isStaleUpdate(submission, updatedCivilCaseReceived.getSubmissionStatus())) {
            LOGGER.info("Ignoring stale stagingprosecutorscivil.event.update-civil-case-received event with status {} for SubmissionId {} - submission is already in terminal status {}",
                    updatedCivilCaseReceived.getSubmissionStatus(), updatedCivilCaseReceived.getSubmissionId(), submission.getSubmissionStatus());
            return;
        }

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

        if(nonNull(submission)) {
            submission.setCompletedAt(extractCreatedAt(envelope.metadata()));
            submission.setSubmissionStatus(SUCCESS.toString());
            submissionRepository.save(submission);
        }
    }

    private boolean isStaleUpdate(final Submission submission, final SubmissionStatus incomingStatus) {
        if (submission == null) {
            return false;
        }
        final SubmissionStatus currentStatus = SubmissionStatus.valueOf(submission.getSubmissionStatus());
        return SubmissionStatus.PENDING.equals(incomingStatus) && !SubmissionStatus.PENDING.equals(currentStatus);
    }

    private void submissionRejected(final UUID submissionId, final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem> errors, final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem> warnings, final ZonedDateTime timestamp) {
        final JsonArray submissionErrors = transformErrorsOrWarningsToJsonArray(errors);
        final JsonArray submissionWarnings = transformErrorsOrWarningsToJsonArray(warnings);
        final Submission submission = submissionRepository.findBy(submissionId);

        if(nonNull(submission)) {
            submission.setSubmissionStatus(SubmissionStatus.REJECTED.toString());
            submission.setCompletedAt(timestamp);
            submission.setErrors(submissionErrors);
            submission.setWarnings(submissionWarnings);
            submissionRepository.save(submission);
        }
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

