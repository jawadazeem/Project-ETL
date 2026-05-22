/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.AppUser;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.mapper.AppUserMapper;
import com.azeem.blueprint.model.user.AppUser;
import com.azeem.blueprint.repository.AppUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {
  private final AppUserRepository appUserRepository;
  private final AppUserMapper appUserMapper;

  public AppUserService(AppUserRepository appUserRepository, AppUserMapper appUserMapper) {
    this.appUserRepository = appUserRepository;
    this.appUserMapper = appUserMapper;
  }

  public AppUser getAppUserById(UUID userId) {
    return appUserMapper.mapToDomain(appUserRepository.getReferenceById(userId));
  }

  public AppUserEntity getAppUserEntityById(UUID userId) {
    return appUserRepository.getReferenceById(userId);
  }

  public AppUserEntity findOrCreateGuest(UUID userId) {
    return appUserRepository
        .findById(userId)
        .map(this::normalizeGuest)
        .orElseGet(
            () -> {
              AppUserEntity guest = new AppUserEntity();
              guest.setId(userId);
              guest.setProvider("guest");
              guest.setProviderSubject(userId.toString());
              guest.setEmail("guest@blueprint.local");
              guest.setDisplayName("Guest");
              guest.setRole("GUEST");
              guest.setCreatedAt(Instant.now());
              return appUserRepository.save(guest);
            });
  }

  private AppUserEntity normalizeGuest(AppUserEntity user) {
    boolean changed = false;

    if (user.getProvider() == null) {
      user.setProvider("guest");
      changed = true;
    }
    if (user.getProviderSubject() == null) {
      user.setProviderSubject(user.getId().toString());
      changed = true;
    }
    if (user.getDisplayName() == null) {
      user.setDisplayName("Guest");
      changed = true;
    }
    if (user.getRole() == null) {
      user.setRole("GUEST");
      changed = true;
    }

    return changed ? appUserRepository.save(user) : user;
  }
}
