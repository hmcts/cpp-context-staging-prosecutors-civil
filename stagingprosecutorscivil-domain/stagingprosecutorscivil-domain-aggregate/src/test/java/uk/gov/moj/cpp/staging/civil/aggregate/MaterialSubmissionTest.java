package uk.gov.moj.cpp.staging.civil.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MaterialSubmissionTest {

    private MaterialSubmission materialSubmission;

    @BeforeEach
    public void setUp() {
        materialSubmission = new MaterialSubmission();
    }

    @Test
    public void shouldRaiseSingleMaterialSubmittedEvent() {
        final List<Object> events = materialSubmission
                .submitMaterial(randomUUID(), randomUUID(), "T20217654", "THREE RIVER", "CCTV_FOOTAGE", empty())
                .collect(toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(MaterialSubmitted.class));
    }

    @Test
    public void shouldRaiseMaterialSubmittedEventWithAllFields() {
        final UUID submissionId = randomUUID();
        final UUID materialId = randomUUID();

        final List<Object> events = materialSubmission
                .submitMaterial(submissionId, materialId, "T20217654", "THREE RIVER", "CCTV_FOOTAGE", empty())
                .collect(toList());

        final MaterialSubmitted event = (MaterialSubmitted) events.get(0);
        assertThat(event.getSubmissionId(), is(submissionId));
        assertThat(event.getMaterialId(), is(materialId));
        assertThat(event.getCaseUrn(), is("T20217654"));
        assertThat(event.getProsecutingAuthority(), is("THREE RIVER"));
        assertThat(event.getMaterialType(), is("CCTV_FOOTAGE"));
        assertThat(event.getSubmissionStatus(), is(SubmissionStatus.PENDING));
    }

    @Test
    public void shouldRaiseMaterialSubmittedEventWithDefendantIdWhenPresent() {
        final List<Object> events = materialSubmission
                .submitMaterial(randomUUID(), randomUUID(), "T20217654", "THREE RIVER", "CCTV_FOOTAGE", of("defendant-123"))
                .collect(toList());

        final MaterialSubmitted event = (MaterialSubmitted) events.get(0);
        assertThat(event.getDefendantId(), is("defendant-123"));
    }

    @Test
    public void shouldRaiseMaterialSubmittedEventWithNullDefendantIdWhenAbsent() {
        final List<Object> events = materialSubmission
                .submitMaterial(randomUUID(), randomUUID(), "T20217654", "THREE RIVER", "CCTV_FOOTAGE", empty())
                .collect(toList());

        final MaterialSubmitted event = (MaterialSubmitted) events.get(0);
        assertThat(event.getDefendantId(), is(nullValue()));
    }

    @Test
    public void shouldRaiseSingleMaterialSubmissionRejectedEvent() {
        final UUID submissionId = randomUUID();
        submitMaterial(submissionId);

        final List<Object> events = materialSubmission.rejectMaterial(emptyList(), emptyList()).collect(toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(MaterialSubmissionRejected.class));
    }

    @Test
    public void shouldRaiseMaterialSubmissionRejectedEventWithCorrectSubmissionId() {
        final UUID submissionId = randomUUID();
        submitMaterial(submissionId);

        final List<Object> events = materialSubmission.rejectMaterial(emptyList(), emptyList()).collect(toList());

        final MaterialSubmissionRejected event = (MaterialSubmissionRejected) events.get(0);
        assertThat(event.getSubmissionId(), is(submissionId));
    }

    @Test
    public void shouldRaiseMaterialSubmissionRejectedEventWithErrors() {
        final UUID submissionId = randomUUID();
        submitMaterial(submissionId);
        final List<Problem> errors = singletonList(Problem.problem().withCode("ERR001").withValues(emptyList()).build());

        final List<Object> events = materialSubmission.rejectMaterial(errors, emptyList()).collect(toList());

        final MaterialSubmissionRejected event = (MaterialSubmissionRejected) events.get(0);
        assertThat(event.getErrors(), is(errors));
        assertThat(event.getWarnings(), is(emptyList()));
    }

    @Test
    public void shouldRaiseMaterialSubmissionRejectedEventWithWarnings() {
        final UUID submissionId = randomUUID();
        submitMaterial(submissionId);
        final List<Problem> warnings = singletonList(Problem.problem().withCode("WARN001").withValues(emptyList()).build());

        final List<Object> events = materialSubmission.rejectMaterial(emptyList(), warnings).collect(toList());

        final MaterialSubmissionRejected event = (MaterialSubmissionRejected) events.get(0);
        assertThat(event.getErrors(), is(emptyList()));
        assertThat(event.getWarnings(), is(warnings));
    }

    @Test
    public void shouldRaiseMaterialSubmissionRejectedEventWithErrorsAndWarnings() {
        final UUID submissionId = randomUUID();
        submitMaterial(submissionId);
        final List<Problem> errors = singletonList(Problem.problem().withCode("ERR001").withValues(emptyList()).build());
        final List<Problem> warnings = singletonList(Problem.problem().withCode("WARN001").withValues(emptyList()).build());

        final List<Object> events = materialSubmission.rejectMaterial(errors, warnings).collect(toList());

        final MaterialSubmissionRejected event = (MaterialSubmissionRejected) events.get(0);
        assertThat(event.getErrors(), is(errors));
        assertThat(event.getWarnings(), is(warnings));
    }

    private void submitMaterial(final UUID submissionId) {
        materialSubmission.submitMaterial(submissionId, randomUUID(), "T20217654", "THREE RIVER", "CCTV_FOOTAGE", empty())
                .collect(toList());
    }
}
