package dev.blancocl.jevmc;

import com.google.gson.JsonObject;

/**
 * Typed question evaluated against a state in a single Jev API call.
 * Do not instantiate directly: use {@link Noul}, {@link Choice} or {@link Score}.
 */
public abstract class Question {

    private final String type;
    private final Object instructions;

    protected Question(String type, Object instructions) {
        this.type = type;
        this.instructions = instructions;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.add("instructions", JsonUtils.toJsonTree(instructions));
        return json;
    }
}
