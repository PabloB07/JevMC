package dev.blancocl.jevmc;

import com.google.gson.JsonObject;

/**
 * Yes/no question. Returns the probability that the answer is yes
 * (0.0 = no, 1.0 = yes).
 *
 * <pre>{@code
 * Noul urgency = new Noul("The message conveys urgency");
 * }</pre>
 */
public final class Noul extends Question {

    private final String yesMeans;
    private final String noMeans;

    public Noul(String instructions) {
        this(instructions, null, null);
    }

    /**
     * @param instructions the yes/no question to evaluate
     * @param yesMeans     what a value near 1 means
     * @param noMeans      what a value near 0 means
     */
    public Noul(String instructions, String yesMeans, String noMeans) {
        super("noul", instructions);
        this.yesMeans = yesMeans;
        this.noMeans = noMeans;
    }

    @Override
    JsonObject toJson() {
        JsonObject json = super.toJson();
        if (yesMeans != null || noMeans != null) {
            JsonObject criteria = new JsonObject();
            if (yesMeans != null) {
                criteria.add("true", JsonUtils.toJsonTree(yesMeans));
            }
            if (noMeans != null) {
                criteria.add("false", JsonUtils.toJsonTree(noMeans));
            }
            json.add("criteria", criteria);
        }
        return json;
    }
}
