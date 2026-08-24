package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CivilProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import java.util.Collection;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionSubmissionSucceededPublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionSubmissionSucceededPublicEventProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("public.prosecutioncasefile.civil.prosecution-submission-succeeded")
    public void prosecutionSubmissionSucceeded(final Envelope<CivilProsecutionSubmissionSucceeded> envelope) {

        final CivilProsecutionSubmissionSucceeded payload = envelope.payload();
        final String submissionId = nonNull(payload.getExternalId()) ? payload.getExternalId().toString() : null;

        LOGGER.info("Received public.prosecutioncasefile.civil-prosecution-submission-succeeded event with payload for submission id {} ", submissionId);

        if (nonNull(submissionId) && CIVIL.equals(payload.getChannel())) {
            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                    .add("submissionId", submissionId)
                    .add("submissionStatus", SubmissionStatus.SUCCESS.name());
            sender.send(envelop(jsonObjectBuilder.build())
                    .withName("stagingprosecutorscivil.command.update-civil-case")
                    .withMetadataFrom(envelope));
        } else {
            LOGGER.info("Message unrelated to CIVIL channel.  Not processing");
        }
    }

    @Handles("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings")
    public void prosecutionSubmissionSucceededWithWarnings(final Envelope<ProsecutionSubmissionSucceededWithWarnings> prosecutionSubmissionSucceededWithWarningsEnvelope) {

        final ProsecutionSubmissionSucceededWithWarnings payload = prosecutionSubmissionSucceededWithWarningsEnvelope.payload();
        final String submissionId = payload.getExternalId()!= null ? payload.getExternalId().toString(): null;

        LOGGER.info("Received public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings event with payload for submission id {} ", submissionId);

        if (nonNull(submissionId) && CIVIL.equals(payload.getChannel())) {
            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                    .add("submissionId", submissionId)
                    .add("submissionStatus", SubmissionStatus.SUCCESS_WITH_WARNINGS.name());
            ofNullable(toJsonArray(payload.getWarnings()))
                    .ifPresent(warnings -> jsonObjectBuilder.add("warnings", warnings));
            // the case-level list is sourced from civilCaseWarnings (CaseProblem[]); the event's
            // legacy caseWarnings (flat Problem[]) is never populated and must not be read
            ofNullable(toJsonArray(payload.getCivilCaseWarnings()))
                    .ifPresent(caseWarnings -> jsonObjectBuilder.add("caseWarnings", caseWarnings));
            ofNullable(toJsonArray(payload.getDefendantWarnings()))
                    .ifPresent(defendantWarnings -> jsonObjectBuilder.add("defendantWarnings", defendantWarnings));

            sender.send(envelop(jsonObjectBuilder.build())
                    .withName("stagingprosecutorscivil.command.update-civil-case")
                    .withMetadataFrom(prosecutionSubmissionSucceededWithWarningsEnvelope));
        } else {
            LOGGER.info("Message unrelated to CIVIL channel.  Not processing");
        }
    }

    private JsonArray toJsonArray(final Collection<?> problems) {
        if (problems == null) {
            return null;
        }
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        problems.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }
}
