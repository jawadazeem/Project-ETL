package com.azeem.billing.exception;

/**
 * Generic exception class for billing-related errors (fall-back).
 */

public class BillingException extends RuntimeException {

    public BillingException(String message) {
        super(message);
    }

}
