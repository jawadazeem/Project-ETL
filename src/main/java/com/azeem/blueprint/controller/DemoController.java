/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.service.dataset.demo.DemoDatasetLoader;
import com.azeem.blueprint.service.orgcontext.demo.DemoOrgContextLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
  private static final Logger log = LoggerFactory.getLogger(DemoController.class);
  private final DemoDatasetLoader demoDatasetLoader;
  private final DemoOrgContextLoader demoOrgContextLoader;

  public DemoController(
      DemoDatasetLoader demoDatasetLoader, DemoOrgContextLoader demoOrgContextLoader) {
    this.demoDatasetLoader = demoDatasetLoader;
    this.demoOrgContextLoader = demoOrgContextLoader;
  }

  @PostMapping("/demo-dataset")
  public ResponseEntity<String> loadDemoBillingData() {
    log.info("POST /demo-dataset called. Triggering demo data ingestion.");
    demoDatasetLoader.loadDemoData();
    return ResponseEntity.ok("Demo billing data loaded. You can now use the analytics endpoints.");
  }

  @PostMapping("/demo-orgcontext")
  public ResponseEntity<String> loadDemoOrgContextData() {
    log.info("POST /demo-orgcontext called. Triggering demo org context data ingestion.");
    demoOrgContextLoader.loadDemoData();
    return ResponseEntity.ok(
        "Demo org context data loaded. You can now use the LLM query endpoints.");
  }
}
