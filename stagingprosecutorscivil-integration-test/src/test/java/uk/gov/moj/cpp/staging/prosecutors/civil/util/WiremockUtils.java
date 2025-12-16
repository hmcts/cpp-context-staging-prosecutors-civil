package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.fail;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.ResourcesUtils.readResource;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.util.UUID;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

public class WiremockUtils {
    private static final String USER_DETAILS_MEDIA_TYPE = "application/vnd.usersgroups.logged-in-user-details+json";
    private static final String USER_DETAILS_URL = "/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user";
    private static final int HTTP_STATUS_OK = 200;

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int PORT = 8080;

    private static final String SYSTEM_ID_MAPPER_URL = "/system-id-mapper-api/rest/systemid/mappings/*";
    private static final String USER_GROUPS_USERS_QUERY_URL = "/usersgroups-service/query/api/rest/usersgroups/users/.*/groups";
    private static final String CONTENT_TYPE_QUERY_PERMISSION = "application/vnd.usersgroups.get-logged-in-user-permissions+json";

    public WiremockUtils() {
        configureFor(HOST, PORT);
        reset();
        stubUserAndGroups();
    }

    public WiremockUtils stubPing(String url) {
        InternalEndpointMockUtils.stubPingFor(url);
        return this;
    }

    public WiremockUtils stubPost(String url) {
        stubFor(post(
                urlMatching(url))
                .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        return this;
    }

    public WiremockUtils stubIdMapperRecordingNewAssociation() {
        stubPingFor("system-id-mapper-api");
        stubFor(get(urlPathMatching(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(post(urlPathMatching(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(systemIdResponseTemplate(randomUUID()))));

        return this;
    }

    private String systemIdResponseTemplate(final UUID associationId) {
        return "{\n" +
                "\t\"_metadata\": {\n" +
                "\t\t\"id\": \"f2426280-f4d7-45cf-9f94-c618a210f7c2\",\n" +
                "\t\t\"name\": \"systemid.map\"\n" +
                "\t},\n" +
                "\t\"id\": \"" + associationId.toString() + "\"\n" +
                "}";
    }

    private void stubUserAndGroups() {
        stubFor(get(urlMatching(USER_GROUPS_USERS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(readResource("stub-data/usersgroups.get-groups-by-user.json"))));
    }

    public static void setupLoggedInUsersPermissionQueryStub(final String userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, userId)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(resourceToString("stub-data/usersgroups.user-civil-permissions.json"))));

        waitForStubToBeReady("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions", CONTENT_TYPE_QUERY_PERMISSION);
    }

    public static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Response.Status.OK);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        var urlPattern =resource.startsWith("/") ? "{0}{1}" : "{0}/{1}";
        poll(requestParams(MessageFormat.format(urlPattern, getBaseUri(), resource), mediaType).build())
                .until(status().is(expectedStatus));
    }


    public static String resourceToString(final String path, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            return format(IOUtils.toString(systemResourceAsStream), placeholders);
        } catch (final IOException e) {
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }

}
