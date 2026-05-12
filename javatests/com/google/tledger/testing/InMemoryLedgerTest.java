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

import static com.google.common.truth.Truth.assertThat;

import com.google.protobuf.ByteString;
import com.google.tledger.domain.model.Entry;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InMemoryLedgerTest {

  private InMemoryLedger ledger;

  @Before
  public void setUp() {
    ledger = new InMemoryLedger();
  }

  @Test
  public void readFromLedger_nonExistentKey_returnsEmpty() {
    Optional<Entry> result = ledger.read("doesNotExist");

    assertThat(result).isEmpty();
  }

  @Test
  public void writeToLedger_andReadFromLedger_returnsSavesAndReturnsContent() {
    String key = "Hi mom";

    Entry content =
        new Entry(
            "Hi mom",
            ByteString.copyFrom("Hi dad", StandardCharsets.UTF_8),
            ByteString.copyFrom("signature", StandardCharsets.UTF_8),
            "crypto");

    ledger.write(content, key);
    Optional<Entry> result = ledger.read(key);

    assertThat(result).hasValue(content);
  }

  @Test
  public void writeToLedger_multipleKeys() {
    String key1 = "one";
    Entry content1 =
        new Entry(
            key1,
            ByteString.copyFrom("one one", StandardCharsets.UTF_8),
            ByteString.copyFrom("signature1", StandardCharsets.UTF_8),
            "crypto1");
    String key2 = "two";
    Entry content2 =
        new Entry(
            key2,
            ByteString.copyFrom("one two", StandardCharsets.UTF_8),
            ByteString.copyFrom("signature2", StandardCharsets.UTF_8),
            "crypto2");

    ledger.write(content1, key1);
    ledger.write(content2, key2);

    Optional<Entry> result1 = ledger.read(key1);
    Optional<Entry> result2 = ledger.read(key2);

    assertThat(result1).hasValue(content1);
    assertThat(result2).hasValue(content2);
  }
}
