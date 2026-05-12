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

package com.google.tledger.domain;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.ByteString;
import com.google.tledger.domain.model.Entry;
import com.google.tledger.domain.model.SignatureRecord;
import com.google.tledger.domain.ports.*;
import jakarta.inject.Inject;
import java.util.Optional;

public class TLedgerService implements TLedger {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String ENTRY_NAME_PREFIX = "entries/";

  private final EntryIdProvider idProvider;
  private final Ledger ledger;
  private final EntrySigner entrySigner;

  @Inject
  TLedgerService(EntryIdProvider idProvider, Ledger ledger, EntrySigner entrySigner) {
    this.idProvider = idProvider;
    this.ledger = ledger;
    this.entrySigner = entrySigner;
  }

  public Entry createEntry(ByteString message) {
    String entryID = idProvider.get(message);
    String entryName = ENTRY_NAME_PREFIX + entryID;
    logger.atInfo().log("Creating entry with ID: %s, size: %d", entryID, message.size());

    SignatureRecord signatureInfo = entrySigner.sign(message);
    Entry result =
        new Entry(entryName, message, signatureInfo.signature(), signatureInfo.algorithm());

    ledger.write(result, entryName);

    return result;
  }

  public Optional<Entry> getEntry(String name) {
    logger.atInfo().log("Getting entry: %s", name);

    Optional<Entry> entry = ledger.read(name);
    if (entry.isEmpty()) {
      logger.atInfo().log("Entry not found: %s", name);
    }

    return entry;
  }
}
