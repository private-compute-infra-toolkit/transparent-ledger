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

package com.google.tledger.server;

import com.google.common.flogger.FluentLogger;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.grpc.GrpcMeterIdPrefixFunction;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.server.metric.MetricCollectingService;
import com.linecorp.armeria.server.prometheus.PrometheusExpositionService;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** An Armeria server that hosts the TransparentLedgerService with gRPC and REST support. */
public class TLedgerServer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Server server;
  private final HealthManager healthManager;
  private final int port;

  public TLedgerServer(
      int port, TransparentLedgerGrpcHandler service, PrometheusMeterRegistry meterRegistry) {
    this.port = port;
    this.healthManager = new HealthManager();

    final GrpcService grpcService =
        GrpcService.builder()
            .addService(service)
            .addService(ProtoReflectionService.newInstance())
            .addService(healthManager.getGrpcHealthService())
            .enableHttpJsonTranscoding(true)
            .build();

    this.server =
        Server.builder()
            .http(port)
            .meterRegistry(meterRegistry)
            .service(
                grpcService,
                MetricCollectingService.newDecorator(
                    GrpcMeterIdPrefixFunction.of("tledger.server")))
            .service(
                "/healthz",
                (ctx, req) -> {
                  if (healthManager.isServing()) {
                    return HttpResponse.of(HttpStatus.OK);
                  }
                  return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                })
            .service(
                "/metrics", PrometheusExpositionService.of(meterRegistry.getPrometheusRegistry()))
            .decorator(LoggingService.newDecorator())
            .build();
  }

  public CompletableFuture<Void> start() {
    return server
        .start()
        .thenRun(
            () -> {
              logger.atInfo().log("TLedger server started listening on port: %d", port);
              healthManager.setStatus(ServingStatus.SERVING);
            });
  }

  public CompletableFuture<Void> stop() {
    healthManager.setStatus(ServingStatus.NOT_SERVING);
    return server.stop();
  }

  public int port() {
    return server.activeLocalPort();
  }

  public void blockUntilShutdown() throws InterruptedException {
    server.blockUntilShutdown();
  }

  public Server getServer() {
    return server;
  }

  public static class HealthManager {
    private final HealthStatusManager healthStatusManager;
    private final AtomicReference<ServingStatus> currentStatus;

    public HealthManager() {
      this.healthStatusManager = new HealthStatusManager();
      this.currentStatus = new AtomicReference<>(ServingStatus.UNKNOWN);
    }

    public synchronized void setStatus(ServingStatus status) {
      currentStatus.set(status);
      healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, status);
    }

    public boolean isServing() {
      return currentStatus.get() == ServingStatus.SERVING;
    }

    public io.grpc.BindableService getGrpcHealthService() {
      return healthStatusManager.getHealthService();
    }
  }
}
