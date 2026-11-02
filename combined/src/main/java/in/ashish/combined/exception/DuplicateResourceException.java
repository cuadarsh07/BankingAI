package in.ashish.combined.exception;

/**
 * Thrown when trying to create a resource that already exists (e.g. duplicate account number).
 * Maps to HTTP 400 BAD_REQUEST (or 409 CONFLICT).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
