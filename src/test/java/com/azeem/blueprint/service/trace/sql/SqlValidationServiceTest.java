/// *
// * (C) Copyright 2026 Jawad Azeem
// * Apache 2.0 License
// */
//
// package com.azeem.blueprint.service.trace.sql;
//
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.azeem.blueprint.model.trace.SqlResponse;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
//
// class SqlValidationServiceTest {
//
//  private SqlValidationService validator;
//
//  @BeforeEach
//  void setUp() {
//    validator = new SqlValidationService();
//  }
//
//  private SqlResponse sql(String query) {
//    SqlResponse r = new SqlResponse();
//    r.setSql(query);
//    r.setReasoning("test");
//    return r;
//  }
//
//  @Test
//  @DisplayName("Valid SELECT query passes validation")
//  void validSelect_passes() {
//    assertThat(validator.isValidSql(sql("SELECT * FROM billing_records WHERE id = 1"))).isTrue();
//  }
//
//  @Test
//  @DisplayName("SELECT with aggregations passes validation")
//  void selectWithAggregations_passes() {
//    assertThat(
//            validator.isValidSql(
//                sql(
//                    "SELECT department, SUM(total_charge) FROM billing_records GROUP BY
// department")))
//        .isTrue();
//  }
//
//  @Test
//  @DisplayName("Column named 'updated_at' does not trigger false positive")
//  void columnNamedUpdatedAt_doesNotFalsePositive() {
//    assertThat(
//            validator.isValidSql(
//                sql("SELECT updated_at, deleted_at FROM billing_records LIMIT 10")))
//        .isTrue();
//  }
//
//  @Test
//  @DisplayName("INSERT statement is rejected")
//  void insertStatement_rejected() {
//    assertThat(validator.isValidSql(sql("INSERT INTO billing_records VALUES (1, 'test')")))
//        .isFalse();
//  }
//
//  @Test
//  @DisplayName("UPDATE statement is rejected")
//  void updateStatement_rejected() {
//    assertThat(validator.isValidSql(sql("UPDATE billing_records SET total_charge =
// 0"))).isFalse();
//  }
//
//  @Test
//  @DisplayName("DELETE statement is rejected")
//  void deleteStatement_rejected() {
//    assertThat(validator.isValidSql(sql("DELETE FROM billing_records WHERE id = 1"))).isFalse();
//  }
//
//  @Test
//  @DisplayName("DROP statement is rejected")
//  void dropStatement_rejected() {
//    assertThat(validator.isValidSql(sql("DROP TABLE billing_records"))).isFalse();
//  }
//
//  @Test
//  @DisplayName("ALTER statement is rejected")
//  void alterStatement_rejected() {
//    assertThat(validator.isValidSql(sql("ALTER TABLE billing_records ADD COLUMN x
// INT"))).isFalse();
//  }
//
//  @Test
//  @DisplayName("TRUNCATE statement is rejected")
//  void truncateStatement_rejected() {
//    assertThat(validator.isValidSql(sql("TRUNCATE TABLE billing_records"))).isFalse();
//  }
//
//  @Test
//  @DisplayName("Multi-statement query with semicolon is rejected")
//  void multiStatement_rejected() {
//    assertThat(validator.isValidSql(sql("SELECT 1; DROP TABLE billing_records"))).isFalse();
//  }
//
//  @Test
//  @DisplayName("Dangerous keyword hidden in block comment is rejected")
//  void dangerousKeywordInBlockComment_rejected() {
//    assertThat(
//            validator.isValidSql(sql("SELECT /* just a comment */ 1; DROP TABLE
// billing_records")))
//        .isFalse();
//  }
//
//  @Test
//  @DisplayName("Dangerous keyword hidden in line comment is stripped")
//  void dangerousKeywordInLineComment_stripped() {
//    // After stripping the line comment, this is just "SELECT 1 "
//    assertThat(validator.isValidSql(sql("SELECT 1 -- DROP TABLE billing_records"))).isTrue();
//  }
//
//  @Test
//  @DisplayName("Null SQL is rejected")
//  void nullSql_rejected() {
//    SqlResponse r = new SqlResponse();
//    r.setSql(null);
//    assertThat(validator.isValidSql(r)).isFalse();
//  }
//
//  @Test
//  @DisplayName("Blank SQL is rejected")
//  void blankSql_rejected() {
//    assertThat(validator.isValidSql(sql("   "))).isFalse();
//  }
//
//  @Test
//  @DisplayName("Non-SELECT query that doesn't start with SELECT is rejected")
//  void nonSelectQuery_rejected() {
//    assertThat(validator.isValidSql(sql("WITH cte AS (DELETE FROM t) SELECT * FROM cte")))
//        .isFalse();
//  }
//
//  @Test
//  @DisplayName("GRANT statement is rejected")
//  void grantStatement_rejected() {
//    assertThat(validator.isValidSql(sql("GRANT ALL ON billing_records TO public"))).isFalse();
//  }
//
//  @Test
//  @DisplayName("CREATE statement is rejected")
//  void createStatement_rejected() {
//    assertThat(validator.isValidSql(sql("CREATE TABLE evil (id INT)"))).isFalse();
//  }
// }
