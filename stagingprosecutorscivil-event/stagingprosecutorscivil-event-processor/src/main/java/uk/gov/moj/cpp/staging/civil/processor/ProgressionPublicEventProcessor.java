package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Objects.nonNull;
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
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;

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

    @Inject
    private SubmissionRepository submissionRepository;

    @Handles("public.progression.court-document-added")
    public void caseDocumentUploaded(final JsonEnvelope courtDocumentAdded) {
        LOGGER.info("Received public.progression.court-document-added event");

        final Optional<UUID> submissionId = ofNullable(
                courtDocumentAdded.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);

        LOGGER.info("Extracted submissionId: {}", submissionId);

        if (submissionId.isPresent()) {
            final Submission submission = submissionRepository.findBy(submissionId.get());

            if (nonNull(submission)) {
                final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                        .add(SUBMISSION_ID, submissionId.get().toString());

                final Metadata metadata = Envelope.metadataFrom(courtDocumentAdded.metadata()).withName("stagingprosecutorscivil.command.receive-material-submission-successful").build();
                sender.send(envelopeFrom(
                        metadata,
                        jsonObjectBuilder.build()));
            } else {
                LOGGER.info("No submission found for submissionId: {}, skipping command send", submissionId.get());
            }
        }
    }
}
