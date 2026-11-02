package in.ashish.combined.exception;

/**
 * Thrown when attempting financial transactions on an account that is SUSPENDED or CLOSED.
 * Maps to HTTP 400 BAD_REQUEST.
 */
public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(String message) {
        super(message);
    }
}
