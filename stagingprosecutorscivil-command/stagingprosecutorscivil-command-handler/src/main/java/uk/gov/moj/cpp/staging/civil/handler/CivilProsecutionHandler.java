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
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.OtherCase;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.Summons;
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

    @Handles("stagingcivil.command.other-case")
    public void handleOtherCase(final Envelope<OtherCase> envelope) throws EventStreamException {
        LOGGER.info("stagingcivil.command.other-case with SubmissionId {}", envelope.payload().getSubmissionId());

        final OtherCase otherCase = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(otherCase.getSubmissionId());
        final ProsecutionSubmissionAggregate aggregate = aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class);
        final Stream<Object> events = aggregate.receiveOtherCase(otherCase.getSubmissionId(), otherCase.getHearingDetails(), otherCase.getHearingDateRangeDetails(), otherCase.getProsecutingAuthority(), otherCase.getProsecutionCases(), otherCase.getRelatedReferenceNumber());

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("stagingcivil.command.summons")
    public void handleSummons(final Envelope<Summons> envelope) throws EventStreamException {
        LOGGER.info("stagingcivil.command.summons with SubmissionId {}", envelope.payload().getSubmissionId());

        final Summons summons = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(summons.getSubmissionId());
        final ProsecutionSubmissionAggregate aggregate = aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class);
        final Stream<Object> events = aggregate.receiveSummons(summons.getSubmissionId(), summons.getHearingDetails(), summons.getProsecutingAuthority(), summons.getProsecutionCases());

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("stagingcivil.command.update-civil-case")
    public void handleCivilCaseUpdate(final Envelope<UpdateCivilCase> envelope) throws EventStreamException {
        LOGGER.info("stagingcivil.command.update-civil-case with SubmissionId {} and status {}", envelope.payload().getSubmissionId(), envelope.payload().getSubmissionStatus());

        final UpdateCivilCase update = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(update.getSubmissionId());
        final ProsecutionSubmissionAggregate aggregate = aggregateService.get(eventStream, ProsecutionSubmissionAggregate.class);
        final Stream<Object> events = aggregate.receiveCivilCaseUpdate(update.getSubmissionId(), update.getSubmissionStatus(), update.getCaseErrors(),
                update.getDefendantErrors(), update.getGroupCaseErrors(), update.getWarnings(), update.getCaseWarnings(), update.getDefendantWarnings());
        appendEventsToStream(envelope, eventStream, events);
    }

}
