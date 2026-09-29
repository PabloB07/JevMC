package dev.blancocl.jevmc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Java client for the Jev model by TypeSafe.ai (System One).
 *
 * <p>Evaluates a {@code state} against a map of typed questions in a single
 * call and returns structured answers with probabilities and confidence.
 *
 * <p>Typical usage from a PaperMC plugin (always off the main thread):
 *
 * <pre>{@code
 * JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
 *
 * jev.evaluateAsync(
 *         "This damn server, what a garbage plugin",
 *         Map.of("toxic", new Noul("The message is toxic or insulting"))
 * ).thenAccept(response -> {
 *     double toxic = response.get("toxic", NoulAnswer.class).noul();
 *     Bukkit.getScheduler().runTask(plugin, () -> {
 *         if (toxic >= 0.8) player.sendMessage("Message flagged");
 *     });
 * });
 * }</pre>
 */
public class JevClient {

    /** Official TypeSafe.ai endpoint. */
    public static final String DEFAULT_ENDPOINT = "https://api.typesafe.ai/v1/systemone";
    /** Alias of TypeSafe's flagship model. */
    public static final String DEFAULT_MODEL = "jev-latest";

    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(500);
    private static final int MAX_RETRIES = 3;

    private final HttpClient httpClient;
    private final Executor executor;
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final int maxRetries;
    private final Duration requestTimeout;

    public JevClient(String apiKey) {
        this(apiKey, DEFAULT_ENDPOINT, DEFAULT_MODEL, MAX_RETRIES,
                Duration.ofSeconds(30), defaultExecutor());
    }

