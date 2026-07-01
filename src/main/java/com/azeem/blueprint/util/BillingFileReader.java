/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.util;

/**
 * @author Jawad
 * @version 1.0 12/2025 Interface for reading and parsing billing files for strategy pattern
 */
public interface BillingFileReader {
  String[] parseNextRow();
}
