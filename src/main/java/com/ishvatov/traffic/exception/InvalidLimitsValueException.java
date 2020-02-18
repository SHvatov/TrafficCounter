package com.ishvatov.traffic.exception;

/**
 * Basic runtime exception, which is thrown, limits' values
 * are invalid - min is bigger or equals to max.
 *
 * @author ishvatov
 */
public class InvalidLimitsValueException extends RuntimeException {
    public InvalidLimitsValueException() {
        super("Invalid limits value: min is bigger or equals to max!");
    }
}
