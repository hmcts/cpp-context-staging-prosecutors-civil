package uk.gov.moj.cpp.staging.civil.query;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.repository.SubmissionRepository;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilProsecutionQueryViewTest {

    @InjectMocks
    private CivilProsecutionQueryView civilProsecutionQueryView;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private SubmissionRepository submissionRepository;

    @Test
    public void shouldReturnSubmissionDetails() {

        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final UUID submissionId = UUID.randomUUID();

        final Set<CaseDetail> caseDetails = new HashSet<>();
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(UUID.randomUUID());
        caseDetail.setCaseUrn("CASEURN");
        caseDetails.add(caseDetail);

        final JsonArray errors = createArrayBuilder().add("error1").build();
        final JsonArray warnings = createArrayBuilder().add("warning1").build();
        final JsonArray caseWarnings = createArrayBuilder().add("casewarning1").build();
        final JsonArray defendantWarnings = createArrayBuilder().add("defendantWarning1").build();

        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "ouCode",
                errors,
                warnings,
                caseWarnings,
                defendantWarnings,
                receivedAt,
                completedAt,
                caseDetails);

        when(submissionRepository.findBy(submissionId)).thenReturn(submission);

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutorscivil.query.submission-details", payload);

        final JsonEnvelope jsonEnvelope = civilProsecutionQueryView.querySubmission(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), Is.is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), Is.is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), Is.is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("errors"), Is.is(errors));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("warnings"), Is.is(warnings));

    }

    @Test
    public void shouldReturnNullPayloadWhenSubmissionNotFound() {
        when(submissionRepository.findBy(any())).thenReturn(null);

        final JsonEnvelope responseEnvelope = civilProsecutionQueryView
                .querySubmission(createEnvelope("stagingprosecutorscivil.query.submission-details",
                        createObjectBuilder()
                                .add("submissionId", UUID.randomUUID().toString())
                                .build())
                );

        assertThat(responseEnvelope.metadata().name(), Is.is("stagingprosecutorscivil.query.submission-details"));
        assertEquals(JsonValue.NULL, responseEnvelope.payload());
    }
}