package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import java.io.IOException;
import java.nio.charset.Charset;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;

public class ResourcesUtils {

    public static JsonObject asJsonObject(String resourceName) {
        return JsonObjects.createReader(ResourcesUtils.class
                .getResourceAsStream("/" + resourceName))
                .readObject();
    }

    public static String readResource(String fileName) {
        try {
            return IOUtils.toString(ClassLoader.getSystemResource(fileName), Charset.defaultCharset());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static JsonObject asJsonObject(String resourceName, String oldValue, String newValue) {
        String jsonString = readResource(resourceName).replace(oldValue, newValue);
        return JsonObjects.createReader(IOUtils.toInputStream(jsonString)).readObject();
    }
}
