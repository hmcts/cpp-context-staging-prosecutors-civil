package uk.gov.moj.cpp.staging.civil.event.listener;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

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
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    private ZonedDateTime extractCreatedAt(final Metadata metadata) {
        return metadata.createdAt().orElseThrow(() -> new IllegalArgumentException("metadata createdAt is not present"));
    }

    private JsonArray transformErrorsToJsonArray(final Collection<Problem> errorsOrWarnings) {
        if (errorsOrWarnings == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        errorsOrWarnings.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }

    private JsonArray transformDefendantProblemsToJsonArray(final Collection<DefendantProblem> errors) {
        if (errors == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        errors.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }
}

