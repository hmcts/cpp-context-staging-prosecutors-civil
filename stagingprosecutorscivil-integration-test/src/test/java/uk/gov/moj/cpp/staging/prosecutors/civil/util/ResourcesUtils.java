package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Map;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;

public class ResourcesUtils {

    public static JsonObject asJsonObject(String resourceName) {
        return createReader(ResourcesUtils.class
                .getResourceAsStream("/" + resourceName))
                .readObject();
    }

    /**
     * Reads a JSON resource, replacing each placeholder (e.g. {@code %SUBMISSION_ID%}) with its value first.
     */
    public static JsonObject asJsonObject(final String resourceName, final Map<String, String> placeholders) {
        String json = readResource(resourceName);
        for (final Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            json = json.replace(placeholder.getKey(), placeholder.getValue());
        }
        return createReader(new StringReader(json)).readObject();
    }

    /**
     * Reads an expected-CSV resource without its trailing newline, matching CSV responses that do
     * not end with one.
     */
    public static String readExpectedCsv(final String resourceName) {
        return readResource(resourceName).replaceFirst("\\R\\z", "");
    }

    public static String readResource(String fileName) {
        try {
            return IOUtils.toString(ClassLoader.getSystemResource(fileName), Charset.defaultCharset());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
