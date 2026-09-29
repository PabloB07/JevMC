package dev.blancocl.jevmc;

/**
 * The API responded with an HTTP error (invalid key, malformed request,
 * rate limit exceeded, etc.).
 */
public class JevApiException extends JevException {

    private final int statusCode;
    private final String responseBody;

    public JevApiException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    /** @return true if the error is due to rate limiting or overload (429/529) */
    public boolean isRetryable() {
        return statusCode == 429 || statusCode == 529;
    }
}
