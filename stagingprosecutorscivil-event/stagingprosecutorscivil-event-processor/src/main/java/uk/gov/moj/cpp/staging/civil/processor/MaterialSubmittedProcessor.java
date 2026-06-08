package uk.gov.moj.cpp.staging.civil.processor;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.staging.civil.processor.util.ProsecutorCaseReferenceUtil.PROSECUTOR_CASE_PATTERN;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.civil.processor.exception.InvalidCaseUrnProvided;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class MaterialSubmittedProcessor {

    public static final String CASE_LEVEL = "Case level";
    public static final String DEFENDANT_LEVEL = "Defendant level";

    private  static final String CASE_MANAGEMENT_SECTION = "Case Management";
    private  static final String CASE_ID = "caseId";
    private  static final String APPLICATION_ID = "applicationId";

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Sender sender;

    @Handles("stagingprosecutorscivil.event.material-submitted")
    public void onMaterialSubmitted(final Envelope<MaterialSubmitted> materialSubmittedEnvelope) {

        final MaterialSubmitted materialSubmitted = materialSubmittedEnvelope.payload();

        final String prosecutorCaseReference = getProsecutorCaseReference(
                materialSubmitted.getProsecutingAuthority(),
                materialSubmitted.getCaseUrn());
        final UUID caseId = systemIdMapperService.getCppCaseIdFor(prosecutorCaseReference);

        final String submissionId = materialSubmitted.getSubmissionId().toString();

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add("material", createObjectBuilder()
                        .add("documentType", materialSubmitted.getMaterialType())
                        .add("fileStoreId", materialSubmitted.getMaterialId().toString())
                        .build()
                );

        ofNullable(materialSubmitted.getProsecutingAuthority()).ifPresent(prosecutingAuthority -> payloadBuilder.add("prosecutingAuthority", prosecutingAuthority));
        ofNullable(materialSubmitted.getDefendantId()).ifPresent(id -> payloadBuilder.add("prosecutorDefendantId", id));

        final Metadata metadata = metadataFrom(materialSubmittedEnvelope.metadata())
                .withName("prosecutioncasefile.add-material")
                .build();


        final Envelope<JsonObject> envelope = Envelope.envelopeFrom(metadata, payloadBuilder.build());

        sender.sendAsAdmin(envelope);
    }

    public static String getProsecutorCaseReference(final String prosecutingAuthority, final String caseUrn) {
        if (isNull(prosecutingAuthority)) {
            return caseUrn;
        }

        if (isNull(caseUrn)) {
            throw new InvalidCaseUrnProvided("please provide a valid caseUrn");
        }

        return format(PROSECUTOR_CASE_PATTERN, prosecutingAuthority, caseUrn);
    }

}
