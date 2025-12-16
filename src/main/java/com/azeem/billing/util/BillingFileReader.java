package com.azeem.billing.util;

import java.util.List;

/**
 * Interface for reading and parsing billing files.
 */
public interface BillingFileReader {
    List<String[]> parse();
}
