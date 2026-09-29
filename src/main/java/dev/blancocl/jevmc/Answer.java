package dev.blancocl.jevmc;

/**
 * Typed answer to a question. One of: {@link NoulAnswer},
 * {@link ChoiceAnswer} or {@link ScoreAnswer}.
 */
public sealed interface Answer permits NoulAnswer, ChoiceAnswer, ScoreAnswer {

    String type();
}
