package uk.gov.moj.cpp.staging.civil.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSubmissionSucceeded;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class GroupSubmissionSucceededPublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupSubmissionSucceededPublicEventProcessor.class);

    @Inject
    private Sender sender;

    @Handles("public.prosecutioncasefile.group-submission-succeeded")
    public void groupSubmissionSucceeded(final Envelope<GroupSubmissionSucceeded> groupSubmissionSucceededEnvelope) {

        final GroupSubmissionSucceeded payload = groupSubmissionSucceededEnvelope.payload();
        final String submissionId = payload.getExternalId() != null ? payload.getExternalId().toString() : null;

        LOGGER.info("Received public.prosecutioncasefile.group-submission-succeeded event with payload for submission id {} ", submissionId);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("submissionId", submissionId)
                .add("submissionStatus", SubmissionStatus.SUCCESS.name());
        sender.send(envelop(jsonObjectBuilder.build())
                .withName("stagingprosecutorscivil.command.update-civil-case")
                .withMetadataFrom(groupSubmissionSucceededEnvelope));

    }
}
