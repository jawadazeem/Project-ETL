/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.model.trace.SqlResponse;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class QueryExecutionService {
  private final JdbcTemplate jdbcTemplate;

  public QueryExecutionService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> executeQuery(SqlResponse response) {
    return jdbcTemplate.queryForList(response.getSql());
  }
}
