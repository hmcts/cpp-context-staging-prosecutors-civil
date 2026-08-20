package uk.gov.moj.cpp.staging.civil.handler.command.api.client;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Reusable cross-context client for {@code referencedata.query.get.prosecutor.by.oucode} -
 * mirrors {@code cpp-context-results}' {@code ReferenceDataService} convention exactly: build the
 * request via {@code JsonEnvelope.envelopeFrom} (not {@code Envelope.envelopeFrom}) and dispatch
 * via {@code Requester.requestAsAdmin(envelope, Class)}, not the plain (per-user) {@code request}
 * overload - reference data is a system-level lookup, not scoped to the calling user's own
 * identity. Deliberately not an {@code @Adapter} class - see {@link UserGroupsClient}'s Javadoc
 * for why (CAD-1613, KB workspace ADR-002).
 */
public class ReferenceDataClient {

    private static final String REFERENCEDATA_GET_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String OUCODE_FIELD = "oucode";
    private static final String SHORT_NAME_FIELD = "shortName";

    @Inject
    @ServiceComponent(Component.COMMAND_API)
    private Requester requester;

    public String getProsecutorShortNameForOuCode(final String ouCode) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_GET_PROSECUTOR_BY_OUCODE)
                .build();
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add(OUCODE_FIELD, ouCode)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadata, queryPayload);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload().getString(SHORT_NAME_FIELD, null);
    }
}
