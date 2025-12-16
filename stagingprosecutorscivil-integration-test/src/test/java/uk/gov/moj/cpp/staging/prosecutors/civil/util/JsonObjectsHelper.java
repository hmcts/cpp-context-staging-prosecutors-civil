package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class JsonObjectsHelper {

    private JsonObjectsHelper() {
        //for sonar...
    }

    public static void validateJsonObjectIsSubset(final JsonObject subset, final JsonObject superset, final String... ignoreNames) {

        final Collection<String> ignored = Arrays.asList(ignoreNames);

        subset.forEach((key, expectedValue) -> {

            if (!(ignored.contains(key))) {
                final JsonValue actualValue = superset.get(key);

                if (isNull(actualValue) && !ignored.contains(key)) {
                    throw new IllegalArgumentException(format("Actual value for key %s not found", key));
                }

                if (actualValue.getValueType() == JsonValue.ValueType.OBJECT) {
                    validateJsonObjectIsSubset(subset.getJsonObject(key), (JsonObject) actualValue, ignoreNames);
                } else if (actualValue.getValueType() == JsonValue.ValueType.ARRAY) {
                    final JsonArray arrayFromSuperset = (JsonArray) actualValue;
                    final JsonArray arrayFromSubset = (JsonArray) expectedValue;

                    validateJsonArrayIsSubset(arrayFromSuperset, arrayFromSubset, key);

                } else if (!actualValue.equals(expectedValue) && !ignored.contains(key)) {
                    throw new IllegalArgumentException((format("Actual %s -> %s, doesn't match expected %s -> %s", key, actualValue, key, expectedValue)));
                }
            }
        });

    }

    private static void validateJsonArrayIsSubset(final JsonArray arrayFromSuperset, final JsonArray arrayFromSubset, final String key) {

        if (arrayFromSuperset.size() != arrayFromSubset.size()) {
            throw new IllegalArgumentException(format("Actual array for %s size %d doesn't match expected size %d", key, arrayFromSuperset.size(), arrayFromSubset.size()));
        }
        for (int i = 0; i < arrayFromSuperset.size(); i++) {
            validateJsonObjectIsSubset(arrayFromSubset.getJsonObject(i), arrayFromSuperset.getJsonObject(i));
        }
    }


    public static JsonObject readFromString(final String jsonObjectStr) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonObjectStr))) {
            return reader.readObject();
        }
    }
}
