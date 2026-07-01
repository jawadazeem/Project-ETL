/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import javax.sql.DataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  public DataSource dataSource(
      @Qualifier("dataSourceProperties") DataSourceProperties dataSourceProperties) {
    return dataSourceProperties.initializeDataSourceBuilder().build();
  }

  @Bean
  @Primary
  public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  @ConfigurationProperties("database-config.vector-db")
  public DataSourceProperties vectorDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource vectorDataSource(
      @Qualifier("vectorDataSourceProperties") DataSourceProperties vectorDataSourceProperties) {
    return vectorDataSourceProperties.initializeDataSourceBuilder().build();
  }

  @Bean
  public JdbcTemplate vectorJdbcTemplate(
      @Qualifier("vectorDataSource") DataSource vectorDataSource) {
    return new JdbcTemplate(vectorDataSource);
  }

  @Bean
  public PgVectorStore pgVectorStore(
      @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
      EmbeddingModel embeddingModel) {
    vectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

    return PgVectorStore.builder(vectorJdbcTemplate, embeddingModel).initializeSchema(true).build();
  }
}
