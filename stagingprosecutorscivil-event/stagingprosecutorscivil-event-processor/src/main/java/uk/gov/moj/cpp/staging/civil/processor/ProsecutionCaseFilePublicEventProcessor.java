package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
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

    @Handles("public.prosecutioncasefile.material-rejected")
    public void caseMaterialRejected(final Envelope<MaterialRejected> materialRejectedEnvelope) {

        final Optional<UUID> submissionId = ofNullable(materialRejectedEnvelope.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info(SUBMISSION_ID_NOT_FOUND);
            return;
        }

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, submissionId.get().toString())
                .add("errors", materialRejectedEnvelope.metadata().asJsonObject().getJsonArray("errors"));

        sender.send(envelop(jsonObjectBuilder.build())
                .withName(STAGING_PROSECUTORS_COMMAND_REJECT_MATERIAL)
                .withMetadataFrom(materialRejectedEnvelope));
    }
}
