/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.preference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.azeem.blueprint.config.AlarmConfig;
import com.azeem.blueprint.mapper.preference.AlarmThresholdPreferenceMapper;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.repository.preference.AlarmPreferenceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmPreferenceQueryServiceTest {
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private AlarmPreferenceRepository alarmPreferenceRepository;

  private AlarmPreferenceQueryService alarmPreferenceQueryService;

  @BeforeEach
  void setUp() {
    alarmPreferenceQueryService =
        new AlarmPreferenceQueryService(
            alarmPreferenceRepository, new AlarmThresholdPreferenceMapper(), alarmConfig());
  }

  @Test
  void shouldReturnConfiguredDefaultsWhenUserHasNoSavedPreference() {
    when(alarmPreferenceRepository.findByOwnerUserId(USER_ID)).thenReturn(Optional.empty());

    AlarmThresholdPreference preference = alarmPreferenceQueryService.getPreference(USER_ID);

    assertThat(preference.id()).isNull();
    assertThat(preference.ownerUserId()).isEqualTo(USER_ID);
    assertThat(preference.provider().monthlyLimit()).isEqualTo(25000);
    assertThat(preference.individual().low()).isEqualTo(1000);
    assertThat(preference.individual().medium()).isEqualTo(2000);
    assertThat(preference.individual().high()).isEqualTo(5000);
    assertThat(preference.account().low()).isEqualTo(500000);
    assertThat(preference.account().high()).isEqualTo(750000);
  }

  private AlarmConfig alarmConfig() {
    AlarmConfig config = new AlarmConfig();
    AlarmConfig.Provider provider = new AlarmConfig.Provider();
    provider.setMonthlyLimit(25000);
    AlarmConfig.Individual individual = new AlarmConfig.Individual();
    individual.setLow(1000);
    individual.setMedium(2000);
    individual.setHigh(5000);
    AlarmConfig.Account account = new AlarmConfig.Account();
    account.setLow(500000);
    account.setHigh(750000);
    config.setProvider(provider);
    config.setIndividual(individual);
    config.setAccount(account);
    return config;
  }
}
