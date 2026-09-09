package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.staging.civil.handler.command.api.SchemaTestConstants.VALID_CHARGE_PROSECUTION_REQUEST;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class JsonRequestBuilderTest {

    @Test
    void from_throwsWhenGivenNonExistentResourcePath() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonRequestBuilder.from("/does-not-exist.json"));
    }

    @Test
    void from_buildsIsolatedCopyThatDoesNotShareStateWithOtherBuilders() {
        JsonRequestBuilder b1 = JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST);
        JsonRequestBuilder b2 = JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST);

        b1.remove("prosecutingAuthority");

        assertFalse(b1.build().has("prosecutingAuthority"), "b1 should have field removed");
        assertTrue(b2.build().has("prosecutingAuthority"),  "b2 should be unaffected");
        assertTrue(JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST).build().has("prosecutingAuthority"),
                "fresh builder should always reflect the original resource");
    }

    @Test
    void remove_deletesFieldAtTopLevelNestedDotNotationAndArrayIndexPaths() {
        JSONObject result = JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST)
                .remove("prosecutingAuthority")           // top-level
                .remove("hearingDetails.timeOfHearing")   // nested dot-notation
                .remove("prosecutionCases[0].urn")        // array-index notation
                .build();

        assertFalse(result.has("prosecutingAuthority"));
        assertFalse(result.getJSONObject("hearingDetails").has("timeOfHearing"));
        assertFalse(result.getJSONArray("prosecutionCases").getJSONObject(0).has("urn"));
    }

    @Test
    void set_preservesFullLengthOfLongStringValues() {
        String longOffenceWording = "A".repeat(2501);

        JSONObject result = JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST)
                .set(longOffenceWording,
                        "prosecutionCases[0].defendants[0].offences[0].offenceDetails.offenceWording")
                .build();

        String stored = result.getJSONArray("prosecutionCases")
                .getJSONObject(0).getJSONArray("defendants")
                .getJSONObject(0).getJSONArray("offences")
                .getJSONObject(0).getJSONObject("offenceDetails")
                .getString("offenceWording");

        assertEquals(2501, stored.length(), "long string must not be truncated by set or deep copy");
        assertEquals(longOffenceWording, stored);
    }

    @Test
    void set_updatesStringIntegerAndObjectValuesAtTopLevelNestedAndArrayIndexPaths() {
        JSONObject org = new JSONObject("{\"organisationName\":\"Test Organisation Ltd\"}");

        JSONObject result = JsonRequestBuilder.from(VALID_CHARGE_PROSECUTION_REQUEST)
                .set("NEWVAL01", "prosecutingAuthority")                                         // top-level String
                .set("2022/11/17", "hearingDetails.dateOfHearing")                              // nested String
                .set("SCIV99999", "prosecutionCases[0].urn")                                    // array-index String
                .set(5,           "prosecutionCases[0].defendants[0].individual.gender")        // integer
                .set(org,         "prosecutionCases[0].defendants[0].organisation")             // JSONObject
                .remove("hearingDetails.timeOfHearing")                                         // chained with remove
                .build();

        assertEquals("NEWVAL01", result.getString("prosecutingAuthority"));
        assertEquals("2022/11/17", result.getJSONObject("hearingDetails").getString("dateOfHearing"));
        assertFalse(result.getJSONObject("hearingDetails").has("timeOfHearing"));
        assertEquals("SCIV99999", result.getJSONArray("prosecutionCases").getJSONObject(0).getString("urn"));
        assertEquals(5, result.getJSONArray("prosecutionCases")
                .getJSONObject(0).getJSONArray("defendants")
                .getJSONObject(0).getJSONObject("individual").getInt("gender"));
        assertEquals("Test Organisation Ltd", result.getJSONArray("prosecutionCases")
                .getJSONObject(0).getJSONArray("defendants")
                .getJSONObject(0).getJSONObject("organisation").getString("organisationName"));
    }
}
