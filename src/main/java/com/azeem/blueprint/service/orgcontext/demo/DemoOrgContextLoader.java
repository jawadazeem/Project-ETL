/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.orgcontext.demo;

import com.azeem.blueprint.exception.core.OrgContextDocumentIngestionException;
import com.azeem.blueprint.repository.OrgContextDocumentRepository;
import com.azeem.blueprint.service.appuser.AppUserService;
import com.azeem.blueprint.service.orgcontext.OrgContextQueryService;
import com.azeem.blueprint.util.CustomMultipartFile;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DemoOrgContextLoader {
  Logger log = LoggerFactory.getLogger(DemoOrgContextLoader.class);
  private final OrgContextQueryService orgContextQueryService;
  private final OrgContextDocumentRepository orgContextDocumentRepository;
  private final AppUserService appUserService;
  private final UUID DEMO_ORG_CONTEXT_DATA_USER_ID = new UUID(0L, 0L);

  public DemoOrgContextLoader(
      OrgContextQueryService orgContextQueryService,
      OrgContextDocumentRepository orgContextDocumentRepository,
      AppUserService appUserService) {
    this.orgContextQueryService = orgContextQueryService;
    this.orgContextDocumentRepository = orgContextDocumentRepository;
    this.appUserService = appUserService;
  }

  public synchronized void loadDemoData() {
    if (isLoaded()) {
      log.warn("Demo OrgContext Docs already loaded, cannot load again.");
      return;
    }

    appUserService.findOrCreateGuest(DEMO_ORG_CONTEXT_DATA_USER_ID);

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    Resource[] resources = null;
    try {
      resources = resolver.getResources("classpath:demo-knowledge/**");
    } catch (IOException e) {
      throw new OrgContextDocumentIngestionException(e);
    }

    for (Resource resource : resources) {

      if (resource.isReadable()) {
        log.info("Found resource: {}", resource.getFilename());

        try {
          MultipartFile customMultipartFile =
              createMultipartFileFromResource(resource, resource.getContentAsByteArray());
          orgContextQueryService.ingestDocuments(
              DEMO_ORG_CONTEXT_DATA_USER_ID, customMultipartFile);
        } catch (IOException e) {
          throw new OrgContextDocumentIngestionException(e);
        }
      }
    }

    log.info("Demo OrgContext Docs have been successfully loaded.");
  }

  private boolean isLoaded() {
    return orgContextDocumentRepository.countByOwnerUserId(DEMO_ORG_CONTEXT_DATA_USER_ID) > 0;
  }

  private MultipartFile createMultipartFileFromResource(Resource resource, byte[] contents)
      throws IOException {
    return new CustomMultipartFile(
        contents, resource.getFilename(), resource.getFilename(), "text/markdown");
  }
}
