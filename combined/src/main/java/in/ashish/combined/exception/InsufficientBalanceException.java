package in.ashish.combined.exception;

/**
 * Thrown when an account withdrawal exceeds the available balance.
 * Maps to HTTP 400 BAD_REQUEST.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
