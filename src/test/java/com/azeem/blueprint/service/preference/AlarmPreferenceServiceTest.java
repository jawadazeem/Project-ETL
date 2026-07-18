/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.preference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.preference.AlarmThresholdPreferenceEntity;
import com.azeem.blueprint.mapper.preference.AlarmThresholdPreferenceMapper;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.repository.preference.AlarmPreferenceRepository;
import com.azeem.blueprint.service.alarm.AlarmService;
import com.azeem.blueprint.service.appuser.AppUserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmPreferenceServiceTest {
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private AlarmService alarmService;
  @Mock private AlarmPreferenceRepository alarmPreferenceRepository;
  @Mock private AppUserService appUserService;
  @Mock private AlarmPreferenceQueryService alarmPreferenceQueryService;

  private AlarmPreferenceService alarmPreferenceService;

  @BeforeEach
  void setUp() {
    alarmPreferenceService =
        new AlarmPreferenceService(
            alarmService,
            alarmPreferenceRepository,
            new AlarmThresholdPreferenceMapper(),
            appUserService,
            alarmPreferenceQueryService);
  }

  @Test
  void shouldReturnPreferenceFromQueryService() {
    AlarmThresholdPreference expected = preference();
    when(alarmPreferenceQueryService.getPreference(USER_ID)).thenReturn(expected);

    AlarmThresholdPreference preference = alarmPreferenceService.getPreference(USER_ID);

    assertThat(preference).isEqualTo(expected);
    verify(alarmPreferenceQueryService).getPreference(USER_ID);
  }

  @Test
  void shouldSavePreferenceForUserAndRecomputeAllUserAlarms() {
    AppUserEntity user = new AppUserEntity();
    user.setId(USER_ID);
    AlarmThresholdPreference preference =
        new AlarmThresholdPreference(
            null,
            USER_ID,
            new AlarmThresholdPreference.Provider(30000),
            new AlarmThresholdPreference.Individual(1500, 2500, 6000),
            new AlarmThresholdPreference.Account(600000, 800000));

    when(appUserService.findOrCreateGuest(USER_ID)).thenReturn(user);
    when(alarmPreferenceQueryService.getPreference(USER_ID)).thenReturn(preference());
    when(alarmPreferenceRepository.findByOwnerUserId(USER_ID)).thenReturn(Optional.empty());
    when(alarmPreferenceRepository.save(any(AlarmThresholdPreferenceEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AlarmThresholdPreference saved =
        alarmPreferenceService.updatePreferenceAndRecompute(USER_ID, preference);

    assertThat(saved.ownerUserId()).isEqualTo(USER_ID);
    assertThat(saved.provider().monthlyLimit()).isEqualTo(30000);
    verify(alarmPreferenceRepository).save(any(AlarmThresholdPreferenceEntity.class));
    verify(alarmService).recompute(eq(USER_ID), any(AlarmThresholdPreference.class), eq(saved));
  }

  // TODO: FIX MISMATCH
  //  @Test
  //  void shouldSavePreferenceForUserAndRecomputeAlarms() {
  //    AppUserEntity user = new AppUserEntity();
  //    user.setId(USER_ID);
  //    when(appUserService.findOrCreateGuest(USER_ID)).thenReturn(user);
  //    when(alarmPreferenceRepository.findByOwnerUserId(USER_ID)).thenReturn(Optional.empty());
  //    when(alarmPreferenceRepository.save(any(AlarmThresholdPreferenceEntity.class)))
  //        .thenAnswer(invocation -> invocation.getArgument(0));
  //
  //    AlarmThresholdPreference saved =
  //        alarmPreferenceService.updatePreferenceAndRecompute(
  //            USER_ID,
  //            new AlarmThresholdPreference(
  //                null,
  //                USER_ID,
  //                new AlarmThresholdPreference.Provider(30000),
  //                new AlarmThresholdPreference.Individual(1500, 2500, 6000),
  //                new AlarmThresholdPreference.Account(600000, 800000)));
  //
  //    assertThat(saved.ownerUserId()).isEqualTo(USER_ID);
  //    assertThat(saved.provider().monthlyLimit()).isEqualTo(30000);
  //    verify(alarmPreferenceRepository).save(any(AlarmThresholdPreferenceEntity.class));
  //    verify(alarmService).recompute(saved);
  //  }

  //  @Test
  //  void shouldUpdateExistingPreferenceAndRecomputeAlarms() {
  //    AppUserEntity user = new AppUserEntity();
  //    user.setId(USER_ID);
  //    AlarmThresholdPreferenceEntity existing = new AlarmThresholdPreferenceEntity();
  //    existing.setOwnerUser(user);
  //    existing.setProviderMonthlyLimit(25000);
  //    existing.setIndividualLow(1000);
  //    existing.setIndividualMedium(2000);
  //    existing.setIndividualHigh(5000);
  //    existing.setAccountLow(500000);
  //    existing.setAccountHigh(750000);
  //
  //    when(appUserService.findOrCreateGuest(USER_ID)).thenReturn(user);
  //
  // when(alarmPreferenceRepository.findByOwnerUserId(USER_ID)).thenReturn(Optional.of(existing));
  //    when(alarmPreferenceRepository.save(existing)).thenReturn(existing);
  //
  //    AlarmThresholdPreference saved =
  //        alarmPreferenceService.updatePreferenceAndRecompute(
  //            USER_ID,
  //            new AlarmThresholdPreference(
  //                null,
  //                USER_ID,
  //                new AlarmThresholdPreference.Provider(35000),
  //                new AlarmThresholdPreference.Individual(1750, 2750, 6500),
  //                new AlarmThresholdPreference.Account(650000, 850000)));
  //
  //    assertThat(saved.provider().monthlyLimit()).isEqualTo(35000);
  //    assertThat(saved.individual().low()).isEqualTo(1750);
  //    assertThat(saved.individual().medium()).isEqualTo(2750);
  //    assertThat(saved.individual().high()).isEqualTo(6500);
  //    assertThat(saved.account().low()).isEqualTo(650000);
  //    assertThat(saved.account().high()).isEqualTo(850000);
  //    verify(alarmPreferenceRepository).save(existing);
  //    verify(alarmService).recompute(saved);
  //  }

  private AlarmThresholdPreference preference() {
    return new AlarmThresholdPreference(
        null,
        USER_ID,
        new AlarmThresholdPreference.Provider(25000),
        new AlarmThresholdPreference.Individual(1000, 2000, 5000),
        new AlarmThresholdPreference.Account(500000, 750000));
  }
}
