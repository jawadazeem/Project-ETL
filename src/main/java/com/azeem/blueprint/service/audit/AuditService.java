package com.azeem.blueprint.service.audit;

import com.azeem.blueprint.model.audit.AuditResponse;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.service.billing.BillingQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final BillingQueryService billingQueryService;
  private final RestTemplate restTemplate;
  private final String auditServiceUrl;

  public AuditService(
      BillingQueryService billingQueryService,
      @Value("${audit.service.url:http://audit-service:5001}") String auditServiceUrl) {
    this.billingQueryService = billingQueryService;
    this.restTemplate = new RestTemplate();
    this.auditServiceUrl = auditServiceUrl;
  }

  public AuditResponse runAudit(UUID datasetId, String billingPeriod) {
    List<BillingRecord> records =
        billingQueryService.getAllRecordsForExport(datasetId, billingPeriod);

    log.info(
        "Running audit on {} records for dataset {} period {}",
        records.size(),
        datasetId,
        billingPeriod);

    List<Map<String, Object>> recordPayload =
        records.stream()
            .map(
                r -> {
                  Map<String, Object> map = new HashMap<>();
                  map.put("serviceName", r.serviceName());
                  map.put("cloudProvider", r.cloudProvider());
                  map.put("totalCharge", r.totalCharge());
                  map.put("accountName", r.accountName());
                  map.put("resourceId", r.resourceId());
                  map.put("billingPeriod", r.billingPeriod());
                  map.put("description", r.description());
                  return map;
                })
            .toList();

    Map<String, Object> request = Map.of("records", recordPayload);

    String url = auditServiceUrl + "/audit";
    return restTemplate.postForObject(url, request, AuditResponse.class);
  }
}