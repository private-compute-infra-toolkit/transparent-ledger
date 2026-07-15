/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.tledger.adapters;

import static com.google.common.truth.Truth.assertThat;

import com.google.tledger.domain.metric.Status;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SystemMetricsTest {

  private PrometheusMeterRegistry registry;
  private SystemMetrics systemMetrics;

  @Before
  public void setUp() {
    registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    systemMetrics = new SystemMetrics(registry);
  }

  @Test
  public void constructor_preRegistersAllCountersWithZeroValue() {
    for (Status status : Status.values()) {
      double value =
          registry
              .get("tledger.ledgerWrites")
              .tag("status", status.name().toLowerCase())
              .counter()
              .count();
      assertThat(value).isEqualTo(0.0);
    }
  }

  @Test
  public void incrementLedgerWrites_succeeds() {
    systemMetrics.incrementLedgerWrites(Status.SUCCESS);
    systemMetrics.incrementLedgerWrites(Status.SUCCESS);
    systemMetrics.incrementLedgerWrites(Status.FAILURE);

    double successVal =
        registry.get("tledger.ledgerWrites").tag("status", "success").counter().count();
    double failureVal =
        registry.get("tledger.ledgerWrites").tag("status", "failure").counter().count();

    assertThat(successVal).isEqualTo(2.0);
    assertThat(failureVal).isEqualTo(1.0);
  }

  @Test
  public void increment_nullInputs_safelyIgnored() {
    systemMetrics.incrementLedgerWrites(null);

    double successVal =
        registry.get("tledger.ledgerWrites").tag("status", "success").counter().count();

    assertThat(successVal).isEqualTo(0.0);
  }

  @Test
  public void setRootCertificateValidity_registersGaugeOnFirstCall() {
    assertThat(registry.find("tledger.root_certificate_validity_seconds").gauge()).isNull();

    systemMetrics.setRootCertificateValidity(Duration.ofMinutes(5));

    io.micrometer.core.instrument.Gauge gauge =
        registry.get("tledger.root_certificate_validity_seconds").gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(300.0);

    systemMetrics.setRootCertificateValidity(Duration.ofMinutes(10));
    assertThat(gauge.value()).isEqualTo(600.0);
  }
}
