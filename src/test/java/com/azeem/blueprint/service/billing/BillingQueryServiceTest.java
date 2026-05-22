/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.exception.core.BillingDataNotFoundException;
import com.azeem.blueprint.exception.web.QueryLimitExceededException;
import com.azeem.blueprint.mapper.BillingRecordMapper;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.model.billing.Department;
import com.azeem.blueprint.repository.BillingRecordRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BillingQueryServiceTest {

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private BillingRecordRepository repository;
  @Mock private BillingRecordMapper mapper;

  @InjectMocks private BillingQueryService service;

  private BillingRecordEntity entity1;
  private BillingRecordEntity entity2;
  private BillingRecord domain1;
  private BillingRecord domain2;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "maxTopNLimit", 100);

    entity1 = new BillingRecordEntity();
    entity1.setId(1L);
    entity1.setDepartment(Department.IT.name());
    entity1.setAccountName("Mark Wojick");
    entity1.setEmployeeId("EMP4");
    entity1.setPhoneNumber("7034305396");
    entity1.setBillingPeriod("2026-01");
    entity1.setMinutesUsed(100);
    entity1.setDataGbUsed(20);
    entity1.setSmsCount(100);
    entity1.setTotalCharge(70.00);

    domain1 =
        new BillingRecord(
            DATASET_ID,
            entity1.getAccountName(),
            entity1.getEmployeeId(),
            entity1.getDepartment(),
            entity1.getPhoneNumber(),
            entity1.getBillingPeriod(),
            entity1.getMinutesUsed(),
            entity1.getDataGbUsed(),
            entity1.getSmsCount(),
            entity1.getTotalCharge());

    entity2 = new BillingRecordEntity();
    entity2.setId(2L);
    entity2.setDepartment(Department.OPERATIONS.name());
    entity2.setAccountName("Seth Alberts");
    entity2.setEmployeeId("EMP5");
    entity2.setPhoneNumber("2020397483");
    entity2.setBillingPeriod("2026-01");
    entity2.setMinutesUsed(150);
    entity2.setDataGbUsed(15);
    entity2.setSmsCount(70);
    entity2.setTotalCharge(80.00);

    domain2 =
        new BillingRecord(
            DATASET_ID,
            entity2.getAccountName(),
            entity2.getEmployeeId(),
            entity2.getDepartment(),
            entity2.getPhoneNumber(),
            entity2.getBillingPeriod(),
            entity2.getMinutesUsed(),
            entity2.getDataGbUsed(),
            entity2.getSmsCount(),
            entity2.getTotalCharge());
  }

  @Test
  void shouldReturnDistinctBillingPeriodsForDataset() {
    when(repository.findDistinctBillingPeriodByDatasetId(DATASET_ID))
        .thenReturn(List.of("2026-02", "2026-01"));

    List<String> periods = service.getDistinctBillingPeriodsById(DATASET_ID);

    assertThat(periods).containsExactly("2026-02", "2026-01");
    verify(repository).findDistinctBillingPeriodByDatasetId(DATASET_ID);
  }

  @Test
  void shouldReturnPagedBillingRecordsForValidPageAndSize() {
    Page<BillingRecordEntity> entityPage =
        new PageImpl<>(List.of(entity1), PageRequest.of(0, 5), 1);
    when(repository.findByDatasetId(eq(DATASET_ID), any(Pageable.class))).thenReturn(entityPage);
    when(mapper.mapToDomain(entity1)).thenReturn(domain1);

    Page<BillingRecord> result = service.getAllRecordsInDataset(DATASET_ID, 0, 5);

    assertThat(result.getContent()).containsExactly(domain1);
    assertThat(result.getNumber()).isEqualTo(0);
    assertThat(result.getSize()).isEqualTo(5);
    assertThat(result.getTotalElements()).isEqualTo(1);

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findByDatasetId(eq(DATASET_ID), captor.capture());
    assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("department").descending());
    verify(mapper).mapToDomain(entity1);
  }

  @Test
  void shouldReturnEmptyPageWhenNoDataExists() {
    when(repository.findByDatasetId(eq(DATASET_ID), any(Pageable.class))).thenReturn(Page.empty());

    Page<BillingRecord> result = service.getAllRecordsInDataset(DATASET_ID, 0, 5);

    assertThat(result.getContent()).isEmpty();
    verify(mapper, never()).mapToDomain(any());
  }

  @Test
  void shouldReturnRecordsForGivenBillingPeriod() {
    Page<BillingRecordEntity> entityPage =
        new PageImpl<>(List.of(entity1), PageRequest.of(0, 5), 1);
    when(repository.findByDatasetIdAndBillingPeriod(
            eq(DATASET_ID), eq("2026-01"), any(Pageable.class)))
        .thenReturn(entityPage);

    service.getDatasetRecordsByPeriod(DATASET_ID, "2026-01", 0, 5);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository)
        .findByDatasetIdAndBillingPeriod(eq(DATASET_ID), eq("2026-01"), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    verify(mapper).mapToDomain(entity1);
  }

  @Test
  void shouldThrowWhenNoDataExistsForBillingPeriod() {
    when(repository.findByDatasetIdAndBillingPeriod(
            eq(DATASET_ID), anyString(), any(Pageable.class)))
        .thenReturn(Page.empty());

    assertThatThrownBy(() -> service.getDatasetRecordsByPeriod(DATASET_ID, "2026-01", 0, 5))
        .isInstanceOf(BillingDataNotFoundException.class);
  }

  @Test
  void shouldReturnRecordsByDepartmentCaseInsensitive() {
    Page<BillingRecordEntity> entityPage =
        new PageImpl<>(List.of(entity1), PageRequest.of(0, 5), 1);
    when(repository.findByDatasetIdAndDepartmentIgnoreCase(
            eq(DATASET_ID), any(String.class), any(Pageable.class)))
        .thenReturn(entityPage);

    service.getRecordsByDepartmentInDataset(DATASET_ID, "fInanCe", 0, 5);

    ArgumentCaptor<String> deptCaptor = ArgumentCaptor.forClass(String.class);
    verify(repository)
        .findByDatasetIdAndDepartmentIgnoreCase(eq(DATASET_ID), deptCaptor.capture(), any());
    assertThat(deptCaptor.getValue()).isEqualTo("fInanCe");
    verify(mapper).mapToDomain(entity1);
  }

  @Test
  void shouldReturnTopNRecordsSortedByTotalChargeDescending() {
    Page<BillingRecordEntity> entityPage =
        new PageImpl<>(List.of(entity1), PageRequest.of(0, 1), 1);
    when(repository.findByDatasetId(eq(DATASET_ID), any(Pageable.class))).thenReturn(entityPage);
    when(repository.count()).thenReturn(1L);

    service.getTopNRecordsInDataset(DATASET_ID, 1);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findByDatasetId(eq(DATASET_ID), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    verify(mapper).mapToDomain(entity1);
  }

  @Test
  void shouldThrowWhenTopNExceedsTotalRecordCount() {
    when(repository.count()).thenReturn(1L);

    assertThatThrownBy(() -> service.getTopNRecordsInDataset(DATASET_ID, 2))
        .isInstanceOf(QueryLimitExceededException.class);
  }

  @Test
  void shouldGenerateSummaryForDataset() {
    Page<BillingRecordEntity> entityPage =
        new PageImpl<>(List.of(entity1), PageRequest.of(0, 1000), 1);
    when(repository.findByDatasetId(eq(DATASET_ID), any(Pageable.class))).thenReturn(entityPage);
    when(mapper.mapToDomain(entity1)).thenReturn(domain1);

    BillingSummary summary = service.generateSummary(DATASET_ID);

    assertThat(summary.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldThrowWhenNoBillingDataExistsForSummary() {
    when(repository.findByDatasetId(eq(DATASET_ID), any(Pageable.class))).thenReturn(Page.empty());

    assertThatThrownBy(() -> service.generateSummary(DATASET_ID))
        .isInstanceOf(BillingDataNotFoundException.class);
  }

  @Test
  void shouldGenerateSummaryAcrossMultiplePages() {
    Page<BillingRecordEntity> firstPage = new PageImpl<>(List.of(entity1), PageRequest.of(0, 1), 2);
    Page<BillingRecordEntity> secondPage =
        new PageImpl<>(List.of(entity2), PageRequest.of(1, 1), 2);

    when(repository.findByDatasetId(eq(DATASET_ID), any(Pageable.class)))
        .thenReturn(firstPage)
        .thenReturn(secondPage);
    when(mapper.mapToDomain(entity1)).thenReturn(domain1);
    when(mapper.mapToDomain(entity2)).thenReturn(domain2);

    BillingSummary summary = service.generateSummary(DATASET_ID);

    assertThat(summary.getTotalRecords()).isEqualTo(2);
    assertThat(summary.getTotalCharges()).isEqualTo(150.00);
  }
}
