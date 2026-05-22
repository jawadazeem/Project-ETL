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
import com.azeem.blueprint.model.billing.Department;
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

    AlarmConfig.Department dept = new AlarmConfig.Department();
    dept.setMonthlyLimit(7500.0);
    config.setDepartment(dept);

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

  private BillingRecord record(String dept, String empId, String phone, double charge) {
    return new BillingRecord(
        DATASET_ID, "Acme Corp", empId, dept, phone, BILLING_PERIOD, 0, 0.0, 0, charge);
  }

  @Test
  @DisplayName("No alarms when all charges are well within limits")
  void noAlarms_whenAllChargesAreNormal() {
    List<BillingRecord> records =
        List.of(
            record("ENGINEERING", "E1", "555-0001", 100.0),
            record("SALES", "E2", "555-0002", 150.0));

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
  @DisplayName("Department alarm fired when department total exceeds monthly limit")
  void departmentAlarm_whenDeptTotalExceedsMonthlyLimit() {
    // 4000 + 4000 = 8000 > 7500
    List<BillingRecord> records =
        List.of(
            record("ENGINEERING", "E1", "555-0001", 4000.0),
            record("ENGINEERING", "E2", "555-0002", 4000.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> deptAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.DEPARTMENT).toList();
    assertThat(deptAlarms).hasSize(1);
    assertThat(deptAlarms.get(0).department()).isEqualTo(Department.ENGINEERING);
    assertThat(deptAlarms.get(0).datasetId()).isEqualTo(DATASET_ID);
    assertThat(deptAlarms.get(0).billingPeriod()).isEqualTo(BILLING_PERIOD);
  }

  @Test
  @DisplayName("No department alarm when total is below monthly limit")
  void noDepartmentAlarm_whenTotalBelowLimit() {
    List<BillingRecord> records =
        List.of(
            record("ENGINEERING", "E1", "555-0001", 3000.0),
            record("ENGINEERING", "E2", "555-0002", 3000.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.DEPARTMENT).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Records with null department are skipped for department alarm check")
  void recordWithNullDepartment_doesNotTriggerDeptAlarm() {
    BillingRecord nullDeptRecord =
        new BillingRecord(
            DATASET_ID, "Acme", "E1", null, "555-0001", BILLING_PERIOD, 0, 0.0, 0, 9000.0);

    List<Alarm> alarms =
        detectionService.detectAlarms(DATASET_ID, List.of(nullDeptRecord), BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.DEPARTMENT).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Individual LOW alarm when charge is between 250 and 370")
  void individualAlarm_lowSeverity_whenChargeInLowRange() {
    List<BillingRecord> records = List.of(record("IT", "E1", "555-0001", 300.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> individualAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.INDIVIDUAL).toList();
    assertThat(individualAlarms).hasSize(1);
    assertThat(individualAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.LOW);
    assertThat(individualAlarms.get(0).employeeId()).isEqualTo("E1");
    assertThat(individualAlarms.get(0).phoneNumber()).isEqualTo("555-0001");
  }

  @Test
  @DisplayName("Individual MEDIUM alarm when charge is between 370 and 500")
  void individualAlarm_mediumSeverity_whenChargeInMediumRange() {
    List<BillingRecord> records = List.of(record("IT", "E1", "555-0001", 400.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> individualAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.INDIVIDUAL).toList();
    assertThat(individualAlarms).hasSize(1);
    assertThat(individualAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.MEDIUM);
  }

  @Test
  @DisplayName("Individual HIGH alarm when charge is 500 or more")
  void individualAlarm_highSeverity_whenChargeAboveHigh() {
    List<BillingRecord> records = List.of(record("IT", "E1", "555-0001", 600.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    List<Alarm> individualAlarms =
        alarms.stream().filter(a -> a.alarmScope() == AlarmScope.INDIVIDUAL).toList();
    assertThat(individualAlarms).hasSize(1);
    assertThat(individualAlarms.get(0).alarmSeverity()).isEqualTo(AlarmSeverity.HIGH);
  }

  @Test
  @DisplayName("No individual alarm when charge is below low threshold")
  void noIndividualAlarm_whenChargeBelowLow() {
    List<BillingRecord> records = List.of(record("IT", "E1", "555-0001", 100.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.INDIVIDUAL).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Account LOW alarm when grand total is between 45000 and 60000")
  void accountAlarmLow_whenGrandTotalBetweenLowAndHigh() {
    // 50000 > 45000 && < 60000 → accountLow
    List<BillingRecord> records = List.of(record("FINANCE", "E1", "555-0001", 50000.0));

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
    List<BillingRecord> records = List.of(record("FINANCE", "E1", "555-0001", 70000.0));

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
        List.of(
            record("ENGINEERING", "E1", "555-0001", 100.0),
            record("SALES", "E2", "555-0002", 200.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().filter(a -> a.alarmScope() == AlarmScope.ACCOUNT).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Multiple alarm types can be generated from a single run")
  void multipleAlarmTypes_generatedTogether() {
    // dept total 8000 > 7500, individual 600 >= 500
    List<BillingRecord> records =
        List.of(
            record("ENGINEERING", "E1", "555-0001", 4000.0),
            record("ENGINEERING", "E2", "555-0002", 4000.0),
            record("SALES", "E3", "555-0003", 600.0));

    List<Alarm> alarms = detectionService.detectAlarms(DATASET_ID, records, BILLING_PERIOD);

    assertThat(alarms.stream().anyMatch(a -> a.alarmScope() == AlarmScope.DEPARTMENT)).isTrue();
    assertThat(alarms.stream().anyMatch(a -> a.alarmScope() == AlarmScope.INDIVIDUAL)).isTrue();
  }
}
