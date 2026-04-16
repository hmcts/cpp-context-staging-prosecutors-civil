package uk.gov.moj.cpp.staging.civil.query;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

public class CivilProsecutionQueryView {
    private static final Logger LOGGER = getLogger(CivilProsecutionQueryView.class);
    @Inject
    private SubmissionRepository submissionRepository;

    public JsonEnvelope querySubmission(JsonEnvelope envelope) {

        final UUID submissionId = fromString(envelope.payloadAsJsonObject()
                .getString("submissionId"));
        LOGGER.info("Query Submission for Id {} ", submissionId);
        final Optional<Submission> submissionOptional = Optional.ofNullable(submissionRepository.findBy(submissionId));

        final JsonObject payload = submissionOptional
                .map(submission -> {
                            final JsonObjectBuilder result = createObjectBuilder()
                                    .add("id", submission.getSubmissionId().toString())
                                    .add("status", submission.getSubmissionStatus())
                                    .add("warnings", submission.getWarnings())
                                    .add("errors", submission.getErrors());
                            if (nonNull(submission.getGroupCaseErrors())) {
                                result.add("caseErrors", submission.getGroupCaseErrors());
                            }
                            if (nonNull(submission.getDefendantErrors())) {
                                result.add("defendantErrors", submission.getDefendantErrors());
                            }
                            return result.build();
                        }
                )
                .orElse(null);

        return envelopeFrom(metadataFrom(envelope.metadata())
                .withName("stagingprosecutorscivil.query.submission-details"), payload);

    }
}
