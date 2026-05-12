/*
 * Copyright 2025 Google LLC
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.logging.LoggingService;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.util.concurrent.atomic.AtomicReference;

public class ServerMain {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void main(String[] args) throws Exception {
    TLedgerArgs tledgerArgs = new TLedgerArgs();
    JCommander jc = JCommander.newBuilder().addObject(tledgerArgs).build();

    try {
      jc.parse(args);

      if (tledgerArgs.isHelp()) {
        jc.usage();
        return;
      }
    } catch (ParameterException e) {
      jc.usage();
      throw e;
    }

    logger.atInfo().log("Main running locally: %b", tledgerArgs.isLocalMode());

    AwsInstanceMetadata awsInstanceMetadata = new ImdsClient().getAwsInstanceMetadata();
    logger.atInfo().log(
        "Resolved AWS environment via IMDS: region=%s, accountId=%s",
        awsInstanceMetadata.region(), awsInstanceMetadata.accountId());

    Injector injector = Guice.createInjector(new TLedgerModule(tledgerArgs, awsInstanceMetadata));
    TransparentLedgerGrpcHandler service = injector.getInstance(TransparentLedgerGrpcHandler.class);

    HealthManager healthManager = new HealthManager();

    int port = 50051;
    final GrpcService grpcService =
        GrpcService.builder()
            .addService(service)
            .addService(ProtoReflectionService.newInstance())
            .addService(healthManager.getGrpcHealthService())
            .enableHttpJsonTranscoding(true)
            .build();

    final Server server =
        Server.builder()
            .http(port)
            .service(grpcService)
            .service(
                "/healthz",
                (ctx, req) -> {
                  if (healthManager.isServing()) {
                    return HttpResponse.of(HttpStatus.OK);
                  }
                  return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                })
            .decorator(LoggingService.newDecorator())
            .build();

    server.start().join();
    logger.atInfo().log("TLedger server started listening on port: %d", port);

    healthManager.setStatus(ServingStatus.SERVING);

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.err.println("*** shutting down Armeria server since JVM is shutting down");
                  healthManager.setStatus(ServingStatus.NOT_SERVING);
                  server.stop().join();
                  System.err.println("*** server shut down");
                }));

    try {
      server.blockUntilShutdown();
    } catch (InterruptedException e) {
      logger.atInfo().log("Server interrupted.");
    }
  }

  private static class HealthManager {
    private final HealthStatusManager healthStatusManager;
    private final AtomicReference<ServingStatus> currentStatus;

    HealthManager() {
      this.healthStatusManager = new HealthStatusManager();
      this.currentStatus = new AtomicReference<>(ServingStatus.UNKNOWN);
    }

    synchronized void setStatus(ServingStatus status) {
      currentStatus.set(status);
      healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, status);
    }

    boolean isServing() {
      return currentStatus.get() == ServingStatus.SERVING;
    }

    io.grpc.BindableService getGrpcHealthService() {
      return healthStatusManager.getHealthService();
    }
  }
}
