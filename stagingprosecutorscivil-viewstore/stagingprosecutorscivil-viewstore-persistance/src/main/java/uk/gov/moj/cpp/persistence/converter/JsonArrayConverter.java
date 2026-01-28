package uk.gov.moj.cpp.persistence.converter;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import java.io.StringReader;

import uk.gov.justice.services.messaging.JsonObjects;
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
        try (final JsonReader reader = JsonObjects.createReader(new StringReader(dbData))) {
            return reader.readArray();
        }
    }
}
