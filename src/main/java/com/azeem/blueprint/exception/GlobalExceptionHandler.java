/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception;

import com.azeem.blueprint.exception.core.*;
import com.azeem.blueprint.exception.infra.DatasetNotFoundException;
import com.azeem.blueprint.exception.web.ApiException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Global exception handler for the billing application. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(CloudProviderNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCloudProviderNotFoundException(
      CloudProviderNotFoundException ex) {
    logger.warn("Cloud provider not found: {}", ex.getMessage());
    ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  // Handle BillingDataNotFoundException
  @ExceptionHandler(BillingDataNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBillingDataNotFoundException(
      BillingDataNotFoundException ex) {
    logger.warn("Billing data not found: {}", ex.getMessage());
    ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  // Handle BillingDataLoadException
  @ExceptionHandler(BillingDataLoadException.class)
  public ResponseEntity<ErrorResponse> handleBillingDataLoadException(BillingDataLoadException ex) {
    logger.error("Billing data load error", ex);
    ErrorResponse response =
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Handle TraceResponseInvalidException
  @ExceptionHandler(TraceResponseInvalidException.class)
  public ResponseEntity<ErrorResponse> handleTraceResponseNotValidException(
      TraceResponseInvalidException ex) {
    logger.error("Trace's Response was invalid.", ex);
    ErrorResponse response =
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Handle ConstraintViolationException (Jakarta Bean Validation)
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex) {
    logger.warn("Validation failed: {}", ex.getMessage());

    String message =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .findFirst()
            .orElse("Validation error");

    ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
    logger.warn("API rate limit exceeded: {}", ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("Validation error");
    logger.warn("Request body validation failed: {}", message);
    ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(DatasetNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleDatasetNotFound(DatasetNotFoundException ex) {
    logger.warn("Dataset not found: {}", ex.getMessage());
    ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(CorporateInfoNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCorporateInfoNotFound(
      CorporateInfoNotFoundException ex) {
    logger.warn("Corporate info not found: {}", ex.getMessage());
    ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(CloudConnectionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCloudConnectionNotFound(
      CloudConnectionNotFoundException ex) {
    logger.warn("Cloud connection not found: {}", ex.getMessage());
    ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(PdfReportNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePdfReportNotFound(PdfReportNotFoundException ex) {
    logger.warn("PDF report not found: {}", ex.getMessage());
    ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(PdfGenerationException.class)
  public ResponseEntity<ErrorResponse> handlePdfGenerationException(PdfGenerationException ex) {
    logger.error("PDF generation failed", ex);
    ErrorResponse response =
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "PDF generation failed");
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(BillingException.class)
  public ResponseEntity<ErrorResponse> handleGenericException(BillingException ex) {
    logger.error("Unhandled billing exception", ex);
    ErrorResponse response =
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
