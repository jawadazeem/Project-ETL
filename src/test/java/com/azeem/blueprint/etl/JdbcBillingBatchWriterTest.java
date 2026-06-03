/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import com.azeem.blueprint.model.billing.BillingRecord;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcBillingBatchWriterTest {

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("writeBatch calls batchUpdate with correct SQL and batch size")
  void shouldCallBatchUpdateWithCorrectBatchSize() {
    JdbcBillingBatchWriter writer = new JdbcBillingBatchWriter(jdbcTemplate);

    List<BillingRecord> records =
        List.of(
            new BillingRecord(
                DATASET_ID,
                "Acme",
                "E001",
                "Engineering",
                "555-0100",
                "2026-01",
                120,
                1.5,
                10,
                45.75),
            new BillingRecord(
                DATASET_ID,
                "Beta Inc",
                "E002",
                "Finance",
                "555-0200",
                "2026-01",
                90,
                0.8,
                5,
                32.50));

    writer.writeBatch(records);

    ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor =
        ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
    verify(jdbcTemplate).batchUpdate(anyString(), setterCaptor.capture());

    BatchPreparedStatementSetter setter = setterCaptor.getValue();
    assertThat(setter.getBatchSize()).isEqualTo(2);
  }

  @Test
  @DisplayName("writeBatch handles empty list")
  void shouldHandleEmptyList() {
    JdbcBillingBatchWriter writer = new JdbcBillingBatchWriter(jdbcTemplate);

    writer.writeBatch(List.of());

    ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor =
        ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
    verify(jdbcTemplate).batchUpdate(anyString(), setterCaptor.capture());

    assertThat(setterCaptor.getValue().getBatchSize()).isZero();
  }

  @Test
  @DisplayName("SQL targets billing_records table with correct columns")
  void shouldUseCorrectInsertSql() {
    JdbcBillingBatchWriter writer = new JdbcBillingBatchWriter(jdbcTemplate);

    writer.writeBatch(List.of());

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), any(BatchPreparedStatementSetter.class));

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("INSERT INTO billing_records");
    assertThat(sql).contains("dataset_id");
    assertThat(sql).contains("billing_period");
    assertThat(sql).contains("total_charge");
    assertThat(sql).doesNotContain("id");
  }
}
