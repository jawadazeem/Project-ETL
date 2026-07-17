/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository.dataset;

import java.util.UUID;

public interface DatasetBillingPeriodProjection {
  String getBillingPeriod();

  UUID getId();
}
