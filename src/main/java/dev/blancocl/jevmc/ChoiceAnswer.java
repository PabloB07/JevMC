package dev.blancocl.jevmc;

import java.util.Map;

/**
 * Choice answer: the selected option, the probability distribution and the
 * derived confidence (between 0 and 1).
 */
public record ChoiceAnswer(String choice,
                           Map<String, Double> probabilities,
                           double confidence) implements Answer {

    @Override
    public String type() {
        return "choice";
    }

    /** @return the probability assigned to the selected option */
    public double probability() {
        return probabilities.getOrDefault(choice, 0.0);
    }
}
