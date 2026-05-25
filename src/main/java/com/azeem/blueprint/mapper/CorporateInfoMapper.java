/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.CorporateInfoEntity;
import com.azeem.blueprint.model.report.CorporateInfo;
import com.azeem.blueprint.model.report.CorporateInfoRequest;
import org.springframework.stereotype.Component;

@Component
public class CorporateInfoMapper {

  public CorporateInfo mapToDomain(CorporateInfoEntity entity) {
    return new CorporateInfo(
        entity.getId(),
        entity.getUser().getId(),
        entity.getCompanyName(),
        entity.getAddressLine1(),
        entity.getAddressLine2(),
        entity.getCity(),
        entity.getState(),
        entity.getZipCode(),
        entity.getCountry(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getLogoUrl(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public void applyRequest(CorporateInfoRequest request, CorporateInfoEntity entity) {
    entity.setCompanyName(request.companyName());
    entity.setAddressLine1(request.addressLine1());
    entity.setAddressLine2(request.addressLine2());
    entity.setCity(request.city());
    entity.setState(request.state());
    entity.setZipCode(request.zipCode());
    entity.setCountry(request.country());
    entity.setPhone(request.phone());
    entity.setEmail(request.email());
    entity.setLogoUrl(request.logoUrl());
  }

  public CorporateInfoEntity createEntity(CorporateInfoRequest request, AppUserEntity user) {
    CorporateInfoEntity entity = new CorporateInfoEntity();
    entity.setUser(user);
    applyRequest(request, entity);
    return entity;
  }
}
