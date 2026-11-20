package uk.gov.moj.cpp.staging.civil.processor.schema;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the EVENT_PROCESSOR copy of the other-case-received schema falling out
 * of sync with the canonical schema in stagingprosecutorscivil-domain-message, which caused
 * "extraneous key [relatedReferenceNumber] is not permitted" at runtime for enforcement cases.
 */
public class OtherCaseReceivedSchemaTest {

    private static final Path SCHEMA_PATH =
            Paths.get("src/yaml/json/schema/stagingprosecutorscivil.event.other-case-received.json");

    private static Schema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        final JSONObject rawSchema = new JSONObject(Files.readString(SCHEMA_PATH));
        removeReferences(rawSchema);
        schema = SchemaLoader.load(rawSchema);
    }

    @Test
    void shouldAcceptRelatedReferenceNumberOnEnforcementCase() {
        final JSONObject payload = validPayload()
                .put("hearingDateRangeDetails", new JSONObject())
                .put("relatedReferenceNumber", "GOB123456791");

        assertDoesNotThrow(() -> schema.validate(payload));
    }

    @Test
    void shouldAcceptCivilCaseWithoutRelatedReferenceNumber() {
        final JSONObject payload = validPayload()
                .put("hearingDetails", new JSONObject());

        assertDoesNotThrow(() -> schema.validate(payload));
    }

    @Test
    void shouldRejectUnknownProperty() {
        final JSONObject payload = validPayload()
                .put("hearingDateRangeDetails", new JSONObject())
                .put("notAKnownField", "unexpected");

        assertThrows(ValidationException.class, () -> schema.validate(payload));
    }

    @Test
    void shouldRejectWhenNeitherHearingDetailsNorDateRangeDetailsPresent() {
        final JSONObject payload = validPayload();

        assertThrows(ValidationException.class, () -> schema.validate(payload));
    }

    @Test
    void shouldRejectWhenBothHearingDetailsAndDateRangeDetailsPresent() {
        final JSONObject payload = validPayload()
                .put("hearingDetails", new JSONObject())
                .put("hearingDateRangeDetails", new JSONObject());

        assertThrows(ValidationException.class, () -> schema.validate(payload));
    }

    private static JSONObject validPayload() {
        return new JSONObject()
                .put("submissionId", "c6608900-8fd4-4ee2-a07f-ae5c0ae5063b")
                .put("submissionStatus", "PENDING")
                .put("prosecutingAuthority", "GAPGD00")
                .put("prosecutionCases", new JSONArray().put(new JSONObject()));
    }

    private static void removeReferences(final JSONObject jsonObject) {
        for (final String key : jsonObject.keySet()) {
            final Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                final JSONObject child = (JSONObject) value;
                child.remove("$ref");
                removeReferences(child);
            }
        }
    }
}
