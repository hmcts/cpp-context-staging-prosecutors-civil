package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OthersProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OthersProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecutionWithSubmissionId;

import java.util.UUID;

import javax.inject.Inject;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class CivilProsecutionApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(CivilProsecutionApi.class);
    @Inject
    @Value(key = "stagingprosecutorscivil.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";

    private final Sender sender;

    @Inject
    public CivilProsecutionApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles("stagingprosecutorscivil.others-prosecution")
    public Envelope<UrlResponse> othersProsecution(final Envelope<OthersProsecution> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final OthersProsecution othersProsecution = envelope.payload();

        final OthersProsecutionWithSubmissionId othersProsecutionWithSubmissionId
                = OthersProsecutionWithSubmissionId.othersProsecutionWithSubmissionId()
                .withProsecutionCases(othersProsecution.getProsecutionCases())
                .withProsecutingAuthority(othersProsecution.getProsecutingAuthority())
                .withHearingDetails(othersProsecution.getHearingDetails())
                .withHearingDateRangeDetails(othersProsecution.getHearingDateRangeDetails())
                .withRelatedReferenceNumber(othersProsecution.getRelatedReferenceNumber())
                .withSubmissionId(submissionId)
                .build();

        LOGGER.info("Received submission at stagingprosecutorscivil.others-prosecution with submissionId {}",submissionId);
        sender.send(envelop(othersProsecutionWithSubmissionId)
                .withName("stagingprosecutorscivil.command.others-prosecution")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                .withSubmissionId(submissionId).build());


    }

    @Handles("stagingprosecutorscivil.summons-prosecution")
    public Envelope<UrlResponse> summonsProsecution(final Envelope<SummonsProsecution> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final SummonsProsecution summonsProsecution = envelope.payload();
        final SummonsProsecutionWithSubmissionId summonsProsecutionWithSubmissionId
                = SummonsProsecutionWithSubmissionId.summonsProsecutionWithSubmissionId()
                .withProsecutionCases(summonsProsecution.getProsecutionCases())
                .withProsecutingAuthority(summonsProsecution.getProsecutingAuthority())
                .withHearingDetails(summonsProsecution.getHearingDetails())
                .withSubmissionId(submissionId)
                .build();
        LOGGER.info("Received submission at  stagingprosecutorscivil.summons-prosecution with submissionId {}",submissionId);
        sender.send(envelop(summonsProsecutionWithSubmissionId)
                .withName("stagingprosecutorscivil.command.summons-prosecution")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                        .withSubmissionId(submissionId).build());

    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }

}
