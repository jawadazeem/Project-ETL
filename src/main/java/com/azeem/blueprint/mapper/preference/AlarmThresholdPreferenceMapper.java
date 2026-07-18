/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper.preference;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.preference.AlarmThresholdPreferenceEntity;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import org.springframework.stereotype.Component;

@Component
public class AlarmThresholdPreferenceMapper {

  public AlarmThresholdPreferenceEntity mapToEntity(
      AlarmThresholdPreference preference, AppUserEntity ownerUser) {
    AlarmThresholdPreferenceEntity entity = new AlarmThresholdPreferenceEntity();
    entity.setId(preference.id());
    entity.setOwnerUser(ownerUser);
    updateEntity(entity, preference);
    return entity;
  }

  public void updateEntity(
      AlarmThresholdPreferenceEntity entity, AlarmThresholdPreference preference) {
    entity.setProviderMonthlyLimit(preference.provider().monthlyLimit());
    entity.setIndividualLow(preference.individual().low());
    entity.setIndividualMedium(preference.individual().medium());
    entity.setIndividualHigh(preference.individual().high());
    entity.setAccountLow(preference.account().low());
    entity.setAccountHigh(preference.account().high());
  }

  public AlarmThresholdPreference mapToDomain(AlarmThresholdPreferenceEntity entity) {
    return new AlarmThresholdPreference(
        entity.getId(),
        entity.getOwnerUser().getId(),
        new AlarmThresholdPreference.Provider(entity.getProviderMonthlyLimit()),
        new AlarmThresholdPreference.Individual(
            entity.getIndividualLow(), entity.getIndividualMedium(), entity.getIndividualHigh()),
        new AlarmThresholdPreference.Account(entity.getAccountLow(), entity.getAccountHigh()));
  }
}
