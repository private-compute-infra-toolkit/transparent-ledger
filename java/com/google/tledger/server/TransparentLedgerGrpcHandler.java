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

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.tledger.adapters.EntryMapper;
import com.google.tledger.domain.TLedger;
import com.google.tledger.domain.model.Entry;
import com.google.tledger.domain.ports.LedgerException;
import com.google.tledger.v1.CreateEntryRequest;
import com.google.tledger.v1.GetEntryRequest;
import com.google.tledger.v1.TransparentLedgerServiceGrpc.TransparentLedgerServiceImplBase;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

public class TransparentLedgerGrpcHandler extends TransparentLedgerServiceImplBase {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final TLedger tLedgerService;

  @Inject
  TransparentLedgerGrpcHandler(TLedger tLedgerService) {
    this.tLedgerService = tLedgerService;
  }

  @Override
  public void createEntry(
      CreateEntryRequest request, StreamObserver<com.google.tledger.v1.Entry> responseObserver) {
    logger.atInfo().log("createEntry invoked.");

    if (request.getEntry().getRawEntry().isEmpty()) {
      Status status = Status.INVALID_ARGUMENT.withDescription("Raw entry should not be empty");
      responseObserver.onError(status.asRuntimeException());
      return;
    }
    com.google.tledger.v1.Entry entry;
    try {
      entry = EntryMapper.toProto(tLedgerService.createEntry(request.getEntry().getRawEntry()));
    } catch (LedgerException e) {
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).withCause(e).asRuntimeException());
      return;
    }
    responseObserver.onNext(entry);
    responseObserver.onCompleted();
  }

  @Override
  public void getEntry(
      GetEntryRequest request, StreamObserver<com.google.tledger.v1.Entry> responseObserver) {
    logger.atInfo().log("getEntry invoked.");

    if (request.getName().isEmpty()) {
      Status status = Status.INVALID_ARGUMENT.withDescription("Entry name should not be empty");
      responseObserver.onError(status.asRuntimeException());
      return;
    }

    Optional<Entry> entry;
    try {
      entry = tLedgerService.getEntry(request.getName());
    } catch (LedgerException e) {
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).withCause(e).asRuntimeException());
      return;
    }

    if (entry.isPresent()) {
      responseObserver.onNext(EntryMapper.toProto(entry.get()));
      responseObserver.onCompleted();
    } else {
      Status status =
          Status.NOT_FOUND.withDescription("Entry not found for ID: " + request.getName());
      responseObserver.onError(status.asRuntimeException());
    }
  }
}
