package uk.gov.moj.cpp.staging.civil.handler.command.api.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

/**
 * The converter checks every row's hearing values against copies of two {@code hearing-details.json}
 * rules, because the schema itself only sees row 1's values. Fails if the copies drift from the schema.
 */
class SubmissionLevelColumnRulesTest {

    @Test
    void hearingRulesMatchTheHearingDetailsSchema() throws IOException {
        final JSONObject properties = hearingDetailsSchema().getJSONObject("properties");

        assertEquals(properties.getJSONObject("timeOfHearing").getString("pattern"),
                SummonsProsecutionCsvToJsonConverter.TIME_OF_HEARING_REGEX);
        assertEquals(properties.getJSONObject("courtHearingLocation").getInt("maxLength"),
                SummonsProsecutionCsvToJsonConverter.COURT_HEARING_LOCATION_MAX_LENGTH);
    }

    private JSONObject hearingDetailsSchema() throws IOException {
        try (InputStream schema = getClass().getResourceAsStream("/json/schema/hearing-details.json")) {
            return new JSONObject(new JSONTokener(schema));
        }
    }
}
