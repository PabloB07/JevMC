package dev.blancocl.jevmc;

import java.util.Map;

/**
 * Result of a Jev evaluation: one answer per question, keyed by the same
 * identifier it was sent with.
 */
public record JevResponse(String model, Map<String, Answer> answers, Usage usage) {

    /**
     * Gets the answer for a question and casts it to the expected type.
     *
     * @throws ClassCastException if the type does not match
     */
    public <T extends Answer> T get(String questionId, Class<T> type) {
        Answer answer = answers.get(questionId);
        if (answer == null) {
            throw new IllegalArgumentException("No answer for question: " + questionId);
        }
        return type.cast(answer);
    }
}
