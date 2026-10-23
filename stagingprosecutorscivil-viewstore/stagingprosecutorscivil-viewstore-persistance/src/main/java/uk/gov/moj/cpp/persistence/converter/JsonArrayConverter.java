package uk.gov.moj.cpp.persistence.converter;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.StringReader;

import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
        try (final JsonReader reader = createReader(new StringReader(dbData))) {
            return reader.readArray();
        }
    }
}
