package uk.gov.moj.cpp.staging.civil.aggregate;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.UpdateCivilCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;

@SuppressWarnings("squid:S1068")
public class ProsecutionSubmissionAggregate implements Aggregate {
    private static final Logger LOGGER = getLogger(ProsecutionSubmissionAggregate.class);
    private static final long serialVersionUID = 6717379765262641757L;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                otherwiseDoNothing()
        );
    }

    public Stream<Object> receiveChargeProsecution(final UUID submissionId,
                                                   final HearingDetails hearingDetails,
                                                   final String prosecutingAuthority,
                                                   final List<ProsecutionCase> prosecutionCases) {
        LOGGER.info("Raising private event stagingprosecutorscivil.event.charge-prosecution-received for submission id {}", submissionId);
        return apply(
                Stream.of(
                        ChargeProsecutionReceived.chargeProsecutionReceived()
                                .withSubmissionId(submissionId)
                                .withSubmissionStatus(SubmissionStatus.PENDING)
                                .withProsecutingAuthority(prosecutingAuthority)
                                .withHearingDetails(hearingDetails)
                                .withProsecutionCases(prosecutionCases)
                                .build()
                )
        );
    }

    public Stream<Object> receiveSummonsProsecution(final UUID submissionId,
                                                    final HearingDetails hearingDetails,
                                                    final String prosecutingAuthority,
                                                    final List<ProsecutionCase> prosecutionCases) {
        LOGGER.info("Raising private event stagingprosecutorscivil.event.summons-prosecution-received for submission id {}", submissionId);
        return apply(
                Stream.of(
                        SummonsProsecutionReceived.summonsProsecutionReceived()
                                .withSubmissionId(submissionId)
                                .withSubmissionStatus(SubmissionStatus.PENDING)
                                .withProsecutingAuthority(prosecutingAuthority)
                                .withHearingDetails(hearingDetails)
                                .withProsecutionCases(prosecutionCases)
                                .build()
                )
        );
    }

    public Stream<Object> receiveCivilCaseUpdate(final UUID submissionId, final String submissionStatus, final List<Problem> caseErrors,
                                                 final List<DefendantProblem> defendantErrors, final List<Problem> groupCaseErrors,
                                                 final List<Problem> warnings, final List<Problem> caseWarnings, final List<DefendantProblem> defendantWarnings) {
        LOGGER.info("Raising private event stagingprosecutorscivil.event.update-civil-case-received for submission id {} and status {}", submissionId, submissionStatus);
        return apply(
                Stream.of(
                        UpdateCivilCaseReceived.updateCivilCaseReceived()
                                .withSubmissionId(submissionId)
                                .withSubmissionStatus(SubmissionStatus.valueOf(submissionStatus))
                                .withCaseErrors(caseErrors)
                                .withDefendantErrors(defendantErrors)
                                .withGroupCaseErrors(groupCaseErrors)
                                .withWarnings(warnings)
                                .withCaseWarnings(caseWarnings)
                                .withDefendantWarnings(defendantWarnings)
                                .build()
                )
        );
    }
}
