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

package com.google.tledger.adapters.signature;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.mbs.qualifier.MbsRoot;
import com.google.protobuf.ByteString;
import com.google.tledger.domain.model.SignatureRecord;
import com.google.tledger.domain.ports.EntrySigner;
import jakarta.inject.Inject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

public class RsaEntrySigner implements EntrySigner {
  private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

  private final PrivateKey privateKey;

  @Inject
  RsaEntrySigner(@MbsRoot PrivateKey privateKey) {
    this.privateKey = checkNotNull(privateKey, "Private key cannot be null.");
  }

  public SignatureRecord sign(ByteString content) {
    checkNotNull(content, "Content to be signed cannot be null.");

    Signature signature = getSignatureInstance();
    init(signature);
    ByteString signatureBytes = signContent(content, signature);

    return new SignatureRecord(signatureBytes, SIGNATURE_ALGORITHM);
  }

  private Signature getSignatureInstance() {
    try {
      return Signature.getInstance(SIGNATURE_ALGORITHM);
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private void init(Signature signature) {
    try {
      signature.initSign(privateKey);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException("The key returned by MBS should always be valid!", e);
    }
  }

  private ByteString signContent(ByteString content, Signature signature) {
    try {
      signature.update(content.toByteArray());
      return ByteString.copyFrom(signature.sign());
    } catch (SignatureException e) {
      throw new IllegalStateException(e);
    }
  }
}
