package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCase;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.OtherCaseWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.Summons;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDateRangeDetails;

import java.time.LocalDate;
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

    @Handles("stagingcivil.other-case")
    public Envelope<UrlResponse> otherCase(final Envelope<OtherCase> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final OtherCase otherCase = envelope.payload();

        validateHearingDateRangeDetails(otherCase.getHearingDateRangeDetails());

        final OtherCaseWithSubmissionId otherCaseWithSubmissionId
                = OtherCaseWithSubmissionId.otherCaseWithSubmissionId()
                .withProsecutionCases(otherCase.getProsecutionCases())
                .withProsecutingAuthority(otherCase.getProsecutingAuthority())
                .withHearingDetails(otherCase.getHearingDetails())
                .withHearingDateRangeDetails(otherCase.getHearingDateRangeDetails())
                .withRelatedReferenceNumber(otherCase.getRelatedReferenceNumber())
                .withSubmissionId(submissionId)
                .build();

        LOGGER.info("Received submission at stagingcivil.other-case with submissionId {}",submissionId);
        sender.send(envelop(otherCaseWithSubmissionId)
                .withName("stagingcivil.command.other-case")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                .withSubmissionId(submissionId).build());


    }

    @Handles("stagingcivil.summons")
    public Envelope<UrlResponse> summons(final Envelope<Summons> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final Summons summons = envelope.payload();
        final SummonsWithSubmissionId summonsWithSubmissionId
                = SummonsWithSubmissionId.summonsWithSubmissionId()
                .withProsecutionCases(summons.getProsecutionCases())
                .withProsecutingAuthority(summons.getProsecutingAuthority())
                .withHearingDetails(summons.getHearingDetails())
                .withSubmissionId(submissionId)
                .build();
        LOGGER.info("Received submission at  stagingcivil.summons with submissionId {}",submissionId);
        sender.send(envelop(summonsWithSubmissionId)
                .withName("stagingcivil.command.summons")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                        .withSubmissionId(submissionId).build());

    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }

    @SuppressWarnings("squid:S1166")
    private void validateHearingDateRangeDetails(final HearingDateRangeDetails hearingDateRangeDetails) {
        if (hearingDateRangeDetails == null) {
            return;
        }

        final LocalDate startDateRangeOfHearing = hearingDateRangeDetails.getStartDateRangeOfHearing();
        final LocalDate endDateRangeOfHearing = hearingDateRangeDetails.getEndDateRangeOfHearing();

        if (endDateRangeOfHearing.isBefore(startDateRangeOfHearing)) {
            throw new BadRequestException(format(
                    "endDateRangeOfHearing %s must not be before startDateRangeOfHearing %s",
                    endDateRangeOfHearing, startDateRangeOfHearing));
        }
    }

}
