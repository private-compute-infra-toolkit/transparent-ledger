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

import com.google.mbs.Metrics.MbsEvent;
import com.google.tledger.domain.metric.Metrics;
import com.google.tledger.domain.metric.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class SystemMetrics implements Metrics, com.google.mbs.Metrics {

  private static final String PREFIX = "tledger.";

  private final PrometheusMeterRegistry registry;
  private final Counter[] ledgerWrites;
  private final AtomicInteger[] mbsStatusValues;
  private final AtomicLong rootCertificateValiditySeconds = new AtomicLong(0);
  private final AtomicBoolean rootCertificateValiditySecondsRegistered = new AtomicBoolean(false);

  @Inject
  public SystemMetrics(PrometheusMeterRegistry registry) {
    this.registry = registry;
    this.ledgerWrites = createCounters(PREFIX + "ledgerWrites", "status", Status.class);
    this.mbsStatusValues = createGauges(PREFIX + "mbsStatus", "status", MbsEvent.class);
  }

  private <E extends Enum<E>> Counter[] createCounters(
      String name, String tagKey, Class<E> enumClass) {
    E[] constants = enumClass.getEnumConstants();
    Counter[] array = new Counter[constants.length];
    for (E item : constants) {
      array[item.ordinal()] =
          Counter.builder(name)
              .tag(tagKey, item.name().toLowerCase())
              .description("Metrics tracking for " + name)
              .register(registry);
    }
    return array;
  }

  private <E extends Enum<E>> AtomicInteger[] createGauges(
      String name, String tagKey, Class<E> enumClass) {
    E[] constants = enumClass.getEnumConstants();
    AtomicInteger[] array = new AtomicInteger[constants.length];
    for (E item : constants) {
      array[item.ordinal()] = new AtomicInteger(0);
      Gauge.builder(name, array[item.ordinal()], AtomicInteger::get)
          .tag(tagKey, item.name().toLowerCase())
          .description("Metrics tracking for " + name)
          .register(registry);
    }
    return array;
  }

  @Override
  public void incrementLedgerWrites(Status status) {
    if (status != null) {
      ledgerWrites[status.ordinal()].increment();
    }
  }

  @Override
  public void recordEvent(MbsEvent event) {
    if (event != null) {
      for (MbsEvent e : MbsEvent.class.getEnumConstants()) {
        mbsStatusValues[e.ordinal()].set(e == event ? 1 : 0);
      }
    }
  }

  @Override
  public void setRootCertificateValidity(Duration remaining) {
    rootCertificateValiditySeconds.set(remaining.toSeconds());
    if (rootCertificateValiditySecondsRegistered.compareAndSet(false, true)) {
      Gauge.builder(
              PREFIX + "root_certificate_validity_seconds",
              rootCertificateValiditySeconds,
              AtomicLong::get)
          .description("Seconds remaining until the root certificate expires")
          .register(registry);
    }
  }
}
