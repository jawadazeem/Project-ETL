/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class UnrecognizedPromptException extends RuntimeException {
  public UnrecognizedPromptException(String message) {
    super(message);
  }
}