    private JevClient(String apiKey, String endpoint, String model, int maxRetries,
                      Duration requestTimeout, Executor executor) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("A TypeSafe.ai API key is required");
        }
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.maxRetries = maxRetries;
        this.requestTimeout = requestTimeout;
        this.executor = executor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(executor)
                .build();
    }

    public JevClient withModel(String model) {
        return new JevClient(apiKey, endpoint, model, maxRetries, requestTimeout, executor);
    }

    public JevClient withEndpoint(String endpoint) {
        return new JevClient(apiKey, endpoint, model, maxRetries, requestTimeout, executor);
    }

    public JevClient withMaxRetries(int maxRetries) {
        return new JevClient(apiKey, endpoint, model, maxRetries, requestTimeout, executor);
    }

    public JevClient withRequestTimeout(Duration timeout) {
        return new JevClient(apiKey, endpoint, model, maxRetries, timeout, executor);
    }

    public JevClient withExecutor(Executor executor) {
        return new JevClient(apiKey, endpoint, model, maxRetries, requestTimeout, executor);
    }

    private static Executor defaultExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "jevmc-http");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Evaluates the state against the questions (blocking).
     * From a Paper plugin, only call this from a secondary thread or use
     * {@link #evaluateAsync(Object, Map)} instead.
     */
    public JevResponse evaluate(Object state, Map<String, ? extends Question> questions) {
        try {
            return evaluateAsync(state, questions).join();
        } catch (CompletionException e) {
            throw unwrap(e);
        }
    }

    /**
     * Evaluates the state against the questions asynchronously and returns a
     * future holding the typed response. Retries on 429/529 or network errors
     * use exponential backoff without blocking any threads.
     *
     * @param state     text or structure (Map/List/JsonElement) to evaluate
     * @param questions map of identifier -> typed question
     */
    public CompletableFuture<JevResponse> evaluateAsync(Object state,
                                                        Map<String, ? extends Question> questions) {
        HttpRequest request = buildRequest(buildRequestBody(state, questions));
        return executeWithRetry(request, 0);
    }

    private HttpRequest buildRequest(JsonObject body) {
        return HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    JsonObject buildRequestBody(Object state, Map<String, ? extends Question> questions) {
        JsonObject body = new JsonObject();
        body.add("state", JsonUtils.toJsonTree(state));
        body.addProperty("model", model);
        JsonObject questionsJson = new JsonObject();
        questions.forEach((id, question) -> questionsJson.add(id, question.toJson()));
        body.add("questions", questionsJson);
        return body;
    }

    private CompletableFuture<JevResponse> executeWithRetry(HttpRequest request, int attempt) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        return CompletableFuture.completedFuture(parseResponse(response.body()));
                    }
                    JevApiException error = new JevApiException(
                            response.statusCode(),
                            "TypeSafe API responded " + response.statusCode(),
                            response.body());
                    if (error.isRetryable() && attempt < maxRetries) {
                        return retryLater(request, attempt);
                    }
                    throw error;
                })
                .exceptionallyCompose(e -> {
                    Throwable cause = unwrap(e);
                    if (isRetryableNetworkError(cause) && attempt < maxRetries) {
                        return retryLater(request, attempt);
                    }
                    if (cause instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw new JevException("TypeSafe API call failed", cause);
                });
    }

    private CompletableFuture<JevResponse> retryLater(HttpRequest request, int attempt) {
        Duration backoff = INITIAL_BACKOFF.multipliedBy(1L << attempt);
        return CompletableFuture.<JevResponse>completedFuture(null)
                .thenComposeAsync(ignored -> executeWithRetry(request, attempt + 1),
                        CompletableFuture.delayedExecutor(backoff.toMillis(), TimeUnit.MILLISECONDS));
    }

    private static boolean isRetryableNetworkError(Throwable cause) {
        return cause instanceof IOException || cause instanceof HttpTimeoutException;
    }

    private static RuntimeException unwrap(Throwable e) {
        Throwable cause = e;
        while ((cause instanceof CompletionException || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        return new JevException(cause.getMessage(), cause);
    }

    JevResponse parseResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject answersJson = root.getAsJsonObject("answers");
            Map<String, Answer> answers = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : answersJson.entrySet()) {
                answers.put(entry.getKey(), parseAnswer(entry.getValue().getAsJsonObject()));
            }
            JsonObject usageJson = root.has("usage")
                    ? root.getAsJsonObject("usage") : new JsonObject();
            return new JevResponse(
                    JsonUtils.getString(root, "model"),
                    Map.copyOf(answers),
                    Usage.fromJson(usageJson));
        } catch (com.google.gson.JsonParseException | IllegalStateException
                 | ClassCastException | NullPointerException e) {
            throw new JevException("Invalid TypeSafe API response: " + responseBody, e);
        }
    }

    private static Answer parseAnswer(JsonObject json) {
        String type = JsonUtils.getString(json, "type");
        if (type == null) {
            throw new JevException("Answer missing type field: " + json);
        }
        switch (type) {
            case "noul" -> {
                return new NoulAnswer(JsonUtils.getDouble(json, "noul", 0.0));
            }
            case "choice" -> {
                Map<String, Double> probabilities = new LinkedHashMap<>();
                JsonObject probs = json.getAsJsonObject("probabilities");
                if (probs != null) {
                    probs.entrySet().forEach(e ->
                            probabilities.put(e.getKey(), e.getValue().getAsDouble()));
                }
                return new ChoiceAnswer(
                        JsonUtils.getString(json, "choice"),
                        Map.copyOf(probabilities),
                        JsonUtils.getDouble(json, "confidence", 0.0));
            }
            case "score" -> {
                Map<String, String> legend = new LinkedHashMap<>();
                Map<String, Double> probabilities = new LinkedHashMap<>();
                JsonObject legendJson = json.getAsJsonObject("legend");
                if (legendJson != null) {
                    legendJson.entrySet().forEach(e ->
                            legend.put(e.getKey(), e.getValue().getAsString()));
                }
                JsonObject probs = json.getAsJsonObject("probabilities");
                if (probs != null) {
                    probs.entrySet().forEach(e ->
                            probabilities.put(e.getKey(), e.getValue().getAsDouble()));
                }
                return new ScoreAnswer(
                        JsonUtils.getDouble(json, "score", 0.0),
                        Map.copyOf(legend),
                        Map.copyOf(probabilities),
                        JsonUtils.getDouble(json, "confidence", 0.0));
            }
            default -> throw new JevException("Unknown answer type: " + type);
        }
    }
}
