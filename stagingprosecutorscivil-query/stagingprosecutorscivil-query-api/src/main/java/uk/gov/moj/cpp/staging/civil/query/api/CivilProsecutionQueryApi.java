package uk.gov.moj.cpp.staging.civil.query.api;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.civil.query.CivilProsecutionQueryView;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CivilProsecutionQueryApi {

    @Inject
    private CivilProsecutionQueryView civilProsecutionQueryView;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Requester requester;

    @Inject
    private ProsecutingAuthorityValidationService prosecutingAuthorityValidationService;

    @Handles("stagingprosecutorscivil.submission-details")
    public JsonEnvelope getSubmissionDetails(final JsonEnvelope envelope) {

        final String userId = requireUserId(envelope);
        final Optional<Submission> submissionOptional = validateSubmissionId(envelope, userId);

        final JsonEnvelope queryEnvelop = envelopeFrom(metadataFrom(envelope.metadata())
                .withName("stagingprosecutorscivil.query.submission-details"), envelope.payloadAsJsonObject());

        return civilProsecutionQueryView.buildSubmissionDetailsResponse(queryEnvelop, submissionOptional);

    }

    @Handles("stagingprosecutorscivil.submission-error-details")
    public JsonEnvelope getSubmissionErrorDetailsCsv(final JsonEnvelope envelope) {

        final String userId = requireUserId(envelope);
        final Optional<Submission> submissionOptional = validateSubmissionId(envelope, userId);

        final JsonEnvelope queryEnvelop = envelopeFrom(metadataFrom(envelope.metadata())
                .withName("stagingprosecutorscivil.query.submission-error-details-csv"), envelope.payloadAsJsonObject());

        return civilProsecutionQueryView.buildSubmissionErrorDetailsCsvResponse(queryEnvelop, submissionOptional);

    }

    private boolean callerBelongsToSubmissionAuthority(final String userId, final Submission submission) {
        return prosecutingAuthorityValidationService.callerBelongsToProsecutingAuthority(
                userId, submission.getOuCode(), submission.getProsecutorShortName());
    }

    @SuppressWarnings("squid:S1166")
    private Optional<Submission> validateSubmissionId(final JsonEnvelope envelope, final String userId) {
        final String submissionIdString = envelope.payloadAsJsonObject().getString("submissionId");
        final UUID submissionId;
        try {
            submissionId = fromString(submissionIdString);
        } catch (final IllegalArgumentException e) {
            throw new BadRequestException(format("Specified string %s, is not valid UUID", submissionIdString));
        }

        final Optional<Submission> submissionOptional = civilProsecutionQueryView.findSubmission(submissionId);
        submissionOptional.ifPresent(submission -> {
            if (!callerBelongsToSubmissionAuthority(userId, submission)) {
                throw new BadRequestException("The prosecutor information does not match");
            }
        });
        return submissionOptional;
    }

    private String requireUserId(final JsonEnvelope envelope) {
        return envelope.metadata().userId()
                .filter(userId -> !userId.isBlank())
                .orElseThrow(() -> new BadRequestException("Missing user id on request"));
    }

}
