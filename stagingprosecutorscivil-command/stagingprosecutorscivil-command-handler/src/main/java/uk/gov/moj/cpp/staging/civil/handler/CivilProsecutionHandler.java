package uk.gov.moj.cpp.staging.civil.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.staging.civil.handler.util.EventStreamAppender.appendEventsToStream;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.civil.aggregate.ProsecutionSubmissionAggregate;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.OthersProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.SummonsProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.UpdateCivilCase;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(COMMAND_HANDLER)
public class CivilProsecutionHandler {

    private static final Logger LOGGER = getLogger(CivilProsecutionHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("stagingprosecutorscivil.command.others-prosecution")
    public void handleOthersProsecution(final Envelope<OthersProsecution> envelope) throws EventStreamException {
        LOGGER.info("stagingprosecutorscivil.command.others-prosecution with SubmissionId {}", envelope.payload().getSubmissionId());

        final OthersProsecution othersProsecution = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(othersProsecution.getSubmissionId());
        final ProsecutionSubmissionAggregate aggregate = aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class);
        final Stream<Object> events = aggregate.receiveOthersProsecution(othersProsecution.getSubmissionId(), othersProsecution.getHearingDetails(), othersProsecution.getHearingDateRangeDetails(), othersProsecution.getProsecutingAuthority(), othersProsecution.getProsecutionCases(), othersProsecution.getRelatedReferenceNumber());

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("stagingprosecutorscivil.command.summons-prosecution")
    public void handleSummonsProsecution(final Envelope<SummonsProsecution> envelope) throws EventStreamException {
        LOGGER.info("stagingprosecutorscivil.command.summons-prosecution with SubmissionId {}", envelope.payload().getSubmissionId());

        final SummonsProsecution summonsProsecution = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(summonsProsecution.getSubmissionId());
        final ProsecutionSubmissionAggregate aggregate = aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class);
        final Stream<Object> events = aggregate.receiveSummonsProsecution(summonsProsecution.getSubmissionId(), summonsProsecution.getHearingDetails(), summonsProsecution.getProsecutingAuthority(), summonsProsecution.getProsecutionCases());

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("stagingprosecutorscivil.command.update-civil-case")
    public void handleCivilCaseUpdate(final Envelope<UpdateCivilCase> envelope) throws EventStreamException {
        LOGGER.info("stagingprosecutorscivil.command.update-civil-case with SubmissionId {} and status {}", envelope.payload().getSubmissionId(), envelope.payload().getSubmissionStatus());

        final UpdateCivilCase update = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(update.getSubmissionId());
        final ProsecutionSubmissionAggregate aggregate = aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class);
        final Stream<Object> events = aggregate.receiveCivilCaseUpdate(update.getSubmissionId(), update.getSubmissionStatus(), update.getCaseErrors(),
                update.getDefendantErrors(), update.getGroupCaseErrors(), update.getWarnings(), update.getCaseWarnings(), update.getDefendantWarnings());
        appendEventsToStream(envelope, eventStream, events);
    }

}
