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

package com.google.tledger.testing;

import com.google.tledger.domain.model.Entry;
import com.google.tledger.domain.ports.Ledger;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLedger implements Ledger {
  ConcurrentHashMap<String, Entry> fakeLedger;

  public InMemoryLedger() {
    fakeLedger = new ConcurrentHashMap<>();
  }

  @Override
  public void write(Entry entry, String name) {
    fakeLedger.put(name, entry);
  }

  @Override
  public Optional<Entry> read(String name) {
    return Optional.ofNullable(fakeLedger.get(name));
  }
}
