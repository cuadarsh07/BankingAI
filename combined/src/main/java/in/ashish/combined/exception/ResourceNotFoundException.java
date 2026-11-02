package in.ashish.combined.exception;

/**
 * Thrown when a requested resource (like an Account) is not found in the database.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
