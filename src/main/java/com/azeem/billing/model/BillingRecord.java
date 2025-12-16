package com.azeem.billing.model;

/**
 * Represents the detailed billing record for a single account.
 */

public record BillingRecord(String accountName, String employeeId, String department, String phoneNumber,
                            String billingPeriod, int minutesUsed, double dataGbUsed, int smsCount,
                            double totalCharge) {

    @Override
    public String toString() {
        return accountName + ", " + employeeId + ", " + department + ", " + phoneNumber + ", " + billingPeriod + ", " + minutesUsed + ", " + dataGbUsed + ", " + smsCount + ", " + totalCharge;
    }
}
