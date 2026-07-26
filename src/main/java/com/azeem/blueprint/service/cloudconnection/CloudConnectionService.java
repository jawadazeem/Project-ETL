/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.cloudconnection;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.CloudConnectionEntity;
import com.azeem.blueprint.exception.core.CloudConnectionNotFoundException;
import com.azeem.blueprint.mapper.CloudConnectionMapper;
import com.azeem.blueprint.model.cloudconnection.*;
import com.azeem.blueprint.repository.cloudconnection.CloudConnectionRepository;
import com.azeem.blueprint.service.appuser.AppUserService;
import com.azeem.blueprint.service.billing.BillingS3Service;
import com.azeem.blueprint.service.dataset.DatasetService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloudConnectionService {
  private static final Logger log = LoggerFactory.getLogger(CloudConnectionService.class);

  private final CloudConnectionRepository connectionRepository;
  private final CloudConnectionMapper connectionMapper;
  private final CredentialEncryptionService encryptionService;
  private final AppUserService appUserService;

  public CloudConnectionService(
      CloudConnectionRepository connectionRepository,
      CloudConnectionMapper connectionMapper,
      CredentialEncryptionService encryptionService,
      AppUserService appUserService,
      BillingS3Service billingS3Service,
      DatasetService datasetService) {
    this.connectionRepository = connectionRepository;
    this.connectionMapper = connectionMapper;
    this.encryptionService = encryptionService;
    this.appUserService = appUserService;
  }

  public CloudConnection createConnection(UUID userId, CloudConnectionRequest request) {
    log.info("Creating cloud connection for user: {}", userId);
    AppUserEntity owner = appUserService.findOrCreateGuest(userId);
    String encrypted = encryptionService.encrypt(request.credentials());
    CloudConnectionEntity entity = connectionMapper.mapToEntity(owner.getId(), request, encrypted);
    CloudConnectionEntity saved = connectionRepository.save(entity);
    return connectionMapper.mapToDomain(saved);
  }

  public List<CloudConnection> listConnections(UUID userId) {
    return connectionRepository.findByOwnerUserId(userId).stream()
        .map(connectionMapper::mapToDomain)
        .toList();
  }

  public CloudConnection getConnection(UUID connectionId, UUID userId) {
    CloudConnectionEntity entity =
        connectionRepository
            .findByIdAndOwnerUserId(connectionId, userId)
            .orElseThrow(() -> new CloudConnectionNotFoundException(connectionId));
    return connectionMapper.mapToDomain(entity);
  }

  @Transactional
  public void deleteConnection(UUID connectionId, UUID userId) {
    log.info("Deleting cloud connection: {} for user: {}", connectionId, userId);
    CloudConnectionEntity entity =
        connectionRepository
            .findByIdAndOwnerUserId(connectionId, userId)
            .orElseThrow(() -> new CloudConnectionNotFoundException(connectionId));
    connectionRepository.delete(entity);
  }

  @Transactional
  public CloudConnection updateStatus(
      UUID connectionId, UUID userId, CloudConnectionStatus newStatus) {
    log.info("Updating status of connection {} to {}", connectionId, newStatus);
    CloudConnectionEntity entity =
        connectionRepository
            .findByIdAndOwnerUserId(connectionId, userId)
            .orElseThrow(() -> new CloudConnectionNotFoundException(connectionId));
    entity.setStatus(newStatus.name());
    entity.setUpdatedAt(Instant.now());
    return connectionMapper.mapToDomain(connectionRepository.save(entity));
  }

  @Transactional
  public void markPolled(UUID connectionId) {
    CloudConnectionEntity entity =
        connectionRepository
            .findById(connectionId)
            .orElseThrow(() -> new CloudConnectionNotFoundException(connectionId));
    entity.setLastPolledAt(Instant.now());
    connectionRepository.save(entity);
  }
}
