package uk.gov.moj.cpp.staging.prosecutors.civil.util.queryclient;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QueryPoller<T> {

    private static final String READ_BASE_URI = "/stagingprosecutors-query-api/query/api/rest/stagingprosecutors";

    public ObjectMapper getMapper() {
        return mapper;
    }

    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper().copy();

    private final RestClient restClient = new RestClient();

    private final Class<T> queryClass;

    private Integer timeout = 10;

    private UUID userId = UUID.randomUUID();

    private Map<String, String> pathParameters = new HashMap<>();

    private Map<String, String> queryParameters = new HashMap<>();

    private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    public QueryPoller(Class<T> queryClass) {
        if (!queryClass.isAnnotationPresent(Query.class)) {
            throw new IllegalArgumentException("QueryPoller can only be used with the classes decorated with Query annotation.");
        }

        this.queryClass = queryClass;
    }

    public QueryPoller<T> setPathParameter(String key, String value) {
        pathParameters.put(key, value);
        return this;
    }

    public QueryPoller<T> setQueryParameter(String key, String value) {
        queryParameters.put(key, value);
        return this;
    }

    public QueryPoller<T> setHeader(String key, String value) {
        getHeaders().putSingle(key, value);
        return this;
    }

    public QueryPoller<T> setExecutingUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public QueryPoller<T> setTimeout(Integer seconds) {
        this.timeout = seconds;
        return this;
    }

    public T pollUntil(Predicate<T> predicate) {
        return await()
                .atMost(this.timeout, SECONDS)
                .until(this::read, predicate);
    }

    public T read() {
        final String uri = getUri();
        final String contentTypes = this.queryClass.getAnnotation(Query.class).contentType();
        final Response query = restClient.query(uri, contentTypes, getHeaders());
        if(query.getStatus() == Response.Status.OK.getStatusCode()) {
            final String stringEntity = query.readEntity(String.class);
            return deserialize(stringEntity);
        }
        return null;
    }

    private MultivaluedMap<String, Object> getHeaders() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, userId);
        headers.putAll(this.headers);
        return headers;
    }

    private String getUri() {
        String writeBaseUri = getBaseUri() + READ_BASE_URI;

        final Query queryAnnotation = this.queryClass.getAnnotation(Query.class);
        UriTemplate template = UriTemplate.buildFromTemplate(queryAnnotation.URI()).build();

        for (String variable : template.getVariables()) {
            if (!this.pathParameters.containsKey(variable)) {
                throw new RuntimeException("Specify all path params before calling");
            }
            Object value = this.pathParameters.get(variable);
            template.set(variable, value);
        }

        return writeBaseUri + template.expand();
    }

    private T deserialize(String payload) {
        try {
            return mapper.readValue(payload, queryClass);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
