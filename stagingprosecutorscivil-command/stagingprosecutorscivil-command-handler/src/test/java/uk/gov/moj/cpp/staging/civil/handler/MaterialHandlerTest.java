package uk.gov.moj.cpp.staging.civil.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.civil.aggregate.MaterialSubmission;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.ReceiveMaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.RejectMaterial;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.handler.SubmitMaterialCommand;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialHandlerTest {

    private static final String PRIVATE_COMMAND_SUBMIT_MATERIAL = "stagingprosecutorscivil.command.submit-material";
    private static final String PRIVATE_EVENT_MATERIAL_SUBMITTED = "stagingprosecutorscivil.event.material-submitted";
    private static final String PRIVATE_COMMAND_REJECT_MATERIAL = "stagingprosecutorscivil.command.reject-material";
    private static final String PRIVATE_COMMAND_RECEIVE_MATERIAL_SUBMISSION_SUCCESSFUL = "stagingprosecutorscivil.command.receive-material-submission-successful";
    private static final String PRIVATE_EVENT_MATERIAL_SUBMISSION_SUCCESSFUL = "stagingprosecutorscivil.event.material-submission-successful";

    @InjectMocks
    private MaterialHandler materialHandler;

    private static final UUID USER_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(MaterialSubmitted.class, MaterialSubmissionSuccessful.class);

    @Test
    public void shouldHandleSubmitMaterialCommand() {

        assertThat(materialHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleSubmitMaterial")
                        .thatHandles(PRIVATE_COMMAND_SUBMIT_MATERIAL)));

    }

    @Test
    public void shouldRaiseMaterialSubmittedPrivateEvent() throws Exception {

        final UUID submissionId = randomUUID();
        final Envelope<SubmitMaterialCommand> envelope = buildSubmitMaterialEnvelope(submissionId, null);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(new MaterialSubmission());

        materialHandler.handleSubmitMaterial(envelope);

        verifyMaterialSubmittedPrivateEvent(null);

    }

    @Test
    public void shouldRaiseMaterialSubmittedPrivateEventWithDefendantId() throws Exception {

        final UUID submissionId = randomUUID();
        final Envelope<SubmitMaterialCommand> envelope = buildSubmitMaterialEnvelope(submissionId, "defendant-123");
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(new MaterialSubmission());

        materialHandler.handleSubmitMaterial(envelope);

        verifyMaterialSubmittedPrivateEvent("defendant-123");

    }

    private void verifyMaterialSubmittedPrivateEvent(final String expectedDefendantId) throws EventStreamException {

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        if (expectedDefendantId != null) {
            assertThat(envelopeStream, streamContaining(
                    jsonEnvelope(
                            metadata()
                                    .withName(PRIVATE_EVENT_MATERIAL_SUBMITTED),
                            payload().isJson(allOf(
                                    withJsonPath("$.submissionId", notNullValue()),
                                    withJsonPath("$.caseUrn", is("T20217654")),
                                    withJsonPath("$.prosecutingAuthority", is("THREE RIVER")),
                                    withJsonPath("$.materialType", is("CCTV_FOOTAGE")),
                                    withJsonPath("$.submissionStatus", is(SubmissionStatus.PENDING.name())),
                                    withJsonPath("$.defendantId", is(expectedDefendantId))))
                    ))
            );
        } else {
            assertThat(envelopeStream, streamContaining(
                    jsonEnvelope(
                            metadata()
                                    .withName(PRIVATE_EVENT_MATERIAL_SUBMITTED),
                            payload().isJson(allOf(
                                    withJsonPath("$.submissionId", notNullValue()),
                                    withJsonPath("$.caseUrn", is("T20217654")),
                                    withJsonPath("$.prosecutingAuthority", is("THREE RIVER")),
                                    withJsonPath("$.materialType", is("CCTV_FOOTAGE")),
                                    withJsonPath("$.submissionStatus", is(SubmissionStatus.PENDING.name()))))
                    ))
            );
        }

    }

    @Test
    public void shouldHandleRejectMaterialCommand() {

        assertThat(materialHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleReceiveMaterialSubmissionRejected")
                        .thatHandles(PRIVATE_COMMAND_REJECT_MATERIAL)));

    }

    @Test
    public void shouldCallRejectMaterialOnAggregateWithErrorsAndWarnings() throws Exception {

        final UUID submissionId = randomUUID();
        final MaterialSubmission materialSubmission = mock(MaterialSubmission.class);
        when(materialSubmission.rejectMaterial(any(), any())).thenReturn(Stream.empty());
        when(eventSource.getStreamById(submissionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(materialSubmission);

        final Envelope<RejectMaterial> envelope = buildRejectMaterialEnvelope(submissionId);
        materialHandler.handleReceiveMaterialSubmissionRejected(envelope);

        verify(materialSubmission).rejectMaterial(emptyList(), emptyList());

    }

    private Envelope<RejectMaterial> buildRejectMaterialEnvelope(final UUID submissionId) {

        final RejectMaterial rejectMaterial = RejectMaterial.rejectMaterial()
                .withSubmissionId(submissionId)
                .withErrors(emptyList())
                .withWarnings(emptyList())
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString())
                        .withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(rejectMaterial)
                .withName(PRIVATE_COMMAND_REJECT_MATERIAL)
                .withMetadataFrom(requestEnvelope);

    }

    @Test
    public void shouldHandleReceiveMaterialSubmissionSuccessfulCommand() {

        assertThat(materialHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleReceiveMaterial")
                        .thatHandles(PRIVATE_COMMAND_RECEIVE_MATERIAL_SUBMISSION_SUCCESSFUL)));

    }

    @Test
    public void shouldRaiseMaterialSubmissionSuccessfulPrivateEvent() throws Exception {

        final UUID submissionId = randomUUID();
        final Envelope<ReceiveMaterialSubmissionSuccessful> envelope = buildReceiveMaterialSubmissionSuccessfulEnvelope(submissionId);
        when(eventSource.getStreamById(submissionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(new MaterialSubmission());

        materialHandler.handleReceiveMaterial(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName(PRIVATE_EVENT_MATERIAL_SUBMISSION_SUCCESSFUL),
                        payload().isJson(withJsonPath("$.submissionId", notNullValue())))));

    }

    private Envelope<ReceiveMaterialSubmissionSuccessful> buildReceiveMaterialSubmissionSuccessfulEnvelope(final UUID submissionId) {

        final ReceiveMaterialSubmissionSuccessful command = ReceiveMaterialSubmissionSuccessful.receiveMaterialSubmissionSuccessful()
                .withSubmissionId(submissionId)
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString())
                        .withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(command)
                .withName(PRIVATE_COMMAND_RECEIVE_MATERIAL_SUBMISSION_SUCCESSFUL)
                .withMetadataFrom(requestEnvelope);

    }

    private Envelope<SubmitMaterialCommand> buildSubmitMaterialEnvelope(final UUID submissionId, final String defendantId) {

        final SubmitMaterialCommand submitMaterialCommand = SubmitMaterialCommand.submitMaterialCommand()
                .withSubmissionId(submissionId)
                .withMaterialId(randomUUID())
                .withCaseUrn("T20217654")
                .withProsecutingAuthority("THREE RIVER")
                .withMaterialType("CCTV_FOOTAGE")
                .withDefendantId(defendantId)
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString())
                        .withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(submitMaterialCommand)
                .withName(PRIVATE_COMMAND_SUBMIT_MATERIAL)
                .withMetadataFrom(requestEnvelope);

    }

}
