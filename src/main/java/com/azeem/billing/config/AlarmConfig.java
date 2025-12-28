package com.azeem.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alarm")
public class AlarmConfig {

    private Department department;
    private Individual individual;
    private Account account;

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Individual getIndividual() { return individual; }
    public void setIndividual(Individual individual) { this.individual = individual; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public static class Department {
        private double monthlyLimit;
        public double getMonthlyLimit() { return monthlyLimit; }
        public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }
    }

    public static class Individual {
        private double low;
        private double medium;
        private double high;
        public double getLow() { return low; }
        public void setLow(double low) { this.low = low; }
        public double getMedium() { return medium; }
        public void setMedium(double medium) { this.medium = medium; }
        public double getHigh() { return high; }
        public void setHigh(double high) { this.high = high; }
    }

    public static class Account {
        private double low;
        private double high;
        public double getLow() { return low; }
        public void setLow(double low) { this.low = low; }
        public double getHigh() { return high; }
        public void setHigh(double high) { this.high = high; }
    }
}
