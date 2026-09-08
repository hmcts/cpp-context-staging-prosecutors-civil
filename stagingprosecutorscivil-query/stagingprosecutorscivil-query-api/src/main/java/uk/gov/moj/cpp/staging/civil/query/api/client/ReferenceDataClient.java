package uk.gov.moj.cpp.staging.civil.query.api.client;

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
 * Query-api-local counterpart of {@code uk.gov.moj.cpp.staging.civil.handler.command.api.client.ReferenceDataClient}
 * (command-api). Needed on the query side because {@code Submission.prosecutorShortName} is only
 * ever populated for complaints-CSV-uploaded summons submissions - regular charge-prosecution and
 * summons-prosecution submissions carry only {@code ouCode}, so authority validation on read must
 * be able to resolve a short name from that ou code, the same way the CSV upload path does at
 * write time.
 */
public class ReferenceDataClient {

    private static final String REFERENCEDATA_GET_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String OUCODE_FIELD = "oucode";
    private static final String SHORT_NAME_FIELD = "shortName";

    @Inject
    @ServiceComponent(Component.QUERY_API)
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
        final JsonObject payload = response.payload();
        return payload == null ? null : payload.getString(SHORT_NAME_FIELD, null);
    }
}
