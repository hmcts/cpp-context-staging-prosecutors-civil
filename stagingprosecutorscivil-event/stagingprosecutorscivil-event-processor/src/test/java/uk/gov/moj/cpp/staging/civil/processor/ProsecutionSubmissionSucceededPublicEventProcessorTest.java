package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.updateCivilProsecutionSubmissionSucceeded;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CivilProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionSubmissionSucceededPublicEventProcessorTest {

    private static final String PROSECUTOR_CASE_REFERENCE = "URN-CIVIL-1";
    private static final String CASE_WARNING_CODE = "CASE_HEARING_DATE_IN_PAST";

    @InjectMocks
    private ProsecutionSubmissionSucceededPublicEventProcessor prosecutionSubmissionSucceededPublicEventProcessor;

    @Mock
    private Response response;

    @Mock
    private Sender sender;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldHandleProsecutionSubmissionSucceeded() {
        final CivilProsecutionSubmissionSucceeded prosecutionSubmissionSucceeded = updateCivilProsecutionSubmissionSucceeded();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<CivilProsecutionSubmissionSucceeded> envelope = testEnvelope(
                prosecutionSubmissionSucceeded,
                "public.prosecutioncasefile.civil-prosecution-submission-succeeded",
                eventCreatedTime);

        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceeded(envelope);

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();
        assertThat(payload.getString("submissionId"), is(prosecutionSubmissionSucceeded.getExternalId().toString()));
        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.name()));
    }

    @Test
    public void shouldOmitWarningFieldsFromCommandWhenAllNull() {
        final ProsecutionSubmissionSucceededWithWarnings event =
                ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings()
                        .withExternalId(randomUUID())
                        .withChannel(Channel.CIVIL)
                        .build();

        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceededWithWarnings(
                testEnvelope(event, "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings",
                        PAST_UTC_DATE_TIME.next()));

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS_WITH_WARNINGS.name()));
        assertThat(payload.containsKey("warnings"), is(false));
        assertThat(payload.containsKey("caseWarnings"), is(false));
        assertThat(payload.containsKey("defendantWarnings"), is(false));
    }

    @Test
    public void shouldIncludeWarningFieldsInCommandWhenNonNull() {
        final ProsecutionSubmissionSucceededWithWarnings event =
                ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings()
                        .withExternalId(randomUUID())
                        .withChannel(Channel.CIVIL)
                        .withWarnings(new ArrayList<>())
                        .withCivilCaseWarnings(new ArrayList<>())
                        .withDefendantWarnings(new ArrayList<>())
                        .build();

        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceededWithWarnings(
                testEnvelope(event, "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings",
                        PAST_UTC_DATE_TIME.next()));

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS_WITH_WARNINGS.name()));
        assertThat(payload.containsKey("warnings"), is(true));
        assertThat(payload.containsKey("caseWarnings"), is(true));
        assertThat(payload.containsKey("defendantWarnings"), is(true));

        // real JSON arrays, not JSON strings — the command schema declares these as type: array
        assertThat(payload.get("warnings").getValueType(), is(JsonValue.ValueType.ARRAY));
        assertThat(payload.get("caseWarnings").getValueType(), is(JsonValue.ValueType.ARRAY));
        assertThat(payload.get("defendantWarnings").getValueType(), is(JsonValue.ValueType.ARRAY));
    }

    @Test
    public void shouldSendCaseWarningsAsCaseProblemShapedJsonArraySourcedFromCivilCaseWarnings() {
        final Problem problem = Problem.problem().withCode(CASE_WARNING_CODE).build();
        final CaseProblem caseProblem = CaseProblem.caseProblem()
                .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                .withProblems(singletonList(problem))
                .build();

        final JsonObject caseProblemJson = Json.createObjectBuilder()
                .add("prosecutorCaseReference", PROSECUTOR_CASE_REFERENCE)
                .add("problems", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("code", CASE_WARNING_CODE)))
                .build();
        when(objectToJsonObjectConverter.convert(caseProblem)).thenReturn(caseProblemJson);

        final ProsecutionSubmissionSucceededWithWarnings event =
                ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings()
                        .withExternalId(randomUUID())
                        .withChannel(Channel.CIVIL)
                        .withCivilCaseWarnings(singletonList(caseProblem))
                        .build();

        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceededWithWarnings(
                testEnvelope(event, "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings",
                        PAST_UTC_DATE_TIME.next()));

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(payload.get("caseWarnings").getValueType(), is(JsonValue.ValueType.ARRAY));
        assertThat(payload.getJsonArray("caseWarnings").size(), is(1));
        assertThat(payload.getJsonArray("caseWarnings").getJsonObject(0), is(caseProblemJson));
    }

    @Test
    public void shouldIgnoreTheDeadCaseWarningsFieldOnThePublicEvent() {
        // the event's legacy flat caseWarnings (Problem[]) is never populated by the producer:
        // the case-level list must come from civilCaseWarnings only
        final ProsecutionSubmissionSucceededWithWarnings event =
                ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings()
                        .withExternalId(randomUUID())
                        .withChannel(Channel.CIVIL)
                        .withCaseWarnings(singletonList(Problem.problem().withCode("IGNORED").build()))
                        .build();

        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceededWithWarnings(
                testEnvelope(event, "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings",
                        PAST_UTC_DATE_TIME.next()));

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(payload.containsKey("caseWarnings"), is(false));
    }

    @Test
    public void shouldIncludeOnlyNonNullWarningFieldsInCommand() {
        final ProsecutionSubmissionSucceededWithWarnings event =
                ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings()
                        .withExternalId(randomUUID())
                        .withChannel(Channel.CIVIL)
                        .withWarnings(new ArrayList<>())
                        .build();

        prosecutionSubmissionSucceededPublicEventProcessor.prosecutionSubmissionSucceededWithWarnings(
                testEnvelope(event, "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings",
                        PAST_UTC_DATE_TIME.next()));

        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(payload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS_WITH_WARNINGS.name()));
        assertThat(payload.containsKey("warnings"), is(true));
        assertThat(payload.containsKey("caseWarnings"), is(false));
        assertThat(payload.containsKey("defendantWarnings"), is(false));
    }

    private <T> Envelope<T> testEnvelope(final T payload, final String eventName, final ZonedDateTime createdAt) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(createdAt);

        return Envelope.envelopeFrom(metadataFrom(createObjectBuilder(
                metadataBuilder.build().asJsonObject()).build())
                .withUserId(randomUUID().toString()).build(), payload);
    }
}
