package uk.gov.moj.cpp.staging.civil.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutors.civil.event.OtherCaseReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProsecutionSubmissionAggregateTest {

    private ProsecutionSubmissionAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new ProsecutionSubmissionAggregate();
    }

    @Test
    public void shouldRaiseOtherCaseReceivedEvent() {
        final UUID submissionId = UUID.randomUUID();
        final HearingDetails hearingDetails = HearingDetails.hearingDetails()
                .withDateOfHearing(LocalDate.now())
                .withTimeOfHearing("10:00:00")
                .withCourtHearingLocation("COURT1")
                .build();
        final List<ProsecutionCase> cases = Collections.singletonList(ProsecutionCase.prosecutionCase()
                .withUrn("URN123")
                .build());

        final Stream<Object> events = aggregate.receiveOtherCase(
                submissionId, hearingDetails, "GAAAA01", cases);

        final List<Object> eventList = events.collect(Collectors.toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(OtherCaseReceived.class));
        final OtherCaseReceived event = (OtherCaseReceived) eventList.get(0);
        assertThat(event.getSubmissionId(), is(submissionId));
        assertThat(event.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(event.getSubmissionStatus(), is(SubmissionStatus.PENDING));
    }

    @Test
    public void shouldRaiseSummonsReceivedEvent() {
        final UUID submissionId = UUID.randomUUID();
        final HearingDetails hearingDetails = HearingDetails.hearingDetails()
                .withDateOfHearing(LocalDate.now())
                .withTimeOfHearing("10:00:00")
                .withCourtHearingLocation("COURT2")
                .build();
        final List<ProsecutionCase> cases = Collections.singletonList(ProsecutionCase.prosecutionCase()
                .withUrn("URN456")
                .build());

        final Stream<Object> events = aggregate.receiveSummons(
                submissionId, hearingDetails, "GAAAA01", cases);

        final List<Object> eventList = events.collect(Collectors.toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(SummonsReceived.class));
        final SummonsReceived event = (SummonsReceived) eventList.get(0);
        assertThat(event.getSubmissionId(), is(submissionId));
        assertThat(event.getSubmissionStatus(), is(SubmissionStatus.PENDING));
    }
}
