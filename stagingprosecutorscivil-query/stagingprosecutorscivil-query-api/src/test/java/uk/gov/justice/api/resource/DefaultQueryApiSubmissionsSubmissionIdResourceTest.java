package uk.gov.justice.api.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.parameter.Parameter;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilder;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;

import java.util.Collection;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiSubmissionsSubmissionIdResourceTest {

    @Mock
    private RestProcessor restProcessor;

    @Mock
    private ActionMapper actionMapper;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    private HttpHeaders headers;

    @Mock
    private ParameterCollectionBuilderFactory validParameterCollectionBuilderFactory;

    @Mock
    private ParameterCollectionBuilder parameterCollectionBuilder;

    @Mock
    private TraceLogger traceLogger;

    @Mock
    private HttpTraceLoggerHelper httpTraceLoggerHelper;

    @Mock
    private Response expectedResponse;

    @InjectMocks
    private DefaultQueryApiSubmissionsSubmissionIdResource resource;

    @BeforeEach
    public void setUp() {
        lenient().when(validParameterCollectionBuilderFactory.create()).thenReturn(parameterCollectionBuilder);
        lenient().when(parameterCollectionBuilder.parameters()).thenReturn((Collection<Parameter>) (Collection<?>) java.util.Collections.emptyList());
    }

    @Test
    public void shouldUseJsonResponseStrategyForJsonAction() {
        final String submissionId = UUID.randomUUID().toString();
        when(actionMapper.actionOf("getSubmissionsBySubmissionId", "GET", headers))
                .thenReturn("stagingprosecutorscivil.submission-details");
        when(restProcessor.process(eq("OkStatusEnvelopePayloadEntityResponseStrategy"), any(), eq("stagingprosecutorscivil.submission-details"), eq(headers), anyCollection()))
                .thenReturn(expectedResponse);

        final Response response = resource.getSubmissionsBySubmissionId(submissionId, "false");

        assertThat(response, is(expectedResponse));
        verify(restProcessor).process(eq("OkStatusEnvelopePayloadEntityResponseStrategy"), any(), eq("stagingprosecutorscivil.submission-details"), eq(headers), anyCollection());
    }

    @Test
    public void shouldUseCsvResponseStrategyForCsvAction() {
        final String submissionId = UUID.randomUUID().toString();
        when(actionMapper.actionOf("getSubmissionsBySubmissionId", "GET", headers))
                .thenReturn("stagingprosecutorscivil.submission-error-details");
        when(restProcessor.process(eq("SubmissionErrorDetailsCsvResponseStrategy"), any(), eq("stagingprosecutorscivil.submission-error-details"), eq(headers), anyCollection()))
                .thenReturn(expectedResponse);

        final Response response = resource.getSubmissionsBySubmissionId(submissionId, null);

        assertThat(response, is(expectedResponse));
        verify(restProcessor).process(eq("SubmissionErrorDetailsCsvResponseStrategy"), any(), eq("stagingprosecutorscivil.submission-error-details"), eq(headers), anyCollection());
    }
}
