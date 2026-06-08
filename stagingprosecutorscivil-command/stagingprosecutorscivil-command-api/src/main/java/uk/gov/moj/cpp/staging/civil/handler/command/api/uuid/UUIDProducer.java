package uk.gov.moj.cpp.staging.civil.handler.command.api.uuid;

import java.util.UUID;

@FunctionalInterface
public interface UUIDProducer {
    UUID generateUUID();
}
