/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.CorporateInfoEntity;
import com.azeem.blueprint.mapper.CorporateInfoMapper;
import com.azeem.blueprint.model.report.CorporateInfo;
import com.azeem.blueprint.model.report.CorporateInfoRequest;
import com.azeem.blueprint.repository.CorporateInfoRepository;
import com.azeem.blueprint.service.AppUser.AppUserService;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorporateInfoService {
  private static final Logger log = LoggerFactory.getLogger(CorporateInfoService.class);

  private final CorporateInfoRepository corporateInfoRepository;
  private final CorporateInfoMapper corporateInfoMapper;
  private final AppUserService appUserService;

  public CorporateInfoService(
      CorporateInfoRepository corporateInfoRepository,
      CorporateInfoMapper corporateInfoMapper,
      AppUserService appUserService) {
    this.corporateInfoRepository = corporateInfoRepository;
    this.corporateInfoMapper = corporateInfoMapper;
    this.appUserService = appUserService;
  }

  @Transactional(readOnly = true)
  public Optional<CorporateInfo> getCorporateInfo(UUID userId) {
    return corporateInfoRepository.findByUserId(userId).map(corporateInfoMapper::mapToDomain);
  }

  @Transactional
  public CorporateInfo upsertCorporateInfo(UUID userId, CorporateInfoRequest request) {
    CorporateInfoEntity entity =
        corporateInfoRepository
            .findByUserId(userId)
            .map(
                existing -> {
                  corporateInfoMapper.applyRequest(request, existing);
                  return existing;
                })
            .orElseGet(
                () -> {
                  AppUserEntity user = appUserService.findOrCreateGuest(userId);
                  return corporateInfoMapper.createEntity(request, user);
                });

    CorporateInfoEntity saved = corporateInfoRepository.save(entity);
    log.info("Corporate info saved for user: {}", userId);
    return corporateInfoMapper.mapToDomain(saved);
  }
}
