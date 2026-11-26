package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;

public class ResourcesUtils {

    public static JsonObject asJsonObject(String resourceName) {
        return createReader(ResourcesUtils.class
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
}
