/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.cloudconnection;

import java.util.Map;
import java.util.UUID;

/** Sent to the poller Lambda function responsible for polling clients' BLOB storages. */
public record CloudConnectionPollRequest(
    UUID id,
    UUID ownerUserId,
    String ownerBlueprintBucketKey, // the bucket key to write to
    String provider,
    String clientBucketName,
    String region,
    Map<String, String> credentials) {
  public CloudConnectionPollRequest(
      CloudConnection connection, Map<String, String> credentials, String ownerBlueprintBucketKey) {
    this(
        connection.id(),
        connection.ownerUserId(),
        ownerBlueprintBucketKey,
        connection.provider(),
        connection.bucketName(),
        connection.region(),
        credentials);
  }
}
