package uk.gov.moj.cpp.persistence.converter;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class JsonArrayConverter implements AttributeConverter<JsonArray, String> {
    @Override
    public String convertToDatabaseColumn(final JsonArray attribute) {
        return ofNullable(attribute).map(JsonValue::toString).orElse(null);
    }

    @Override
    public JsonArray convertToEntityAttribute(final String dbData) {
        return ofNullable(dbData).map(this::convertNotNullStringToArray).orElse(createArrayBuilder().build());
    }

    private JsonArray convertNotNullStringToArray(final String dbData) {
        try (final JsonReader reader = Json.createReader(new StringReader(dbData))) {
            return reader.readArray();
        }
    }
}
