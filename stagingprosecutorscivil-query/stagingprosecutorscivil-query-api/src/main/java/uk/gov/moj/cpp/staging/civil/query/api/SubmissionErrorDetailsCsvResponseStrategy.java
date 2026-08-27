package uk.gov.moj.cpp.staging.civil.query.api;

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;

import uk.gov.justice.services.adapter.rest.processor.response.ResponseStrategy;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Named;
import javax.ws.rs.core.Response;

/**
 * The framework's built-in response strategies always serialise the envelope payload as JSON, so
 * this strategy unwraps the CSV text placed on the payload by {@code CivilProsecutionQueryView}
 * and writes it as the raw response body with a {@code text/csv} content type instead.
 */
@Named("SubmissionErrorDetailsCsvResponseStrategy")
public class SubmissionErrorDetailsCsvResponseStrategy implements ResponseStrategy {

    private static final String CSV_MIME_TYPE = "text/csv";

    @Override
    public Response responseFor(final String action, final Optional<JsonEnvelope> envelope) {
        return envelope
                .map(this::toCsvResponse)
                .orElseGet(() -> status(OK).build());
    }

    private Response toCsvResponse(final JsonEnvelope envelope) {
        final String submissionId = envelope.payloadAsJsonObject().getString("submissionId");
        final String csv = envelope.payloadAsJsonObject().getString("csv");

        return status(OK)
                .entity(csv)
                .header(CONTENT_TYPE, CSV_MIME_TYPE)
                .header(CONTENT_DISPOSITION, "attachment; filename=\"submission-" + submissionId + "-errors.csv\"")
                .build();
    }
}
