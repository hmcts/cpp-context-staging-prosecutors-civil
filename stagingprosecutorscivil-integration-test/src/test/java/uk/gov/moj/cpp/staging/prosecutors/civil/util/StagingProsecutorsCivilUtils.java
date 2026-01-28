package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.civil.model.Submission;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class StagingProsecutorsCivilUtils {

    public static final String SUMMONS_PROSECUTION_CONTENT_TYPE = "application/vnd.stagingprosecutorscivil.summons-prosecution+json";
    public static final String CHARGE_PROSECUTION_CONTENT_TYPE = "application/vnd.stagingprosecutorscivil.charge-prosecution+json";
    private static final RestClient restClient = new RestClient();
    private static final String COMMAND_BASE_URI = getBaseUri() + "/stagingprosecutorscivil-command-api/command/api/rest/stagingprosecutors-civil";
    private static final String TOPIC_NAME = "jms.topic.stagingprosecutorscivil.event";
    private static ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private static JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private static final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
    private static final String READ_BASE_URI = getBaseUri()
            + "/stagingprosecutorscivil-query-api/query/api/rest/stagingprosecutors-civil";

    public static UrlResponse submitSummonsProsecution(final String inputFileName, final String contentType) {

       return submitProsecution(inputFileName, contentType, "stagingprosecutorscivil.event.summons-prosecution-received");

    }

    public static UrlResponse submitChargeProsecution(final String inputFileName, final String contentType) {

        return submitProsecution(inputFileName, contentType, "stagingprosecutorscivil.event.charge-prosecution-received");

    }

    private static UrlResponse submitProsecution(final String inputFileName, final String contentType, final String eventName) {

        try (final MessageConsumerClient messageConsumerClient = new MessageConsumerClient()) {

            messageConsumerClient.startConsumer(eventName, TOPIC_NAME);

            return submitProsecution(inputFileName, contentType);

        }

    }

    private static UrlResponse submitProsecution(final String resourceName, final String contentType) {

        final Response response = postCommand(resourceName, contentType);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        String responseFromCommandAPI = response.readEntity(String.class);
        JsonReader jsonReader = JsonObjects.createReader(new ByteArrayInputStream(responseFromCommandAPI.getBytes()));
        JsonObject jsonObject = jsonReader.readObject();
        UrlResponse urlResponse = jsonObjectToObjectConverter.convert(jsonObject, UrlResponse.class);
        return urlResponse;

    }

    private static Response postCommand(final String resourceName, final String contentType) {

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        return restClient.postCommand(COMMAND_BASE_URI + "/prosecutions/",
                contentType,
                ResourcesUtils.readResource(resourceName), headers
        );
    }

    public static Submission pollForSubmission(final UUID submissionId, final SubmissionStatus expectedSubmissionStatus) {
        return getSubmission(submissionId, withJsonPath("status", is(expectedSubmissionStatus.name())));
    }

    public static Submission getSubmission(final UUID submissionId, final Matcher<? super ReadContext> matcher) {
        final String payload = poll(getRequestParams(submissionId))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(10, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(matcher)
                )
                .getPayload();

        try {
            return mapper.readValue(payload, Submission.class);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static RequestParams getRequestParams(final UUID submissionId) {
        final String url = READ_BASE_URI + "/submissions/" + submissionId;
        final String mediaType = "application/vnd.stagingprosecutorscivil.submission-details+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, UUID.randomUUID())
                .build();
    }

    public static Metadata buildMetadata(final String eventName, final String userId) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withUserId(userId)
                .build();
    }

}
