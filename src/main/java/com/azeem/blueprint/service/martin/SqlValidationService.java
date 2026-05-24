/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.martin;

import com.azeem.blueprint.model.martin.SqlResponse;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * SQL Validation Layer that ensures only safe, read-only SELECT queries are executed.
 *
 * <p>Uses word-boundary regex matching and comment stripping to avoid false positives (e.g.,
 * columns named "updated_at" or "deleted") and false negatives (keywords hidden in comments).
 */
@Component
public class SqlValidationService {

  private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
  private static final Pattern LINE_COMMENT = Pattern.compile("--[^\n]*");
  private static final Pattern DANGEROUS_KEYWORD =
      Pattern.compile(
          "\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|exec|execute|call)\\b",
          Pattern.CASE_INSENSITIVE);

  public boolean isValidSql(SqlResponse response) {
    if (response.getSql() == null || response.getSql().isBlank()) {
      return false;
    }

    String stripped = stripComments(response.getSql());
    String trimmed = stripped.trim().toLowerCase();

    if (!trimmed.startsWith("select")) {
      return false;
    }

    if (stripped.contains(";")) {
      return false;
    }

    return !DANGEROUS_KEYWORD.matcher(stripped).find();
  }

  private String stripComments(String sql) {
    String noBlock = BLOCK_COMMENT.matcher(sql).replaceAll(" ");
    return LINE_COMMENT.matcher(noBlock).replaceAll(" ");
  }
}
