/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@DisplayName("DatasetRepository Integration Tests")
class DatasetRepositoryTest {

  @Autowired private DatasetRepository repository;
  @Autowired private TestEntityManager entityManager;

  private AppUserEntity user1;
  private AppUserEntity user2;
  private DatasetEntity dataset1;
  private DatasetEntity dataset2;

  @BeforeEach
  void setUp() {
    user1 = new AppUserEntity();
    user1.setProviderSubject("google|user-one");
    entityManager.persist(user1);

    user2 = new AppUserEntity();
    user2.setProviderSubject("google|user-two");
    entityManager.persist(user2);

    dataset1 = new DatasetEntity();
    dataset1.setOwnerUser(user1);
    dataset1.setSourceFilename("billing-jan.csv");
    dataset1.setStatus("READY");
    dataset1.setUploadedAt(Instant.now());
    entityManager.persist(dataset1);

    dataset2 = new DatasetEntity();
    dataset2.setOwnerUser(user1);
    dataset2.setSourceFilename("billing-feb.csv");
    dataset2.setStatus("PENDING_INGESTION");
    dataset2.setUploadedAt(Instant.now());
    entityManager.persist(dataset2);

    entityManager.flush();
  }

  @Test
  @DisplayName("findByOwnerUserId returns all datasets belonging to the owner")
  void findByOwnerUserId_returnsDatasetsForOwner() {
    List<DatasetEntity> result = repository.findByOwnerUserId(user1.getId());

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(DatasetEntity::getSourceFilename)
        .containsExactlyInAnyOrder("billing-jan.csv", "billing-feb.csv");
  }

  @Test
  @DisplayName("findByOwnerUserId returns empty list when owner has no datasets")
  void findByOwnerUserId_returnsEmpty_forOwnerWithNoDatasets() {
    List<DatasetEntity> result = repository.findByOwnerUserId(user2.getId());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findByIdAndOwnerUserId returns dataset when both ID and owner match")
  void findByIdAndOwnerUserId_returnsDataset_whenOwnerMatches() {
    Optional<DatasetEntity> result =
        repository.findByIdAndOwnerUserId(dataset1.getId(), user1.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getSourceFilename()).isEqualTo("billing-jan.csv");
  }

  @Test
  @DisplayName("findByIdAndOwnerUserId returns empty when owner does not match")
  void findByIdAndOwnerUserId_returnsEmpty_whenOwnerDoesNotMatch() {
    Optional<DatasetEntity> result =
        repository.findByIdAndOwnerUserId(dataset1.getId(), user2.getId());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("deleteByIdAndOwnerUserId deletes the dataset and returns 1")
  void deleteByIdAndOwnerUserId_deletesDataset_whenOwnerMatches() {
    int deleted = repository.deleteByIdAndOwnerUserId(dataset1.getId(), user1.getId());

    assertThat(deleted).isEqualTo(1);
    assertThat(repository.findById(dataset1.getId())).isEmpty();
  }

  @Test
  @DisplayName("deleteByIdAndOwnerUserId returns 0 when owner does not match")
  void deleteByIdAndOwnerUserId_returnsZero_whenOwnerDoesNotMatch() {
    int deleted = repository.deleteByIdAndOwnerUserId(dataset1.getId(), user2.getId());

    assertThat(deleted).isEqualTo(0);
    assertThat(repository.findById(dataset1.getId())).isPresent();
  }
}
