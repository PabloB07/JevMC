package dev.blancocl.jevmc;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Picks one option from a set defined by criteria. Returns the chosen option,
 * the full probability distribution and the confidence.
 *
 * <pre>{@code
 * Choice department = new Choice("Which team should handle this",
 *         Map.of("billing", "Payment or subscription issues",
 *                "technical", "Bugs or integration problems"));
 * }</pre>
 */
public final class Choice extends Question {

    private final Map<String, Object> options;

    /**
     * @param instructions what the model should decide
     * @param options      map of option -> criteria description (use a
     *                     {@link java.util.LinkedHashMap} to preserve order)
     */
    public Choice(String instructions, Map<String, Object> options) {
        super("choice", instructions);
        this.options = Map.copyOf(options);
    }

    @Override
    JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonObject criteria = new JsonObject();
        options.forEach((option, description) -> {
            if (description == null) {
                criteria.addProperty(option, (String) null);
            } else {
                criteria.add(option, JsonUtils.toJsonTree(description));
            }
        });
        json.add("criteria", criteria);
        return json;
    }
}
