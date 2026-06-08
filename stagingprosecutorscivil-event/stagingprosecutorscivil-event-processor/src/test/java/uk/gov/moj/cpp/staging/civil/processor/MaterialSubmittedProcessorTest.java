package uk.gov.moj.cpp.staging.civil.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.PENDING;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.staging.civil.processor.exception.InvalidCaseUrnProvided;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialSubmittedProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @InjectMocks
    private MaterialSubmittedProcessor target;

    @Test
    public void shouldHandleMaterialSubmittedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method("onMaterialSubmitted")
                        .thatHandles("stagingprosecutorscivil.event.material-submitted")
                ));
    }

    @Test
    public void shouldSendAddMaterialCommandWithProsecutingAuthorityAndDefendantId() {
        final UUID submissionId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "TVL123456";
        final String prosecutingAuthority = "GAAAA01";
        final String defendantId = "defendant-id-001";
        final String materialType = "CJSM";

        final MaterialSubmitted materialSubmitted = MaterialSubmitted.materialSubmitted()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority(prosecutingAuthority)
                .withCaseUrn(caseUrn)
                .withMaterialId(materialId)
                .withMaterialType(materialType)
                .withDefendantId(defendantId)
                .build();

        when(systemIdMapperService.getCppCaseIdFor(prosecutingAuthority + ":" + caseUrn)).thenReturn(caseId);

        target.onMaterialSubmitted(testEnvelope(materialSubmitted, "stagingprosecutorscivil.event.material-submitted", PAST_UTC_DATE_TIME.next()));

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        final JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(captor.getValue().metadata().name(), is("prosecutioncasefile.add-material"));
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getJsonObject("material").getString("documentType"), is(materialType));
        assertThat(payload.getJsonObject("material").getString("fileStoreId"), is(materialId.toString()));
        assertThat(payload.getString("prosecutingAuthority"), is(prosecutingAuthority));
        assertThat(payload.getString("prosecutorDefendantId"), is(defendantId));
    }

    @Test
    public void shouldSendAddMaterialCommandWithoutDefendantId() {
        final UUID submissionId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "TVL123456";
        final String prosecutingAuthority = "GAAAA01";
        final String materialType = "CJSM";

        final MaterialSubmitted materialSubmitted = MaterialSubmitted.materialSubmitted()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority(prosecutingAuthority)
                .withCaseUrn(caseUrn)
                .withMaterialId(materialId)
                .withMaterialType(materialType)
                .build();

        when(systemIdMapperService.getCppCaseIdFor(prosecutingAuthority + ":" + caseUrn)).thenReturn(caseId);

        target.onMaterialSubmitted(testEnvelope(materialSubmitted, "stagingprosecutorscivil.event.material-submitted", PAST_UTC_DATE_TIME.next()));

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        final JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.containsKey("prosecutorDefendantId"), is(false));
    }

    @Test
    public void shouldSendAddMaterialCommandUsingCaseUrnAsReferenceWhenProsecutingAuthorityIsNull() {
        final UUID submissionId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "TVL123456";
        final String materialType = "CJSM";

        final MaterialSubmitted materialSubmitted = MaterialSubmitted.materialSubmitted()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withCaseUrn(caseUrn)
                .withMaterialId(materialId)
                .withMaterialType(materialType)
                .build();

        when(systemIdMapperService.getCppCaseIdFor(caseUrn)).thenReturn(caseId);

        target.onMaterialSubmitted(testEnvelope(materialSubmitted, "stagingprosecutorscivil.event.material-submitted", PAST_UTC_DATE_TIME.next()));

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        final JsonObject payload = (JsonObject) captor.getValue().payload();

        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.containsKey("prosecutingAuthority"), is(false));
    }

    @Test
    public void shouldThrowExceptionWhenProsecutingAuthorityProvidedAndCaseUrnIsNull() {
        final MaterialSubmitted materialSubmitted = MaterialSubmitted.materialSubmitted()
                .withSubmissionId(randomUUID())
                .withSubmissionStatus(PENDING)
                .withProsecutingAuthority("GAAAA01")
                .withMaterialId(randomUUID())
                .withMaterialType("CJSM")
                .build();

        assertThrows(InvalidCaseUrnProvided.class,
                () -> target.onMaterialSubmitted(testEnvelope(materialSubmitted, "stagingprosecutorscivil.event.material-submitted", PAST_UTC_DATE_TIME.next())));
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
