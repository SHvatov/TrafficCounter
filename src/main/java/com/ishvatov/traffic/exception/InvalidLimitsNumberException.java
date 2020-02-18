package com.ishvatov.traffic.exception;

/**
 * Basic runtime exception, which is thrown, when zero or more than one
 * record in the limits_per_hour table has the maximum effective_date
 * column value.
 *
 * @author ishvatov
 */
public class InvalidLimitsNumberException extends RuntimeException {
    /**
     * Default class constructor.
     */
    public InvalidLimitsNumberException() {
        super("Invalid number of the limits found in the database!");
    }
}
