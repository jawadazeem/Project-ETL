/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import com.azeem.blueprint.entity.CloudConnectionEntity;
import com.azeem.blueprint.model.cloudconnection.*;
import com.azeem.blueprint.repository.AppUserRepository;
import com.azeem.blueprint.service.cloudconnection.CredentialEncryptionService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CloudConnectionMapper {
  private final AppUserRepository appUserRepository;
  private final CredentialEncryptionService encryptionService;

  public CloudConnectionMapper(
      AppUserRepository appUserRepository, CredentialEncryptionService encryptionService) {
    this.appUserRepository = appUserRepository;
    this.encryptionService = encryptionService;
  }

  public CloudConnection mapToDomain(CloudConnectionEntity entity) {
    return new CloudConnection(
        entity.getId(),
        entity.getOwnerUser().getId(),
        entity.getProvider(),
        entity.getDisplayName(),
        entity.getBucketName(),
        entity.getRegion(),
        CloudConnectionStatus.valueOf(entity.getStatus()),
        entity.getLastPolledAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public ActiveCloudConnection mapToActiveConnection(CloudConnectionEntity entity) {
    Map<String, String> credentials = encryptionService.decrypt(entity.getEncryptedCredentials());
    return new ActiveCloudConnection(
        entity.getId(),
        entity.getOwnerUser().getId(),
        entity.getProvider(),
        entity.getBucketName(),
        entity.getRegion(),
        credentials);
  }

  public CloudConnectionEntity mapToEntity(
      UUID ownerUserId, CloudConnectionRequest request, String encryptedCredentials) {
    CloudConnectionEntity entity = new CloudConnectionEntity();
    entity.setOwnerUser(appUserRepository.getReferenceById(ownerUserId));
    entity.setProvider(request.provider().toUpperCase());
    entity.setDisplayName(request.displayName());
    entity.setBucketName(request.bucketName());
    entity.setRegion(request.region());
    entity.setEncryptedCredentials(encryptedCredentials);
    entity.setStatus(CloudConnectionStatus.ACTIVE.name());
    entity.setCreatedAt(Instant.now());
    return entity;
  }
}
