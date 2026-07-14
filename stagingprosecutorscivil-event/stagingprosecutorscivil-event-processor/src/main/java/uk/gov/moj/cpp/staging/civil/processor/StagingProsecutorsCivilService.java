package uk.gov.moj.cpp.staging.civil.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StagingProsecutorsCivilService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingProsecutorsCivilService.class);
    private static final String QUERY_SUBMISSION_DETAILS = "stagingprosecutorscivil.submission-details";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public Optional<JsonObject> submissionExistsById(final JsonEnvelope envelope, final String submissionId) {

        final JsonObject requestParameter = createObjectBuilder().add("submissionId", submissionId).build();

        LOGGER.info("Submission Id {} --- ", submissionId);

        final JsonEnvelope response = requester.request(enveloper.withMetadataFrom(envelope, QUERY_SUBMISSION_DETAILS).apply(requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("submissionExistsById - Response --- {}", response.toObfuscatedDebugString());
        }

        return Optional.ofNullable(response.payloadAsJsonObject());
    }
}
