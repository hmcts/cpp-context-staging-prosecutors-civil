package uk.gov.moj.cpp.staging.civil.handler.command.api;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Builder that creates a deep copy of a base valid request
 * and applies targeted field modifications for violation test scenarios.
 */
class JsonRequestBuilder {

    private final JSONObject json;

    private JsonRequestBuilder(JSONObject base) {
        this.json = new JSONObject(base.toString());
    }

    static JsonRequestBuilder from(String resourcePath) {
        return new JsonRequestBuilder(SchemaValidationTestUtils.loadJson(resourcePath));
    }

    JsonRequestBuilder remove(String dotPath) {
        String[] parts = parse(dotPath);
        Object parent = navigate(json, parts, parts.length - 1);
        String last = parts[parts.length - 1];
        if (parent instanceof JSONObject) ((JSONObject) parent).remove(last);
        return this;
    }

    JsonRequestBuilder set(Object value, String dotPath) {
        String[] parts = parse(dotPath);
        Object parent = navigate(json, parts, parts.length - 1);
        String last = parts[parts.length - 1];
        if (parent instanceof JSONObject)   ((JSONObject) parent).put(last, value);
        else if (parent instanceof JSONArray) ((JSONArray) parent).put(Integer.parseInt(last), value);
        return this;
    }

    JSONObject build() {
        return json;
    }

    private static String[] parse(String dotPath) {
        return dotPath.replace("[", ".").replace("]", "").split("\\.");
    }

    private static Object navigate(Object node, String[] parts, int depth) {
        Object current = node;
        for (int i = 0; i < depth; i++) {
            String key = parts[i];
            if (current instanceof JSONObject)  current = ((JSONObject)  current).get(key);
            else if (current instanceof JSONArray) current = ((JSONArray) current).get(Integer.parseInt(key));
        }
        return current;
    }
}
