/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.preference;

import com.azeem.blueprint.config.AlarmConfig;
import com.azeem.blueprint.mapper.preference.AlarmThresholdPreferenceMapper;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.repository.preference.AlarmPreferenceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlarmPreferenceQueryService {
  private final AlarmPreferenceRepository alarmPreferenceRepository;
  private final AlarmThresholdPreferenceMapper alarmThresholdPreferenceMapper;
  private final AlarmConfig alarmConfig;

  public AlarmPreferenceQueryService(
      AlarmPreferenceRepository alarmPreferenceRepository,
      AlarmThresholdPreferenceMapper alarmThresholdPreferenceMapper,
      AlarmConfig alarmConfig) {
    this.alarmPreferenceRepository = alarmPreferenceRepository;
    this.alarmThresholdPreferenceMapper = alarmThresholdPreferenceMapper;
    this.alarmConfig = alarmConfig;
  }

  public List<AlarmThresholdPreference> listPreferences() {
    return alarmPreferenceRepository.findAll().stream()
        .map(alarmThresholdPreferenceMapper::mapToDomain)
        .toList();
  }

  public AlarmThresholdPreference getPreference(UUID ownerUserId) {
    return alarmPreferenceRepository
        .findByOwnerUserId(ownerUserId)
        .map(alarmThresholdPreferenceMapper::mapToDomain)
        .orElseGet(() -> defaultPreference(ownerUserId));
  }

  private AlarmThresholdPreference defaultPreference(UUID ownerUserId) {
    return new AlarmThresholdPreference(
        null,
        ownerUserId,
        new AlarmThresholdPreference.Provider(alarmConfig.getProvider().getMonthlyLimit()),
        new AlarmThresholdPreference.Individual(
            alarmConfig.getIndividual().getLow(),
            alarmConfig.getIndividual().getMedium(),
            alarmConfig.getIndividual().getHigh()),
        new AlarmThresholdPreference.Account(
            alarmConfig.getAccount().getLow(), alarmConfig.getAccount().getHigh()));
  }
}
