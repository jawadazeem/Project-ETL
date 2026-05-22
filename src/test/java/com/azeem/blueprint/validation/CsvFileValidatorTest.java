/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class CsvFileValidatorTest {

  private final CsvFileValidator validator = new CsvFileValidator();

  @Test
  @DisplayName("null file fails validation (Rule 1)")
  void nullFile_fails() {
    assertThat(validator.isValid(null, null)).isFalse();
  }

  @Test
  @DisplayName("Empty file fails validation (Rule 2)")
  void emptyFile_fails() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(true);

    assertThat(validator.isValid(file, null)).isFalse();
  }

  @Test
  @DisplayName("File with null original filename fails validation (Rule 3)")
  void nullFilename_fails() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getOriginalFilename()).thenReturn(null);

    assertThat(validator.isValid(file, null)).isFalse();
  }

  @Test
  @DisplayName("File with blank original filename fails validation (Rule 3)")
  void blankFilename_fails() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getOriginalFilename()).thenReturn("   ");

    assertThat(validator.isValid(file, null)).isFalse();
  }

  @Test
  @DisplayName("File with non-.csv extension fails validation (Rule 4)")
  void nonCsvExtension_fails() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getOriginalFilename()).thenReturn("report.xlsx");

    assertThat(validator.isValid(file, null)).isFalse();
  }

  @Test
  @DisplayName("Valid .csv file passes all rules")
  void validCsvFile_passes() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getOriginalFilename()).thenReturn("billing-2026-01.csv");

    assertThat(validator.isValid(file, null)).isTrue();
  }

  @Test
  @DisplayName(".CSV uppercase extension passes (case-insensitive Rule 4)")
  void uppercaseCsvExtension_passes() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getOriginalFilename()).thenReturn("BILLING.CSV");

    assertThat(validator.isValid(file, null)).isTrue();
  }

  @Test
  @DisplayName("Mixed-case .Csv extension passes (case-insensitive Rule 4)")
  void mixedCaseCsvExtension_passes() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getOriginalFilename()).thenReturn("data.Csv");

    assertThat(validator.isValid(file, null)).isTrue();
  }
}
