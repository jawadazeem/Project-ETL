/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace.sql;

import com.azeem.blueprint.model.trace.SqlResponse;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Designed to execute queries on the Postgres relational database. The PGVector database uses
 * Spring AI for document retrieval.
 */
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
