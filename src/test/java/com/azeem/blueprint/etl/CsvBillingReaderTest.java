/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Enterprise-style unit tests for {@link CsvBillingReader}.
 *
 * <p>These tests use in-memory streams to avoid filesystem dependencies and exercise header
 * skipping, row parsing, and EOF behavior.
 */
public class CsvBillingReaderTest {

  @Test
  @DisplayName("parseNextRow should skip header when configured and return subsequent rows")
  void parseNextRow_withHeader_skipsHeaderAndReturnsRows() throws Exception {
    String csv =
        "accountName,resourceId,cloudProvider\n" + "Acme,i-001,AWS\n" + "Beta,proj-002,GCP\n";

    try (InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        CsvBillingReader reader = new CsvBillingReader(in, true)) {

      String[] first = reader.parseNextRow();
      assertNotNull(first);
      assertArrayEquals(new String[] {"Acme", "i-001", "AWS"}, first);

      String[] second = reader.parseNextRow();
      assertNotNull(second);
      assertArrayEquals(new String[] {"Beta", "proj-002", "GCP"}, second);

      String[] eof = reader.parseNextRow();
      assertNull(eof, "parseNextRow should return null at EOF");
    }
  }

  @Test
  @DisplayName("parseNextRow without header should return rows starting at first line")
  void parseNextRow_noHeader_returnsAllRows() throws Exception {
    String csv = "A,i-001,AWS\nB,proj-002,GCP\n";

    try (InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        CsvBillingReader reader = new CsvBillingReader(in, false)) {

      String[] r1 = reader.parseNextRow();
      assertArrayEquals(new String[] {"A", "i-001", "AWS"}, r1);

      String[] r2 = reader.parseNextRow();
      assertArrayEquals(new String[] {"B", "proj-002", "GCP"}, r2);

      assertNull(reader.parseNextRow());
    }
  }

  @Test
  @DisplayName("close() should be idempotent and not throw when invoked multiple times")
  void close_idempotent_noException() throws Exception {
    String csv = "A,i-001,AWS\n";
    InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    CsvBillingReader reader = new CsvBillingReader(in, false);
    reader.close();
    assertDoesNotThrow(reader::close);
  }
}
