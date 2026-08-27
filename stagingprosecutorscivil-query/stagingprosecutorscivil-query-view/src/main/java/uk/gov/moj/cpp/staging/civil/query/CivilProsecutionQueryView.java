package uk.gov.moj.cpp.staging.civil.query;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

public class CivilProsecutionQueryView {
    private static final Logger LOGGER = getLogger(CivilProsecutionQueryView.class);
    @Inject
    private SubmissionRepository submissionRepository;

    public JsonEnvelope querySubmission(JsonEnvelope envelope) {

        final JsonObject requestPayload = envelope.payloadAsJsonObject();
        final UUID submissionId = fromString(requestPayload.getString("submissionId"));
        final boolean additionalInfo = requestPayload.getBoolean("additionalInfo", false);
        LOGGER.info("Query Submission for Id {} ", submissionId);
        final Optional<Submission> submissionOptional = Optional.ofNullable(submissionRepository.findBy(submissionId));

        final JsonObject payload = submissionOptional
                .map(submission -> buildSubmissionDetailsPayload(submission, additionalInfo))
                .orElse(null);

        return envelopeFrom(metadataFrom(envelope.metadata())
                .withName("stagingprosecutorscivil.query.submission-details"), payload);

    }

    private static JsonObject buildSubmissionDetailsPayload(final Submission submission, final boolean additionalInfo) {
        // every array attribute is always present: consumers can rely on the key existing, with
        // an empty array standing in for "no data"
        final JsonObjectBuilder result = createObjectBuilder()
                .add("id", submission.getSubmissionId().toString())
                .add("status", submission.getSubmissionStatus())
                .add("materialWarnings", orEmptyArray(submission.getWarnings()))
                .add("materialErrors", orEmptyArray(submission.getErrors()))
                .add("caseErrors", orEmptyArray(submission.getGroupCaseErrors()))
                .add("defendantErrors", orEmptyArray(submission.getDefendantErrors()))
                .add("caseWarnings", orEmptyArray(submission.getCaseWarnings()))
                .add("defendantWarnings", orEmptyArray(submission.getDefendantWarnings()));

        addTypeAndTimestamps(result, submission);

        if (additionalInfo) {
            addAdditionalInfo(result, submission);
        }

        return result.build();
    }

    // type and received_at carry no NOT NULL constraint in the viewstore, so a legacy row could
    // still hold null; guard rather than fail the whole query. completedAt is the sole genuinely
    // optional attribute: absent until the submission reaches a terminal status
    private static void addTypeAndTimestamps(final JsonObjectBuilder result, final Submission submission) {
        if (nonNull(submission.getType())) {
            result.add("type", submission.getType().name());
        }
        if (nonNull(submission.getReceivedAt())) {
            result.add("receivedAt", ZonedDateTimes.toString(submission.getReceivedAt()));
        }
        if (nonNull(submission.getCompletedAt())) {
            result.add("completedAt", ZonedDateTimes.toString(submission.getCompletedAt()));
        }
    }

    private static void addAdditionalInfo(final JsonObjectBuilder result, final Submission submission) {
        if (nonNull(submission.getFileName())) {
            result.add("fileName", submission.getFileName());
        }
        if (nonNull(submission.getSubmittedByUserName())) {
            result.add("username", submission.getSubmittedByUserName());
        }
        if (nonNull(submission.getProsecutorShortName())) {
            result.add("prosecutingAuthority", submission.getProsecutorShortName());
        }
    }

    private static JsonArray orEmptyArray(final JsonArray value) {
        return nonNull(value) ? value : createArrayBuilder().build();
    }
}
