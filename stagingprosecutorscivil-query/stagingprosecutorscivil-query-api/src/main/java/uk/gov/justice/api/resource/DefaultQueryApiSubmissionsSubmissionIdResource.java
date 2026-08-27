package uk.gov.justice.api.resource;

import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilder;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterType;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hand-written in place of the RAML-generated default: the generated resource always renders its
 * response via a single hardcoded "OkStatusEnvelopePayloadEntityResponseStrategy", which forces
 * the envelope payload to be serialised as JSON regardless of the resolved action's responseType.
 * That works for the JSON submission-details mapping but cannot produce raw text/csv bytes for the
 * submission-error-details-csv mapping, so this override picks the response strategy per resolved
 * action while still routing every request through the normal interceptor chain (access control
 * included) via {@link RestProcessor#process}.
 */
@Adapter(Component.QUERY_API)
public class DefaultQueryApiSubmissionsSubmissionIdResource implements QueryApiSubmissionsSubmissionIdResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiSubmissionsSubmissionIdResource.class);

    private static final String JSON_RESPONSE_STRATEGY = "OkStatusEnvelopePayloadEntityResponseStrategy";
    private static final String CSV_RESPONSE_STRATEGY = "SubmissionErrorDetailsCsvResponseStrategy";
    private static final String CSV_ACTION_NAME = "stagingprosecutorscivil.submission-error-details-csv";

    @Inject
    RestProcessor restProcessor;

    @Inject
    @Named("DefaultQueryApiSubmissionsSubmissionIdResourceActionMapper")
    ActionMapper actionMapper;

    @Inject
    InterceptorChainProcessor interceptorChainProcessor;

    @Context
    HttpHeaders headers;

    @Inject
    ParameterCollectionBuilderFactory validParameterCollectionBuilderFactory;

    @Inject
    TraceLogger traceLogger;

    @Inject
    HttpTraceLoggerHelper httpTraceLoggerHelper;

    @Override
    public Response getSubmissionsBySubmissionId(final String submissionId, final String additionalInfo) {
        final ParameterCollectionBuilder validParameterCollectionBuilder = validParameterCollectionBuilderFactory.create();
        traceLogger.trace(LOGGER, () -> String.format("Received REST request with headers: %s", httpTraceLoggerHelper.toHttpHeaderTrace(headers)));
        validParameterCollectionBuilder.putRequired("submissionId", submissionId, ParameterType.STRING);
        validParameterCollectionBuilder.putOptional("additionalInfo", additionalInfo, ParameterType.BOOLEAN);

        final String action = actionMapper.actionOf("getSubmissionsBySubmissionId", "GET", headers);
        final String responseStrategy = CSV_ACTION_NAME.equals(action) ? CSV_RESPONSE_STRATEGY : JSON_RESPONSE_STRATEGY;

        return restProcessor.process(responseStrategy, interceptorChainProcessor::process, action, headers, validParameterCollectionBuilder.parameters());
    }
}
