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
import uk.gov.moj.cpp.staging.civil.query.CivilProsecutionQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CivilProsecutionQueryApi {

    @Inject
    private CivilProsecutionQueryView civilProsecutionQueryView;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Requester requester;

    @Handles("stagingprosecutorscivil.submission-details")
    public JsonEnvelope getSubmissionDetails(final JsonEnvelope envelope) {

        validateSubmissionId(envelope);

        final JsonEnvelope queryEnvelop = envelopeFrom(metadataFrom(envelope.metadata())
                .withName("stagingprosecutorscivil.query.submission-details"), envelope.payloadAsJsonObject());
        return civilProsecutionQueryView.querySubmission(queryEnvelop);

    }

    @SuppressWarnings("squid:S1166")
    private void validateSubmissionId(final JsonEnvelope envelope) {
        final String submissionId = envelope.payloadAsJsonObject().getString("submissionId");
        try {
            fromString(submissionId);
        } catch (final IllegalArgumentException e) {
            throw new BadRequestException(format("Specified string %s, is not valid UUID", submissionId));
        }
    }

}
