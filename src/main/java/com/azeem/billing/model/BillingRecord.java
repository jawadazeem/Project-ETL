package com.azeem.billing.model;

/**
 * Represents the detailed billing record for a single account.
 */

public record BillingRecord(String accountName, String senatorId, String state, String phoneNumber,
                            String billingPeriod, int minutesUsed, double dataGbUsed, int smsCount,
                            double totalCharge) {

    @Override
    public String toString() {
        return accountName + ", " + senatorId + ", " + state + ", " + phoneNumber + ", " + billingPeriod + ", " + minutesUsed + ", " + dataGbUsed + ", " + smsCount + ", " + totalCharge;
    }
}
