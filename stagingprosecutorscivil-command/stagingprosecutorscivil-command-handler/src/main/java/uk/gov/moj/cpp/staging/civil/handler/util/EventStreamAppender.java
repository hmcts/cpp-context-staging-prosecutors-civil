package uk.gov.moj.cpp.staging.civil.handler.util;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;

import java.util.stream.Stream;

public class EventStreamAppender {

    private EventStreamAppender() {
    }

    public static void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(envelope)));
    }
}
