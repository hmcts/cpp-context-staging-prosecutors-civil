package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

/*
 * TODO: Java 21 has improved JSON Schema validation tooling.
 * See: https://github.com/hmcts/java-21-wildfly-32-updgrade-pilot-cpp-framework/blob/main/new-tools/effective-json-schema-plugin.md
 */
class SchemaValidationTestUtils {

    static final Map<String, String> SCHEMA_MAP = Map.ofEntries(
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/hearing-details.json", "json/schema/hearing-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/hearing-date-range-details.json", "json/schema/hearing-date-range-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/prosecution-case.json", "json/schema/prosecution-case.json"),
            Map.entry("http://justice.gov.uk/domain/core/common/definitions.json", "json/schema/definitions.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/defendant.json", "json/schema/defendant.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/defendant-details.json", "json/schema/defendant-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/offence.json", "json/schema/offence.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/offence-details.json", "json/schema/offence-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/offence-date-code.json", "json/schema/offence-date-code.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/initiation-code.json", "json/schema/initiation-code.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/organisation.json", "json/schema/organisation.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/definitions.json", "json/schema/common-definitions.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/language.json", "json/schema/language.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/address.json", "json/schema/address.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/individual.json", "json/schema/individual.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/name-details.json", "json/schema/name-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/gender.json", "json/schema/gender.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/custody-status.json", "json/schema/custody-status.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian.json", "json/schema/parent-guardian/parent-guardian.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian-individual.json", "json/schema/parent-guardian/parent-guardian-individual.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian-name-details.json", "json/schema/parent-guardian/parent-guardian-name-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian-organisation.json", "json/schema/parent-guardian/parent-guardian-organisation.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/contact-details.json", "json/schema/contact-details.json")
    );

    static Schema buildSchema(String schemaPath) {
        return SchemaLoader.builder()
                .schemaClient(buildSchemaClient())
                .schemaJson(loadJson(schemaPath))
                .build()
                .load()
                .build();
    }

    static JSONObject loadJson(String resourcePath) {
        InputStream stream = SchemaValidationTestUtils.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
        }
        return new JSONObject(new JSONTokener(stream));
    }

    static void assertViolations(Schema schema, String description, JSONObject request, List<String> expectedFragments) {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> schema.validate(request),
                "Expected a ValidationException for: " + description);

        List<String> violations = exception.getAllMessages();

        for (String fragment : expectedFragments) {
            assertTrue(
                    violations.stream().anyMatch(msg -> msg.contains(fragment)),
                    "[" + description + "] Expected violation containing '" + fragment + "' but violations were:\n" +
                            String.join("\n", violations)
            );
        }
    }

    private static SchemaClient buildSchemaClient() {
        return url -> {
            String localResource = SCHEMA_MAP.get(url);
            if (localResource == null) {
                throw new RuntimeException("No schema mapping found for URI: " + url);
            }
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(localResource);
            if (stream == null) {
                throw new RuntimeException("Schema not found in classpath: " + localResource);
            }
            return stream;
        };
    }
}
