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

package com.google.tledger.adapters.entryid;

import static com.google.common.truth.Truth.assertThat;

import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Sha256EntryIdProviderTest {
  private Sha256EntryIdProvider idProvider;

  @Before
  public void setUp() {
    idProvider = new Sha256EntryIdProvider();
  }

  @Test
  public void createEntryId_validInput_hasCorrectHashPrefix() {
    ByteString input = ByteString.copyFromUtf8("test string");
    // SHA-256 digest of UTF-8 encoded "test string"
    String expectedHash = "d5579c46dfcc7f18207013e65b44e4cb4e2c2298f4ac457ba8f82743f31e930b";

    String result = idProvider.get(input);

    String[] parts = result.split("_");
    String hashPart = parts[0];

    assertThat(parts).hasLength(2);
    assertThat(hashPart).isEqualTo(expectedHash);
  }

  @Test
  public void createEntryId_emptyInput_returnsHashOfEmptyString() {
    ByteString input = ByteString.EMPTY;
    // SHA-256 digest of empty string
    String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    String result = idProvider.get(input);

    assertThat(result).startsWith(expectedHash + "_");
  }

  @Test
  public void createEntryId_sameInputCalledTwice_returnsUniqueIds() {
    ByteString input = ByteString.copyFromUtf8("test string 2");

    String result1 = idProvider.get(input);
    String result2 = idProvider.get(input);

    assertThat(result1).isNotEqualTo(result2);

    String hash1 = result1.split("_")[0];
    String hash2 = result2.split("_")[0];
    assertThat(hash1).isEqualTo(hash2);
  }
}
