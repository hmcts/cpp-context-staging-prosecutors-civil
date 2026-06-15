package uk.gov.moj.cpp.staging.civil.handler;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.civil.aggregate.MaterialSubmission;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.RejectMaterial;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.SubmitMaterialCommand;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;

@ServiceComponent(COMMAND_HANDLER)
public class MaterialHandler {
    private static final Logger LOGGER = getLogger(MaterialHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("stagingprosecutorscivil.command.submit-material")
    public void handleSubmitMaterial(final Envelope<SubmitMaterialCommand> command) throws EventStreamException {
        LOGGER.info("..........Received command to submit material with payload {}", command.payload());
        final SubmitMaterialCommand payload = command.payload();

        applyToAggregate(payload.getSubmissionId(), command, materialSubmission -> materialSubmission.submitMaterial(
                payload.getSubmissionId(),
                payload.getMaterialId(),
                payload.getCaseUrn(),
                payload.getProsecutingAuthority(),
                payload.getMaterialType(),
                ofNullable(payload.getDefendantId())));
    }

    @Handles("stagingprosecutorscivil.command.reject-material")
    public void handleReceiveMaterialSubmissionRejected(final Envelope<RejectMaterial> command) throws EventStreamException {
        final RejectMaterial payload = command.payload();
        applyToAggregate(
                payload.getSubmissionId(),
                command,
                materialSubmission -> materialSubmission.rejectMaterial(payload.getErrors(), payload.getWarnings()));
    }

    private void applyToAggregate(final UUID submissionId, final Envelope command, Function<MaterialSubmission, Stream<Object>> aggregateFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(submissionId);
        final MaterialSubmission materialSubmission = aggregateService.get(eventStream, MaterialSubmission.class);

        final Stream<Object> events = aggregateFunction.apply(materialSubmission);

        final JsonEnvelope jsonEnvelope = envelopeFrom(command.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
