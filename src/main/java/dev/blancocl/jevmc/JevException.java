package dev.blancocl.jevmc;

/**
 * Error while communicating with the TypeSafe.ai API.
 */
public class JevException extends RuntimeException {

    public JevException(String message) {
        super(message);
    }

    public JevException(String message, Throwable cause) {
        super(message, cause);
    }
}
