/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BillingPeriodFormatValidatorTest {

  private final BillingPeriodFormatValidator validator = new BillingPeriodFormatValidator();

  @ParameterizedTest
  @ValueSource(strings = {"2026-01", "2025-12", "2024-06", "2000-09", "2099-11"})
  @DisplayName("Valid YYYY-MM billing periods should pass validation")
  void validYearMonthFormat_passes(String value) {
    assertThat(validator.isValid(value, null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"dummy-data"})
  @DisplayName("Special demo billing period 'dummy-data' should pass validation")
  void dummyData_passes(String value) {
    assertThat(validator.isValid(value, null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2026-13", // month 13 is invalid
        "2026-00", // month 00 is invalid
        "70-2070", // year and month reversed
        "26-01", // two-digit year
        "2026-1", // single-digit month
        "2026/01", // wrong separator
        "2026_01", // underscore separator
        "",
        "demo-data", // wrong special value (not 'dummy-data')
        "abcd-ef",
        "2026-01-15" // has a day component
      })
  @DisplayName("Invalid billing period formats should fail validation")
  void invalidFormats_fail(String value) {
    assertThat(validator.isValid(value, null)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-01", "2026-09", "2026-10", "2026-12"})
  @DisplayName("Boundary months (01, 09, 10, 12) should pass validation")
  void boundaryMonths_pass(String value) {
    assertThat(validator.isValid(value, null)).isTrue();
  }
}
