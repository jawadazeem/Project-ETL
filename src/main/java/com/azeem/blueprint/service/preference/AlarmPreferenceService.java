/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.preference;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.preference.AlarmThresholdPreferenceEntity;
import com.azeem.blueprint.mapper.preference.AlarmThresholdPreferenceMapper;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.repository.preference.AlarmPreferenceRepository;
import com.azeem.blueprint.service.alarm.AlarmService;
import com.azeem.blueprint.service.appuser.AppUserService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmPreferenceService {
  private final AlarmService alarmService;
  private final AlarmPreferenceRepository alarmPreferenceRepository;
  private final AlarmThresholdPreferenceMapper alarmThresholdPreferenceMapper;
  private final AlarmPreferenceQueryService alarmPreferenceQueryService;
  private final AppUserService appUserService;

  public AlarmPreferenceService(
      AlarmService alarmService,
      AlarmPreferenceRepository alarmPreferenceRepository,
      AlarmThresholdPreferenceMapper alarmThresholdPreferenceMapper,
      AppUserService appUserService,
      AlarmPreferenceQueryService alarmPreferenceQueryService) {
    this.alarmService = alarmService;
    this.alarmPreferenceRepository = alarmPreferenceRepository;
    this.alarmThresholdPreferenceMapper = alarmThresholdPreferenceMapper;
    this.appUserService = appUserService;
    this.alarmPreferenceQueryService = alarmPreferenceQueryService;
  }

  public AlarmThresholdPreference createPreferenceAndRecompute(
      UUID ownerUserId, AlarmThresholdPreference preference) {
    AlarmThresholdPreference oldPreference = alarmPreferenceQueryService.getPreference(ownerUserId);
    AlarmThresholdPreference savedPreference = savePreference(ownerUserId, preference);
    alarmService.recompute(ownerUserId, oldPreference, savedPreference);
    return savedPreference;
  }

  public AlarmThresholdPreference updatePreferenceAndRecompute(
      UUID ownerUserId, AlarmThresholdPreference preference) {
    return createPreferenceAndRecompute(ownerUserId, preference);
  }

  @Transactional
  public AlarmThresholdPreference savePreference(
      UUID ownerUserId, AlarmThresholdPreference preference) {
    AppUserEntity ownerUser = appUserService.findOrCreateGuest(ownerUserId);
    AlarmThresholdPreferenceEntity entity =
        alarmPreferenceRepository
            .findByOwnerUserId(ownerUserId)
            .orElseGet(
                () ->
                    alarmThresholdPreferenceMapper.mapToEntity(
                        normalizePreference(ownerUserId, preference), ownerUser));

    entity.setOwnerUser(ownerUser);
    alarmThresholdPreferenceMapper.updateEntity(entity, preference);
    return alarmThresholdPreferenceMapper.mapToDomain(alarmPreferenceRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<AlarmThresholdPreference> listPreferences() {
    return alarmPreferenceQueryService.listPreferences();
  }

  @Transactional(readOnly = true)
  public AlarmThresholdPreference getPreference(UUID ownerUserId) {
    return alarmPreferenceQueryService.getPreference(ownerUserId);
  }

  @Transactional
  public void deletePreference(UUID ownerUserId) {
    alarmPreferenceRepository.deleteByOwnerUserId(ownerUserId);
  }

  private AlarmThresholdPreference normalizePreference(
      UUID ownerUserId, AlarmThresholdPreference preference) {
    return new AlarmThresholdPreference(
        preference.id(),
        ownerUserId,
        preference.provider(),
        preference.individual(),
        preference.account());
  }
}
