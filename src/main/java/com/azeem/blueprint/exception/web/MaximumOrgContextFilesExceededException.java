/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.web;

public class MaximumOrgContextFilesExceededException extends RuntimeException {
  public MaximumOrgContextFilesExceededException(String message) {
    super(message);
  }
}
