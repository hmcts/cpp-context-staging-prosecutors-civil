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
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/hearing-details.json", "schemas/hearing-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/prosecution-case.json", "schemas/prosecution-case.json"),
            Map.entry("http://justice.gov.uk/domain/core/common/definitions.json", "schemas/json/schema/definitions.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/defendant.json", "schemas/defendant.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/defendant-details.json", "schemas/defendant-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/offence.json", "schemas/offence.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/offence-details.json", "schemas/offence-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/offence-date-code.json", "schemas/offence-date-code.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/initiation-code.json", "schemas/initiation-code.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/organisation.json", "schemas/organisation.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/definitions.json", "schemas/common-definitions.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/language.json", "schemas/language.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/address.json", "schemas/address.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/individual.json", "schemas/individual.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/name-details.json", "schemas/name-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/gender.json", "schemas/gender.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/custody-status.json", "schemas/custody-status.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian.json", "schemas/parent-guardian/parent-guardian.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian-individual.json", "schemas/parent-guardian/parent-guardian-individual.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian-name-details.json", "schemas/parent-guardian/parent-guardian-name-details.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/parent-guardian-organisation.json", "schemas/parent-guardian/parent-guardian-organisation.json"),
            Map.entry("http://cpp.moj.gov.uk/staging/prosecutors/json/schemas/contact-details.json", "schemas/contact-details.json")
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
