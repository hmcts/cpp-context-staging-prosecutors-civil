package uk.gov.moj.cpp.persistence.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import javax.json.JsonArray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class JsonArrayConverterTest {

    private JsonArrayConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonArrayConverter();
    }

    @Test
    void convertToDatabaseColumnShouldReturnNullWhenAttributeIsNull() {
        String result = converter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    void convertToDatabaseColumn_shouldConvertJsonArrayToString() {
        JsonArray array = createArrayBuilder()
                .add("test")
                .add(123)
                .build();

        String result = converter.convertToDatabaseColumn(array);

        assertNotNull(result);
        assertEquals("[\"test\",123]", result);
    }

    @Test
    void convertToEntityAttributeShouldReturnEmptyArrayWhenDbDataIsNull() {
        JsonArray result = converter.convertToEntityAttribute(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_shouldConvertStringToJsonArray() {
        String json = "[\"test\",123]";

        JsonArray result = converter.convertToEntityAttribute(json);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("test", result.getString(0));
        assertEquals(123, result.getInt(1));
    }

    @Test
    void convertToEntityAttributeShouldHandleEmptyArrayString() {
        String json = "[]";

        JsonArray result = converter.convertToEntityAttribute(json);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void roundTripConversionShouldPreserveData() {
        JsonArray original = createArrayBuilder()
                .add("abc")
                .add(42)
                .add(true)
                .build();

        String dbValue = converter.convertToDatabaseColumn(original);
        JsonArray result = converter.convertToEntityAttribute(dbValue);

        assertEquals(original, result);
    }
}
