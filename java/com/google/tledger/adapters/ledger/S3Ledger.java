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

package com.google.tledger.adapters.ledger;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.tledger.adapters.EntryMapper;
import com.google.tledger.annotations.LedgerBucketName;
import com.google.tledger.domain.metric.Metrics;
import com.google.tledger.domain.metric.Status;
import com.google.tledger.domain.model.Entry;
import com.google.tledger.domain.ports.Ledger;
import com.google.tledger.domain.ports.LedgerException;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3Ledger implements Ledger {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final S3Client s3Client;
  private final String bucketName;

  private final JsonFormat.Parser parser;
  private final JsonFormat.Printer printer;
  private final Metrics metrics;

  @Inject
  S3Ledger(
      S3Client s3Client,
      @LedgerBucketName String bucketName,
      JsonFormat.Parser parser,
      JsonFormat.Printer printer,
      Metrics metrics) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    this.parser = parser;
    this.printer = printer;
    this.metrics = metrics;
  }

  @Override
  public void write(Entry entry, String name) {
    PutObjectRequest objectRequest =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(name)
            .contentType("application/json")
            .checksumAlgorithm(ChecksumAlgorithm.SHA256)
            .build();

    String jsonEntry;
    try {
      jsonEntry = printer.print(EntryMapper.toProto(entry));
      s3Client.putObject(objectRequest, RequestBody.fromString(jsonEntry, StandardCharsets.UTF_8));
      metrics.incrementLedgerWrites(Status.SUCCESS);
    } catch (InvalidProtocolBufferException e) {
      metrics.incrementLedgerWrites(Status.FAILURE);
      throw new IllegalStateException(
          "The Entry proto object should never contain unknown Any types!", e);
    } catch (SdkClientException | S3Exception e) {
      metrics.incrementLedgerWrites(Status.FAILURE);
      throw new LedgerException("Failed to write to the ledger for key: " + name, e);
    }
  }

  @Override
  public Optional<Entry> read(String name) {
    GetObjectRequest objectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(name).build();

    String entryJson;
    try {
      entryJson = s3Client.getObject(objectRequest, ResponseTransformer.toBytes()).asUtf8String();
    } catch (NoSuchKeyException e) {
      logger.atInfo().log("S3 Object not found: %s", name);
      return Optional.empty();
    } catch (SdkClientException | S3Exception e) {
      throw new LedgerException("Failed to read from ledger for key: " + name, e);
    }

    com.google.tledger.v1.Entry resultEntry;
    com.google.tledger.v1.Entry.Builder entryBuilder = com.google.tledger.v1.Entry.newBuilder();
    try {
      parser.merge(entryJson, entryBuilder);
      resultEntry = entryBuilder.build();
    } catch (InvalidProtocolBufferException e) {
      throw new LedgerException("Failed to parse entry from ledger for key: " + name, e);
    }

    return Optional.of(EntryMapper.toDomain(resultEntry));
  }
}
