package dev.blancocl.jevmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JevClientTest {

    private final JevClient client = new JevClient("test-key");

    @Test
    void requestBodyWithTheThreePrimitives() {
        Map<String, Question> questions = new LinkedHashMap<>();
        questions.put("urgency", new Noul("The message conveys urgency"));
        questions.put("team", new Choice("Which team should handle this",
                Map.of("billing", "Payment issues", "technical", "Bugs")));
        questions.put("severity", new Score("Severity of the issue",
                List.of("Minor", "Moderate", "Severe")));

        JsonObject body = client.buildRequestBody("Help, I've had no payouts for 3 days", questions);

        assertEquals("Help, I've had no payouts for 3 days", body.get("state").getAsString());
        assertEquals("jev-latest", body.get("model").getAsString());

        JsonObject urgency = body.getAsJsonObject("questions").getAsJsonObject("urgency");
        assertEquals("noul", urgency.get("type").getAsString());
        assertEquals("The message conveys urgency",
                urgency.get("instructions").getAsString());

        JsonObject team = body.getAsJsonObject("questions").getAsJsonObject("team");
        assertEquals("choice", team.get("type").getAsString());
        assertEquals("Payment issues",
                team.getAsJsonObject("criteria").get("billing").getAsString());

        JsonObject severity = body.getAsJsonObject("questions").getAsJsonObject("severity");
        assertEquals("score", severity.get("type").getAsString());
        JsonArray levels = severity.getAsJsonArray("criteria");
        assertEquals(3, levels.size());
        assertEquals("Severe", levels.get(2).getAsString());
    }

    @Test
    void parsesTypedResponse() {
        String json = """
                {
                  "model": "jev-1.13.0",
                  "answers": {
                    "urgency": { "type": "noul", "noul": 0.95 },
                    "team": {
                      "type": "choice",
                      "choice": "technical",
                      "probabilities": { "technical": 0.85, "billing": 0.15 },
                      "confidence": 0.78
                    },
                    "severity": {
                      "type": "score",
                      "score": 1.05,
                      "legend": { "0": "Minor", "1": "Moderate", "2": "Severe" },
                      "probabilities": { "0": 0.0, "1": 0.95, "2": 0.05 },
                      "confidence": 0.92
                    }
                  },
                  "usage": { "input_tokens": 296, "output_tokens": 20 }
                }
                """;

        JevResponse response = client.parseResponse(json);

        assertEquals("jev-1.13.0", response.model());
        assertEquals(296, response.usage().inputTokens());

        NoulAnswer urgency = response.get("urgency", NoulAnswer.class);
        assertEquals(0.95, urgency.noul(), 1e-9);
        assertTrue(urgency.isLikelyTrue(0.8));

        ChoiceAnswer team = response.get("team", ChoiceAnswer.class);
        assertEquals("technical", team.choice());
        assertEquals(0.85, team.probability(), 1e-9);

        ScoreAnswer severity = response.get("severity", ScoreAnswer.class);
        assertEquals(1.05, severity.score(), 1e-9);
        assertEquals("Moderate", severity.closestLevel());
    }

    @Test
    void invalidResponseThrowsException() {
        assertThrows(JevException.class, () -> client.parseResponse("not json"));
        assertThrows(JevException.class, () ->
                client.parseResponse("{\"answers\":{\"x\":{\"type\":\"unknown\"}}}"));
    }

    @Test
    void apiKeyIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new JevClient(null));
        assertThrows(IllegalArgumentException.class, () -> new JevClient(" "));
    }
}
