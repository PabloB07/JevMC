package dev.blancocl.jevmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Rates the state against an ordered rubric of levels. Returns a
 * probability-weighted value that can land between levels.
 *
 * <pre>{@code
 * Score frustration = new Score("How frustrated the player is",
 *         List.of("Calm", "Frustrated", "Very angry"));
 * }</pre>
 */
public final class Score extends Question {

    private final List<Object> levels;

    /**
     * @param instructions what the model should rate
     * @param levels       ordered list of level descriptions (at least 2,
     *                     up to 10)
     */
    public Score(String instructions, List<Object> levels) {
        super("score", instructions);
        if (levels == null || levels.size() < 2) {
            throw new IllegalArgumentException("A Score needs at least 2 levels");
        }
        if (levels.size() > 10) {
            throw new IllegalArgumentException("A Score accepts up to 10 levels");
        }
        this.levels = List.copyOf(levels);
    }

    public Score(String instructions, Object... levels) {
        this(instructions, List.of(levels));
    }

    @Override
    JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonArray criteria = new JsonArray();
        levels.forEach(level -> criteria.add(JsonUtils.toJsonTree(level)));
        json.add("criteria", criteria);
        return json;
    }
}
