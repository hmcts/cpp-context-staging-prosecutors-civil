package uk.gov.moj.cpp.staging.civil.event.listener;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.OtherCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    private ZonedDateTime extractCreatedAt(final Metadata metadata) {
        return metadata.createdAt().orElseThrow(() -> new IllegalArgumentException("metadata createdAt is not present"));
    }

    private JsonArray transformErrorsToJsonArray(final Collection<Problem> problems) {
        if (problems == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        problems.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }

    private JsonArray transformDefendantProblemsToJsonArray(final Collection<DefendantProblem> problems) {
        if (problems == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        problems.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }
}

