/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.CorporateInfoEntity;
import com.azeem.blueprint.mapper.CorporateInfoMapper;
import com.azeem.blueprint.model.report.CorporateInfo;
import com.azeem.blueprint.model.report.CorporateInfoRequest;
import com.azeem.blueprint.repository.CorporateInfoRepository;
import com.azeem.blueprint.service.AppUser.AppUserService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CorporateInfoServiceTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private CorporateInfoRepository corporateInfoRepository;
  @Mock private CorporateInfoMapper corporateInfoMapper;
  @Mock private AppUserService appUserService;

  @InjectMocks private CorporateInfoService corporateInfoService;

  @Test
  @DisplayName("Returns corporate info when it exists for user")
  void shouldReturnCorporateInfoWhenExists() {
    CorporateInfoEntity entity = new CorporateInfoEntity();
    CorporateInfo domain = makeCorporateInfo();

    when(corporateInfoRepository.findByUserId(USER_ID)).thenReturn(Optional.of(entity));
    when(corporateInfoMapper.mapToDomain(entity)).thenReturn(domain);

    Optional<CorporateInfo> result = corporateInfoService.getCorporateInfo(USER_ID);

    assertThat(result).isPresent();
    assertThat(result.get().companyName()).isEqualTo("Acme Corp");
  }

  @Test
  @DisplayName("Returns empty when no corporate info exists")
  void shouldReturnEmptyWhenNoCorporateInfo() {
    when(corporateInfoRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    Optional<CorporateInfo> result = corporateInfoService.getCorporateInfo(USER_ID);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Creates corporate info on first upsert")
  void shouldCreateCorporateInfoOnFirstUpsert() {
    CorporateInfoRequest request = new CorporateInfoRequest("Acme Corp", null, null, null, null, null, null, null, null, null);
    AppUserEntity user = new AppUserEntity();
    CorporateInfoEntity newEntity = new CorporateInfoEntity();
    CorporateInfo domain = makeCorporateInfo();

    when(corporateInfoRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(appUserService.findOrCreateGuest(USER_ID)).thenReturn(user);
    when(corporateInfoMapper.createEntity(request, user)).thenReturn(newEntity);
    when(corporateInfoRepository.save(newEntity)).thenReturn(newEntity);
    when(corporateInfoMapper.mapToDomain(newEntity)).thenReturn(domain);

    CorporateInfo result = corporateInfoService.upsertCorporateInfo(USER_ID, request);

    assertThat(result.companyName()).isEqualTo("Acme Corp");
    verify(corporateInfoRepository).save(newEntity);
  }

  @Test
  @DisplayName("Updates corporate info on subsequent upsert")
  void shouldUpdateCorporateInfoOnSubsequentUpsert() {
    CorporateInfoRequest request = new CorporateInfoRequest("New Name", null, null, null, null, null, null, null, null, null);
    CorporateInfoEntity existing = new CorporateInfoEntity();
    CorporateInfo domain = makeCorporateInfo();

    when(corporateInfoRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
    when(corporateInfoRepository.save(existing)).thenReturn(existing);
    when(corporateInfoMapper.mapToDomain(existing)).thenReturn(domain);

    CorporateInfo result = corporateInfoService.upsertCorporateInfo(USER_ID, request);

    verify(corporateInfoMapper).applyRequest(request, existing);
    verify(corporateInfoRepository).save(existing);
    assertThat(result).isNotNull();
  }

  private CorporateInfo makeCorporateInfo() {
    return new CorporateInfo(
        UUID.randomUUID(), USER_ID, "Acme Corp",
        "123 Main St", null, "Springfield", "IL", "62701", "US",
        "555-0100", "info@acme.com", null,
        Instant.now(), Instant.now());
  }
}
