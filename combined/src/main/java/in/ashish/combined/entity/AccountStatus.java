package in.ashish.combined.entity;

/**
 * Represents the lifecycle status of a bank account.
 * Stored as a String in the database to prevent enum-ordinal corruption.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}
