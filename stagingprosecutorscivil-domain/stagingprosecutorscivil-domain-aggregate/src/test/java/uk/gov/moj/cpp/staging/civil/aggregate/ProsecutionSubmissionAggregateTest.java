package uk.gov.moj.cpp.staging.civil.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDateRangeDetails;
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
    public void shouldRaiseChargeProsecutionReceivedEvent() {
        final UUID submissionId = UUID.randomUUID();
        final HearingDetails hearingDetails = HearingDetails.hearingDetails()
                .withDateOfHearing(LocalDate.now())
                .withTimeOfHearing("10:00:00")
                .withCourtHearingLocation("COURT1")
                .build();
        final List<ProsecutionCase> cases = Collections.singletonList(ProsecutionCase.prosecutionCase()
                .withUrn("URN123")
                .build());

        final Stream<Object> events = aggregate.receiveChargeProsecution(
                submissionId, hearingDetails, null, "GAAAA01", cases, null);

        final List<Object> eventList = events.collect(Collectors.toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(ChargeProsecutionReceived.class));
        final ChargeProsecutionReceived event = (ChargeProsecutionReceived) eventList.get(0);
        assertThat(event.getSubmissionId(), is(submissionId));
        assertThat(event.getProsecutingAuthority(), is("GAAAA01"));
        assertThat(event.getSubmissionStatus(), is(SubmissionStatus.PENDING));
        assertThat(event.getRelatedReferenceNumber(), is(nullValue()));
    }

    @Test
    public void shouldRaiseChargeProsecutionReceivedEventWithRelatedReferenceNumber() {
        final UUID submissionId = UUID.randomUUID();
        final HearingDateRangeDetails hearingDateRangeDetails = HearingDateRangeDetails.hearingDateRangeDetails()
                .withStartDateRangeOfHearing(LocalDate.of(2026, 3, 12))
                .withEndDateRangeOfHearing(LocalDate.of(2026, 3, 14))
                .withCourtHearingLocation("B01LY01")
                .build();
        final List<ProsecutionCase> cases = Collections.singletonList(ProsecutionCase.prosecutionCase()
                .withUrn("URN-ENF-001")
                .build());

        final Stream<Object> events = aggregate.receiveChargeProsecution(
                submissionId, null, hearingDateRangeDetails, "GAAAA01", cases, "GOB123456789");

        final List<Object> eventList = events.collect(Collectors.toList());
        assertThat(eventList.size(), is(1));
        final ChargeProsecutionReceived event = (ChargeProsecutionReceived) eventList.get(0);
        assertThat(event.getSubmissionId(), is(submissionId));
        assertThat(event.getRelatedReferenceNumber(), is("GOB123456789"));
        assertThat(event.getSubmissionStatus(), is(SubmissionStatus.PENDING));
        assertThat(event.getHearingDateRangeDetails().getStartDateRangeOfHearing(), is(notNullValue()));
        assertThat(event.getHearingDateRangeDetails().getCourtHearingLocation(), is("B01LY01"));
    }

    @Test
    public void shouldRaiseSummonsProsecutionReceivedEvent() {
        final UUID submissionId = UUID.randomUUID();
        final HearingDetails hearingDetails = HearingDetails.hearingDetails()
                .withDateOfHearing(LocalDate.now())
                .withTimeOfHearing("10:00:00")
                .withCourtHearingLocation("COURT2")
                .build();
        final List<ProsecutionCase> cases = Collections.singletonList(ProsecutionCase.prosecutionCase()
                .withUrn("URN456")
                .build());

        final Stream<Object> events = aggregate.receiveSummonsProsecution(
                submissionId, hearingDetails, "GAAAA01", cases);

        final List<Object> eventList = events.collect(Collectors.toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(SummonsProsecutionReceived.class));
        final SummonsProsecutionReceived event = (SummonsProsecutionReceived) eventList.get(0);
        assertThat(event.getSubmissionId(), is(submissionId));
        assertThat(event.getSubmissionStatus(), is(SubmissionStatus.PENDING));
    }
}
