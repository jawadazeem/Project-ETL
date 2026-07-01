/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import org.springframework.stereotype.Service;

@Service
public class SchemaService {
  public String getSchema() {
    return """
            DATABASE SCHEMA

            TABLE datasets (
                id UUID PRIMARY KEY,
                owner_user_id VARCHAR(255),
                billing_period VARCHAR(50),
                source_filename VARCHAR(255),
                s3_object_key VARCHAR(512),
                uploaded_at TIMESTAMPTZ,
                status VARCHAR(50)
            );

            TABLE billing_records (
                id BIGSERIAL PRIMARY KEY,
                dataset_id UUID REFERENCES datasets(id),
                account_name TEXT,
                resource_id TEXT,
                cloud_provider TEXT,
                billing_period TEXT,
                compute_hours DOUBLE PRECISION,
                storage_gb_used DOUBLE PRECISION,
                api_requests BIGINT,
                total_charge DOUBLE PRECISION,
                service_name TEXT,
                description TEXT
            );

            TABLE alarms (
                id UUID PRIMARY KEY,
                dataset_id UUID,
                business_key UUID,
                alarm_scope TEXT,
                billing_period TEXT,
                alarm_type TEXT,
                alarm_severity TEXT,
                explanation TEXT,
                timestamp TIMESTAMPTZ,
                resource_id TEXT,
                cloud_provider TEXT,
                service_name TEXT
            );
            """;
  }
}
