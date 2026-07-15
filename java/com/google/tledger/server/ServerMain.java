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
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

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
    PrometheusMeterRegistry meterRegistry = injector.getInstance(PrometheusMeterRegistry.class);
    CertificateValidityReporter validityReporter =
        injector.getInstance(CertificateValidityReporter.class);
    validityReporter.startAsync();

    int port = 50051;
    TLedgerServer tledgerServer = new TLedgerServer(port, service, meterRegistry);

    tledgerServer.start().join();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.err.println("*** shutting down Armeria server since JVM is shutting down");
                  tledgerServer.stop().join();
                  validityReporter.stopAsync();
                  System.err.println("*** server shut down");
                }));

    try {
      tledgerServer.blockUntilShutdown();
    } catch (InterruptedException e) {
      logger.atInfo().log("Server interrupted.");
    }
  }
}
