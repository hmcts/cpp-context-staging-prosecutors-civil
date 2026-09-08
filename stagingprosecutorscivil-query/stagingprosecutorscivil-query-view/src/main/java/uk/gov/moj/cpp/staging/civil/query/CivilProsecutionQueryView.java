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
    private static final String SUBMISSION_ID = "submissionId";

    @Inject
    private SubmissionRepository submissionRepository;

    /**
     * Exposed so callers (e.g. the query-api's prosecuting-authority check) can inspect a
     * submission's ouCode/prosecutorShortName ahead of - and independently of - building the full
     * response payload via {@link #buildSubmissionDetailsResponse(JsonEnvelope, Optional)}.
     */
    public Optional<Submission> findSubmission(final UUID submissionId) {
        return Optional.ofNullable(submissionRepository.findBy(submissionId));
    }

    public JsonEnvelope querySubmission(final JsonEnvelope envelope) {
        final UUID submissionId = fromString(envelope.payloadAsJsonObject().getString(SUBMISSION_ID));
        LOGGER.info("Query Submission for Id {} ", submissionId);
        return buildSubmissionDetailsResponse(envelope, findSubmission(submissionId));
    }

    /**
     * Renders a submission-details response from an already-resolved {@code submissionOptional} -
     * unlike {@link #querySubmission(JsonEnvelope)}, this does not itself query the repository, so
     * a caller that must inspect the submission before deciding whether to return it at all (e.g.
     * the query-api's prosecuting-authority check) can do so with a single
     * {@link #findSubmission(UUID)} lookup: passing {@code Optional.empty()} here reuses this
     * method's existing "not found" response shape for a caller who isn't authorised to see an
     * existing submission.
     */
    public JsonEnvelope buildSubmissionDetailsResponse(final JsonEnvelope envelope, final Optional<Submission> submissionOptional) {

        final JsonObject requestPayload = envelope.payloadAsJsonObject();
        final boolean additionalInfo = requestPayload.getBoolean("additionalInfo", false);

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
        if (nonNull(submission.getSummonsApplicationId())) {
            result.add("summonsApplicationId", submission.getSummonsApplicationId().toString());
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

    public JsonEnvelope querySubmissionErrorDetailsCsv(final JsonEnvelope envelope) {
        final UUID submissionId = fromString(envelope.payloadAsJsonObject().getString(SUBMISSION_ID));
        LOGGER.info("Query Submission Error Details CSV for Id {} ", submissionId);
        return buildSubmissionErrorDetailsCsvResponse(envelope, findSubmission(submissionId));
    }

    /**
     * Renders an error-details-CSV response from an already-resolved {@code submissionOptional} -
     * see {@link #buildSubmissionDetailsResponse(JsonEnvelope, Optional)} for why.
     */
    public JsonEnvelope buildSubmissionErrorDetailsCsvResponse(final JsonEnvelope envelope, final Optional<Submission> submissionOptional) {

        final JsonObject requestPayload = envelope.payloadAsJsonObject();
        final UUID submissionId = fromString(requestPayload.getString(SUBMISSION_ID));

        // a submission with no case/defendant errors, or one that can't be found (or can't be
        // accessed), all yield a header-only CSV rather than a 404 - consistent with the JSON
        // path never 404-ing either

        final String csv = submissionOptional
                .map(submission -> SubmissionErrorDetailsCsvBuilder.build(submission.getGroupCaseErrors(), submission.getDefendantErrors()))
                .orElseGet(() -> SubmissionErrorDetailsCsvBuilder.build(null, null));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, submissionId.toString())
                .add("csv", csv);

        // fileName is only captured on submissions that originated from a complaints CSV upload;
        // the response strategy falls back to a submissionId-based name when it is absent
        final Optional<String> fileName = submissionOptional.map(Submission::getFileName);
        LOGGER.info("Submission {} fileName for error CSV naming: {}", submissionId, fileName.orElse("<absent>"));
        fileName.ifPresent(name -> payloadBuilder.add("fileName", name));

        final JsonObject payload = payloadBuilder.build();

        return envelopeFrom(metadataFrom(envelope.metadata())
                .withName("stagingprosecutorscivil.query.submission-error-details-csv"), payload);

    }

    private static JsonArray orEmptyArray(final JsonArray value) {
        return nonNull(value) ? value : createArrayBuilder().build();
    }
}
