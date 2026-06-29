/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.orgcontext;

import java.time.Instant;
import java.util.UUID;

public record OrgContextDocument(
    UUID id, UUID ownerUserId, String sourceFilename, String s3ObjectKey, Instant uploadedAt) {}
