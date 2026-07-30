package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
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
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseFilePublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseFilePublicEventProcessor.class);
    private static final String SUBMISSION_ID_NOT_FOUND = "Submission ID not found. Material rejected event ignored";
    private static final String STAGING_PROSECUTORS_COMMAND_REJECT_MATERIAL = "stagingprosecutorscivil.command.reject-material";
    private static final String SUBMISSION_ID = "submissionId";
    @Inject
    private Sender sender;
    @Inject
    private SubmissionRepository submissionRepository;

    @Handles("public.prosecutioncasefile.material-rejected")
    public void caseMaterialRejected(final JsonEnvelope materialRejectedEnvelope) {
        final Optional<UUID> submissionId = ofNullable(materialRejectedEnvelope.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);
        LOGGER.info("Received public.prosecutioncasefile.material-rejected event with metadata: {} and payload: {}",
                materialRejectedEnvelope.metadata(), materialRejectedEnvelope.toObfuscatedDebugString());

        LOGGER.info("submission id {} ", submissionId);

        if (submissionId.isPresent()) {
            final Submission submission = submissionRepository.findBy(submissionId.get());

            if (nonNull(submission)) {
                final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                        .add(SUBMISSION_ID, submissionId.get().toString());

                final JsonArray errors = materialRejectedEnvelope.payload().asJsonObject().getJsonArray("errors");
                if (nonNull(errors) && !errors.isEmpty()) {
                    jsonObjectBuilder.add("errors", errors);
                }
                sender.send(envelop(jsonObjectBuilder.build())
                        .withName(STAGING_PROSECUTORS_COMMAND_REJECT_MATERIAL)
                        .withMetadataFrom(materialRejectedEnvelope));
            } else {
                LOGGER.info(SUBMISSION_ID_NOT_FOUND);
            }
        }
    }
}