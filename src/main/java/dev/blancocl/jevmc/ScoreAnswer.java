package dev.blancocl.jevmc;

import java.util.Map;

/**
 * Score answer: the probability-weighted value across the rubric levels,
 * with its legend and confidence.
 */
public record ScoreAnswer(double score,
                          Map<String, String> legend,
                          Map<String, Double> probabilities,
                          double confidence) implements Answer {

    @Override
    public String type() {
        return "score";
    }

    /** @return the description of the level closest to the score */
    public String closestLevel() {
        long level = Math.round(score);
        return legend.getOrDefault(String.valueOf(level), null);
    }
}
