package uk.gov.moj.cpp.staging.prosecutors.civil.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.ResourcesUtils.readResource;

import java.util.UUID;

public class SystemIDMapperStub {

    private static final String SYSTEM_ID_MAPPER_URL = "/system-id-mapper-api/rest/systemid/mappings";

    public static void stubAddMany() {
        stubPingFor("system-id-mapper-api");
        stubFor(post(urlPathEqualTo(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(USER_ID, UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(readResource("stub-data/systemid.mapping.list-response.json")
                        )));
    }
}
