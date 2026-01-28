package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import uk.gov.justice.services.messaging.JsonObjects;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.JsonObjectsHelper.readFromString;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.InitiationCode;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.Awaitility;

public class ProsecutionCaseFileApi {

    public static void expectInitiateGroupProsecutionInvokedWith(final String resourceName) {
        Awaitility
                .await()
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> createInitiateGroupProsecutionInvokedWith(ResourcesUtils.asJsonObject(resourceName)));
    }
    public static void expectInitiateSingleProsecution(final String resourceName) {
        Awaitility
                .await()
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> createInitiateSingleProsecution(ResourcesUtils.asJsonObject(resourceName)));
    }
    private static boolean createInitiateGroupProsecutionInvokedWith(final JsonObject expectedPayload) {
        final RequestPatternBuilder request = postRequestedFor(
                urlPathEqualTo("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-group-prosecution"))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.prosecutioncasefile.command.initiate-group-prosecution+json"));

        return validateJsonForGroupCase(expectedPayload, request);
    }

    private static boolean createInitiateSingleProsecution(final JsonObject expectedPayload) {
        final RequestPatternBuilder request = postRequestedFor(
                urlPathEqualTo("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cc-prosecution"))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.prosecutioncasefile.command.initiate-cc-prosecution+json"));

        return validateJsonForSingleCase(expectedPayload, request);
    }

    private static boolean validateJsonForGroupCase(final JsonObject expectedPayload, final RequestPatternBuilder request) {
        if (findAll(request).size() == 0) {
            return false;
        } else {
            final Optional<JsonObject> actualJsonObjectPayload = findAll(request)
                    .stream()
                    .filter(actualRequest -> {
                        final String actualPayload = actualRequest.getBodyAsString();
                        final JsonObject payloadJsonObject = readFromString(actualPayload);
                        final JsonArray groupProsecutions = payloadJsonObject.getJsonArray("groupProsecutions");
                        final JsonObject jsonObject = groupProsecutions.getJsonObject(0).getJsonObject("caseDetails");
                        final JsonString prosecutorCaseReference = jsonObject.getJsonString("prosecutorCaseReference");
                        if (prosecutorCaseReference != null) {
                            return prosecutorCaseReference.getString().startsWith("case_urn_value");
                        } else {
                            return false;
                        }
                    })
                    .findFirst()
                    .map(LoggedRequest::getBodyAsString)
                    .map(JsonObjectsHelper::readFromString);

            if (actualJsonObjectPayload.isPresent()) {
                assertThat(expectedPayload.getString("prosecutingAuthority"), is(actualJsonObjectPayload.get().getJsonArray("groupProsecutions").getJsonObject(0).getJsonObject("caseDetails").getString("originatingOrganisation")));
                assertThat(expectedPayload.getJsonArray("prosecutionCases").getJsonObject(0).getString("urn"), is(actualJsonObjectPayload.get().getJsonArray("groupProsecutions").getJsonObject(0).getJsonObject("caseDetails").getString("prosecutorCaseReference")));
                assertThat(InitiationCode.O.name(), is(actualJsonObjectPayload.get().getJsonArray("groupProsecutions").getJsonObject(0).getJsonObject("caseDetails").getString("initiationCode")));
                assertThat(expectedPayload.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getJsonObject("offenceDetails").getString("cjsOffenceCode"), is(actualJsonObjectPayload.get().getJsonArray("groupProsecutions").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceCode")));
                return true;
            } else {
                return false;
            }
        }
    }

    private static boolean validateJsonForSingleCase(final JsonObject expectedPayload, final RequestPatternBuilder request) {
        if (findAll(request).size() == 0) {
            return false;
        } else {
            final Optional<JsonObject> actualJsonObjectPayload = findAll(request)
                    .stream()
                    .filter(actualRequest -> {
                        final String actualPayload = actualRequest.getBodyAsString();
                        final JsonObject payloadJsonObject = readFromString(actualPayload);
                        final JsonObject jsonObject = payloadJsonObject.getJsonObject("caseDetails");
                        final JsonString prosecutorCaseReference = jsonObject.getJsonString("prosecutorCaseReference");
                        if (prosecutorCaseReference != null) {
                            return prosecutorCaseReference.getString().startsWith("case_urn_value");
                        } else {
                            return false;
                        }
                    })
                    .findFirst()
                    .map(LoggedRequest::getBodyAsString)
                    .map(JsonObjectsHelper::readFromString);

            if (actualJsonObjectPayload.isPresent()) {
                assertThat(expectedPayload.getString("prosecutingAuthority"), is(actualJsonObjectPayload.get().getJsonObject("caseDetails").getString("originatingOrganisation")));
                assertThat(expectedPayload.getJsonArray("prosecutionCases").getJsonObject(0).getString("urn"), is(actualJsonObjectPayload.get().getJsonObject("caseDetails").getString("prosecutorCaseReference")));
                assertThat(InitiationCode.O.name(), is(actualJsonObjectPayload.get().getJsonObject("caseDetails").getString("initiationCode")));
                assertThat(expectedPayload.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getJsonObject("offenceDetails").getString("cjsOffenceCode"), is(actualJsonObjectPayload.get().getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceCode")));

                assertThat(actualJsonObjectPayload.get().getBoolean("isGroupMember"), is(false));
                assertThat(actualJsonObjectPayload.get().getBoolean("isGroupMaster"), is(false));
                assertThat(actualJsonObjectPayload.get().getBoolean("isCivil"), is(true));
                return true;
            } else {
                return false;
            }
        }
    }
}
