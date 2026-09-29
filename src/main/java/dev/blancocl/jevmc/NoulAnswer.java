package dev.blancocl.jevmc;

/**
 * Yes/no answer: 0.0 (no) to 1.0 (yes).
 */
public record NoulAnswer(double noul) implements Answer {

    @Override
    public String type() {
        return "noul";
    }

    /** @return true if the probability of "yes" is >= 0.5 */
    public boolean isLikelyTrue() {
        return noul >= 0.5;
    }

    /** @return true if the probability of "yes" reaches the given threshold */
    public boolean isLikelyTrue(double threshold) {
        return noul >= threshold;
    }
}
