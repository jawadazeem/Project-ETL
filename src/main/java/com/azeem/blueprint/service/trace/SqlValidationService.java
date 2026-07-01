/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.exception.core.TraceResponseInvalidException;
import com.azeem.blueprint.model.trace.SqlResponse;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SQL Validation Layer that ensures only safe, read-only SELECT queries are executed.
 *
 * <p>Uses JSql to parse SQL for ensuring the LLM only queries the DB using read-only SQL.
 * Previously used word-boundary regex matching and comment stripping to avoid false positives
 * (e.g., columns named "updated_at" or "deleted") and false negatives (keywords hidden in
 * comments). Though this was deemed unreliable and overly strict.
 */
@Component
public class SqlValidationService {
  private static final Logger log = LoggerFactory.getLogger(SqlValidationService.class);

  public boolean isValidSql(SqlResponse response) {
    String sql = response.getSql();

    if (sql == null || sql.isBlank()) {
      return false;
    }

    Statement statement = null;
    try {
      statement = CCJSqlParserUtil.parse(sql);
    } catch (JSQLParserException e) {
      log.error("Could not validate Trace's SQL: {}", e.getMessage());
      throw new TraceResponseInvalidException(e.getMessage(), e);
    }

    return statement instanceof PlainSelect;
  }
}
