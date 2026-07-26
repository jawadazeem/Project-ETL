/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.cloudconnection;

import com.azeem.blueprint.entity.CloudConnectionEntity;
import com.azeem.blueprint.exception.core.CannotSyncExpiredDatasetException;
import com.azeem.blueprint.exception.core.CloudPollRequestException;
import com.azeem.blueprint.mapper.CloudConnectionMapper;
import com.azeem.blueprint.model.cloudconnection.ActiveCloudConnection;
import com.azeem.blueprint.model.cloudconnection.CloudConnection;
import com.azeem.blueprint.model.cloudconnection.CloudConnectionPollRequest;
import com.azeem.blueprint.model.cloudconnection.CloudConnectionStatus;
import com.azeem.blueprint.model.dataset.Dataset;
import com.azeem.blueprint.repository.cloudconnection.CloudConnectionRepository;
import com.azeem.blueprint.service.billing.BillingS3Service;
import com.azeem.blueprint.service.dataset.DatasetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

/**
 * Hooked up with AWS EventBridge. Listens to the event to poll clients' S3/Container storages
 * across their cloud environments
 */
@Service
public class CloudPollService {
  private static final Logger log = LoggerFactory.getLogger(CloudPollService.class);

  private final EventBridgeClient eventBridgeClient;
  private final CloudConnectionService cloudConnectionService;
  private final CloudConnectionRepository connectionRepository;
  private final CloudConnectionMapper connectionMapper;
  private final BillingS3Service billingS3Service;
  private final DatasetService datasetService;
  private final CredentialEncryptionService encryptionService;
  private final ObjectMapper objectMapper;

  public CloudPollService(
      EventBridgeClient eventBridgeClient,
      CloudConnectionService cloudConnectionService,
      BillingS3Service billingS3Service,
      CloudConnectionMapper connectionMapper,
      DatasetService datasetService,
      CloudConnectionRepository connectionRepository,
      CredentialEncryptionService encryptionService,
      ObjectMapper objectMapper) {
    this.cloudConnectionService = cloudConnectionService;
    this.eventBridgeClient = eventBridgeClient;
    this.connectionMapper = connectionMapper;
    this.connectionRepository = connectionRepository;
    this.billingS3Service = billingS3Service;
    this.datasetService = datasetService;
    this.encryptionService = encryptionService;
    this.objectMapper = objectMapper;
  }

  @Scheduled(cron = "0 0 */3 * * ?") // Fires every 3 hours
  public void triggerGlobalPoll() {
    List<UUID> userIds = connectionRepository.findDistinctOwnerUserIdsByStatus(CloudConnectionStatus.ACTIVE.name());
    log.info("Triggering global sync for {} active users via cron", userIds.size());

    // 2. Loop safely so one corrupt configuration doesn't break the entire system run
    for (UUID userId : userIds) {
      try {
        triggerSync(userId);
      } catch (CannotSyncExpiredDatasetException e) {
        log.warn("Skipping sync for user {} due to missing or expired dataset", userId);
      } catch (Exception e) {
        log.error("Unexpected error syncing user {}", userId, e);
      }
    }
  }

  public int triggerSync(UUID userId) {
    List<CloudConnectionEntity> active =
        connectionRepository.findByOwnerUserIdAndStatus(userId, CloudConnectionStatus.ACTIVE.name());
    Dataset dataset =
        datasetService
            .getCurrentCycleDataset(userId)
            .orElseThrow(
                () ->
                    new CannotSyncExpiredDatasetException(
                        "Cannot sync a dataset that belongs to a previous billing cycle"));
    String key = billingS3Service.getDatasetDir(userId, dataset);
    List<CloudConnectionPollRequest> requests =
        active.stream()
            .map(
                e -> {
                  CloudConnection dto = connectionMapper.mapToDomain(e);
                  Map<String, String> credentials =
                      encryptionService.decrypt(e.getEncryptedCredentials());
                  return new CloudConnectionPollRequest(dto, credentials, key);
                })
            .toList();
    log.info("Triggering sync for {} active connections for user: {}", active.size(), userId);

    sendToEventBridge(requests);
    return active.size();
  }

  private void sendToEventBridge(List<CloudConnectionPollRequest> requests) {
    if (requests.isEmpty()) {
      return;
    }

    try {
      String jsonPayload = objectMapper.writeValueAsString(requests);

      PutEventsRequestEntry entry =
          PutEventsRequestEntry.builder()
              .source("com.azeem.blueprint")
              .detailType("PollStorageTrigger")
              .detail(jsonPayload)
              .build();

      eventBridgeClient.putEvents(PutEventsRequest.builder().entries(entry).build());
    } catch (Exception e) {
      throw new CloudPollRequestException(e.getMessage(), e);
    }
  }
}
