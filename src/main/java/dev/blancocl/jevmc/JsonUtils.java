package dev.blancocl.jevmc;

import com.google.gson.JsonObject;

final class JsonUtils {

    private JsonUtils() {
    }

    static com.google.gson.JsonElement toJsonTree(Object value) {
        if (value instanceof com.google.gson.JsonElement element) {
            return element;
        }
        return new com.google.gson.Gson().toJsonTree(value);
    }

    static double getDouble(JsonObject obj, String member, double fallback) {
        if (obj.has(member) && obj.get(member).isJsonPrimitive()) {
            return obj.get(member).getAsDouble();
        }
        return fallback;
    }

    static String getString(JsonObject obj, String member) {
        if (obj.has(member) && obj.get(member).isJsonPrimitive()) {
            return obj.get(member).getAsString();
        }
        return null;
    }
}
