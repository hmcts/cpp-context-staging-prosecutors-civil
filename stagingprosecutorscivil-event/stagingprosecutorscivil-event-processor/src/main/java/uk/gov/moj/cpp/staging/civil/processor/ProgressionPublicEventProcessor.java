package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionPublicEventProcessor {
    static final String SUBMISSION_ID = "submissionId";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionPublicEventProcessor.class);

    @Inject
    private Sender sender;

    @Handles("public.progression.court-document-added")
    public void caseDocumentUploaded(final JsonEnvelope courtDocumentAdded) {
        LOGGER.info(".......Received public.progression.court-document-added event with metadata: {} and payload: {}",
                courtDocumentAdded.metadata(), courtDocumentAdded.toObfuscatedDebugString());
        final JsonObject metadataJson = courtDocumentAdded.metadata().asJsonObject();

        final Optional<UUID> submissionId = ofNullable(
                courtDocumentAdded.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);

        LOGGER.info(".........Extracted submissionId: {}", submissionId);

        if (submissionId.isPresent()) {
            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                    .add(SUBMISSION_ID, submissionId.get().toString());

            final Metadata metadata = Envelope.metadataFrom(courtDocumentAdded.metadata()).withName("stagingprosecutorscivil.command.receive-material-submission-successful").build();
            sender.send(envelopeFrom(
                    metadata,
                    jsonObjectBuilder.build()));
            LOGGER.info("Sent stagingprosecutorscivil.command.receive-material-submission-successful command with submissionId: {}", submissionId.get());
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Received CourtDocumentAdded event with no submissionId[Metadata: {}], [Payload: {}]",
                        metadataJson, courtDocumentAdded.toObfuscatedDebugString());
            }
        }
    }
}
