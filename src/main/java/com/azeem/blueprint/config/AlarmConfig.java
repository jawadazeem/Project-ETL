/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alarm")
public class AlarmConfig {

  private Provider provider;
  private Individual individual;
  private Account account;

  public Provider getProvider() {
    return provider;
  }

  public void setProvider(Provider provider) {
    this.provider = provider;
  }

  public Individual getIndividual() {
    return individual;
  }

  public void setIndividual(Individual individual) {
    this.individual = individual;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public static class Provider {
    private double monthlyLimit;

    public double getMonthlyLimit() {
      return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
      this.monthlyLimit = monthlyLimit;
    }
  }

  public static class Individual {
    private double low;
    private double medium;
    private double high;

    public double getLow() {
      return low;
    }

    public void setLow(double low) {
      this.low = low;
    }

    public double getMedium() {
      return medium;
    }

    public void setMedium(double medium) {
      this.medium = medium;
    }

    public double getHigh() {
      return high;
    }

    public void setHigh(double high) {
      this.high = high;
    }
  }

  public static class Account {
    private double low;
    private double high;

    public double getLow() {
      return low;
    }

    public void setLow(double low) {
      this.low = low;
    }

    public double getHigh() {
      return high;
    }

    public void setHigh(double high) {
      this.high = high;
    }
  }
}
