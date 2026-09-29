package dev.blancocl.jevmc;

import com.google.gson.JsonObject;

/**
 * Token usage for the request.
 */
public record Usage(int inputTokens, int outputTokens) {

    static Usage fromJson(JsonObject json) {
        int in = json.has("input_tokens") ? json.get("input_tokens").getAsInt() : 0;
        int out = json.has("output_tokens") ? json.get("output_tokens").getAsInt() : 0;
        return new Usage(in, out);
    }
}
