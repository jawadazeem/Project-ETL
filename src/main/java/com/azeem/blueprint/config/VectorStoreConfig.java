/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import javax.sql.DataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

  @Bean
  @ConfigurationProperties("database-config.vector-db")
  public DataSourceProperties vectorDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource vectorDataSource() {
    return vectorDataSourceProperties().initializeDataSourceBuilder().build();
  }

  @Bean
  public PgVectorStore pgVectorStore(
      JdbcTemplate vectorJdbcTemplate, EmbeddingModel embeddingModel) {
    return PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
        .initializeSchema(false)
        .build();
  }
}
