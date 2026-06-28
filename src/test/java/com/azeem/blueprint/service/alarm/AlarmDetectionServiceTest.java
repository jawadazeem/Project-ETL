/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.azeem.blueprint.config.AlarmConfig;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.alarm.AlarmScope;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.BillingRecord;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlarmDetectionServiceTest {

  private AlarmDetectionService detectionService;

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String BILLING_PERIOD = "2026-01";

  @BeforeEach
  void setUp() {
    AlarmConfig config = new AlarmConfig();

    AlarmConfig.Provider provider = new AlarmConfig.Provider();
    provider.setMonthlyLimit(7500.0);
    config.setProvider(provider);

    AlarmConfig.Individual individual = new AlarmConfig.Individual();
    individual.setLow(250.0);
    individual.setMedium(370.0);
    individual.setHigh(500.0);
    config.setIndividual(individual);

    AlarmConfig.Account account = new AlarmConfig.Account();
    account.setLow(45000.0);
    account.setHigh(60000.0);
    config.setAccount(account);

    detectionService = new AlarmDetectionService(config);
  }

  private BillingRecord record(String cloudProvider, String resourceId, double charge) {
    return new BillingRecord(
        DATASET_ID,
        "Acme Corp",
        resourceId,
        cloudProvider,
        BILLING_PERIOD,
        0,
        0.0,
        0,
        charge,
        "EC2",
        "test resource");
  }

  @Test
  @DisplayName("No alarms when all charges are well within limits")
  void noAlarms_whenAllChargesAreNormal() {
    List<BillingRecord> records =
        List.of(record("AWS", "i-001", 100.0), record("GCP", "proj-002", 150.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms).isEmpty();
  }

  @Test
  @DisplayName("Empty record list produces no alarms")
  void detectAlarms_emptyRecords_returnsEmptyList() {
    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, List.of(), BILLING_PERIOD);

    assertThat(alarms).isEmpty();
  }

  @Test
  @DisplayName("Provider alarm fired when provider total exceeds monthly limit")
  void providerAlarm_whenProviderTotalExceedsMonthlyLimit() {
    // 4000 + 4000 = 8000 > 7500
    List<BillingRecord> records =
        List.of(record("AWS", "i-001", 4000.0), record("AWS", "i-002", 4000.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> providerAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.PROVIDER).toList();
    assertThat(providerAlarms).hasSize(1);
    assertThat(providerAlarms.get(0).datasetId()).isEqualTo(DATASET_ID);
    assertThat(providerAlarms.get(0).billingPeriod()).isEqualTo(BILLING_PERIOD);
  }

  @Test
  @DisplayName("No provider alarm when total is below monthly limit")
  void noProviderAlarm_whenTotalBelowLimit() {
    List<BillingRecord> records =
        List.of(record("AWS", "i-001", 3000.0), record("AWS", "i-002", 3000.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.PROVIDER).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Records with null cloud provider are skipped for provider alarm check")
  void recordWithNullProvider_doesNotTriggerProviderAlarm() {
    BillingRecord nullProviderRecord =
        new BillingRecord(
            DATASET_ID, "Acme", "i-001", null, BILLING_PERIOD, 0, 0.0, 0, 9000.0, "EC2", "desc");

    List<Alarm> alarms =
        detectionService.detectAlarms(DATASET_ID, List.of(nullProviderRecord), BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.PROVIDER).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Resource LOW alarm when charge is between 250 and 370")
  void resourceAlarm_lowSeverity_whenChargeInLowRange() {
    List<BillingRecord> records = List.of(record("AWS", "i-001", 300.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> resourceAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.RESOURCE).toList();
    assertThat(resourceAlarms).hasSize(1);
    assertThat(resourceAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.LOW);
    assertThat(resourceAlarms.get(0).resourceId()).isEqualTo("i-001");
  }

  @Test
  @DisplayName("Resource MEDIUM alarm when charge is between 370 and 500")
  void resourceAlarm_mediumSeverity_whenChargeInMediumRange() {
    List<BillingRecord> records = List.of(record("AWS", "i-001", 400.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> resourceAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.RESOURCE).toList();
    assertThat(resourceAlarms).hasSize(1);
    assertThat(resourceAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.MEDIUM);
  }

  @Test
  @DisplayName("Resource HIGH alarm when charge is 500 or more")
  void resourceAlarm_highSeverity_whenChargeAboveHigh() {
    List<BillingRecord> records = List.of(record("AWS", "i-001", 600.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> resourceAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.RESOURCE).toList();
    assertThat(resourceAlarms).hasSize(1);
    assertThat(resourceAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.HIGH);
  }

  @Test
  @DisplayName("No resource alarm when charge is below low threshold")
  void noResourceAlarm_whenChargeBelowLow() {
    List<BillingRecord> records = List.of(record("AWS", "i-001", 100.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.RESOURCE).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Account LOW alarm when grand total is between 45000 and 60000")
  void accountAlarmLow_whenGrandTotalBetweenLowAndHigh() {
    // 50000 > 45000 && < 60000 → accountLow
    List<BillingRecord> records = List.of(record("AWS", "i-001", 50000.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> accountAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.ACCOUNT).toList();
    assertThat(accountAlarms).hasSize(1);
    assertThat(accountAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.LOW);
    assertThat(accountAlarms.get(0).alarmType()).contains("LOW");
  }

  @Test
  @DisplayName("Account HIGH alarm when grand total is 60000 or more")
  void accountAlarmHigh_whenGrandTotalExceedsHigh() {
    // 70000 >= 60000 → accountHigh
    List<BillingRecord> records = List.of(record("AWS", "i-001", 70000.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> accountAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.ACCOUNT).toList();
    assertThat(accountAlarms).hasSize(1);
    assertThat(accountAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.HIGH);
  }

  @Test
  @DisplayName("No account alarm when grand total is below low threshold")
  void noAccountAlarm_whenGrandTotalBelowLow() {
    List<BillingRecord> records =
        List.of(record("AWS", "i-001", 100.0), record("GCP", "proj-002", 200.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.ACCOUNT).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Multiple alarm types can be generated from a single run")
  void multipleAlarmTypes_generatedTogether() {
    // provider total 8000 > 7500, resource 600 >= 500
    List<BillingRecord> records =
        List.of(
            record("AWS", "i-001", 4000.0),
            record("AWS", "i-002", 4000.0),
            record("GCP", "proj-003", 600.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().anyMatch(a -> a.alarmScope() == AlarmScope.PROVIDER)).isTrue();
    assertThat(alarms.stream().anyMatch(a -> a.alarmScope() == AlarmScope.RESOURCE)).isTrue();
  }
}
