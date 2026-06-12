package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class MaterialSubmittedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialSubmittedProcessor.class);

    private  static final String CASE_ID = "caseId";

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Sender sender;

    @Handles("stagingprosecutorscivil.event.material-submitted")
    public void onMaterialSubmitted(final Envelope<MaterialSubmitted> materialSubmittedEnvelope) {
        LOGGER.info("Received material submitted event with payload {}", materialSubmittedEnvelope.payload());

        final MaterialSubmitted materialSubmitted = materialSubmittedEnvelope.payload();

        final UUID caseId = systemIdMapperService.getCppCaseIdFor(materialSubmitted.getCaseUrn());

        LOGGER.info("----------Mapped prosecutorCaseReference {} to caseId {}", materialSubmitted.getCaseUrn(), caseId);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("material", createObjectBuilder()
                        .add("documentType", materialSubmitted.getMaterialType())
                        .add("fileStoreId", materialSubmitted.getMaterialId().toString())
                        .build()
                );

        ofNullable(caseId).ifPresent(c -> payloadBuilder.add(CASE_ID, caseId.toString()));
        ofNullable(materialSubmitted.getProsecutingAuthority()).ifPresent(prosecutingAuthority -> payloadBuilder.add("prosecutingAuthority", prosecutingAuthority));
        ofNullable(materialSubmitted.getDefendantId()).ifPresent(id -> payloadBuilder.add("prosecutorDefendantId", id));

        final Metadata metadata = metadataFrom(materialSubmittedEnvelope.metadata())
                .withName("prosecutioncasefile.add-material")
                .build();

        final Envelope<JsonObject> envelope = Envelope.envelopeFrom(metadata, payloadBuilder.build());
        sender.sendAsAdmin(envelope);
    }
}
