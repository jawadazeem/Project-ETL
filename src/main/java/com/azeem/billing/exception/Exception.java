package com.azeem.billing.exception;

/**
 * Generic exception class for billing-related errors (fall-back).
 */

public class Exception extends RuntimeException {

    public Exception(String message) {
        super(message);
    }

}
