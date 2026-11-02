package in.ashish.combined.exception;

/**
 * Thrown when a transfer request violates business logic
 * (e.g., source and destination accounts are the same).
 * Maps to HTTP 400 BAD_REQUEST.
 */
public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
