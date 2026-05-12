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

import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import com.google.tledger.domain.ports.EntryIdProvider;
import java.util.UUID;

public class Sha256EntryIdProvider implements EntryIdProvider {
  private static final String DELIMITER = "_";

  @Override
  public String get(ByteString message) {
    String hashHex = Hashing.sha256().hashBytes(message.toByteArray()).toString();

    String uid = UUID.randomUUID().toString();
    return hashHex + DELIMITER + uid;
  }
}
