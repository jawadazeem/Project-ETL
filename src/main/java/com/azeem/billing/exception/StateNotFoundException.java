package com.azeem.billing.exception;

/**
 * Exception thrown when no billing records are found for a given state.
 */

public class StateNotFoundException extends RuntimeException {

    public StateNotFoundException(String state) {
        super("No billing records found for state: " + state);
    }
}
