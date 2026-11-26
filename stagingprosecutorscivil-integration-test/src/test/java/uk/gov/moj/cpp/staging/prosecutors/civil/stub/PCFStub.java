package uk.gov.moj.cpp.staging.prosecutors.civil.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.util.UUID;

public class PCFStub {
    public static void stubPCFCommand(final UUID userId) {
        stubPingFor("prosecutioncasefile-service");
        stubFor(post(urlPathMatching("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/*"))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, userId.toString())
                        .withHeader(CONTENT_TYPE, "application/json")));
    }
}
