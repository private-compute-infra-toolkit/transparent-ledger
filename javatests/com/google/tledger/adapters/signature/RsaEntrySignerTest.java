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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.mbs.domain.AttestationToken;
import com.google.mbs.domain.SelfSignedBundle;
import com.google.mbs.domain.SelfSignedBundleWithEpoch;
import com.google.mbs.domain.TrustPackage;
import com.google.protobuf.ByteString;
import com.google.tledger.domain.model.SignatureRecord;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RsaEntrySignerTest {

  private static final AttestationToken DUMMY_TOKEN =
      AttestationToken.fromBytes("token".getBytes());
  private static final X509Certificate DUMMY_CERT = mock(X509Certificate.class);
  private static final int INITIAL_EPOCH_ID = 5;

  private RsaEntrySigner rsaEntrySigner;
  private KeyPair testRsaKeyPair;
  private AtomicReference<SelfSignedBundle> bundleRef;
  private AtomicInteger epochRef;

  @Before
  public void setUp() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    testRsaKeyPair = keyGen.generateKeyPair();

    bundleRef =
        new AtomicReference<>(
            new SelfSignedBundle(DUMMY_CERT, testRsaKeyPair.getPrivate(), DUMMY_TOKEN));
    epochRef = new AtomicInteger(INITIAL_EPOCH_ID);
    rsaEntrySigner =
        new RsaEntrySigner(
            () ->
                new TrustPackage(
                    List.of(new SelfSignedBundleWithEpoch(epochRef.get(), bundleRef.get())),
                    List.of()));
  }

  private boolean verifySignature(ByteString content, ByteString signature, PublicKey publicKey)
      throws Exception {
    Signature sigVerifier = Signature.getInstance("SHA256withRSA");
    sigVerifier.initVerify(publicKey);
    sigVerifier.update(content.toByteArray());
    return sigVerifier.verify(signature.toByteArray());
  }

  @Test
  public void sign_shouldProduceValidSignature() throws Exception {
    ByteString content = ByteString.copyFromUtf8("Test Content for Signing");

    SignatureRecord record = rsaEntrySigner.sign(content);

    assertThat(record).isNotNull();
    assertThat(record.algorithm()).isEqualTo("SHA256withRSA");
    assertThat(record.signature()).isNotEmpty();
    assertThat(record.keyId()).isEqualTo(String.valueOf(INITIAL_EPOCH_ID));

    assertThat(verifySignature(content, record.signature(), testRsaKeyPair.getPublic())).isTrue();
  }

  @Test
  public void sign_emptyContent_shouldProduceValidSignature() throws Exception {
    ByteString content = ByteString.EMPTY;

    SignatureRecord record = rsaEntrySigner.sign(content);

    assertThat(record).isNotNull();
    assertThat(record.algorithm()).isEqualTo("SHA256withRSA");
    assertThat(record.signature()).isNotEmpty();

    assertThat(verifySignature(content, record.signature(), testRsaKeyPair.getPublic())).isTrue();
  }

  @Test
  public void sign_nullContent_throwsNpe() {
    assertThrows(NullPointerException.class, () -> rsaEntrySigner.sign(null));
  }

  @Test
  public void sign_invalidKeyType() throws Exception {
    KeyPairGenerator dsaKeyGen = KeyPairGenerator.getInstance("DSA");
    dsaKeyGen.initialize(1024);
    KeyPair dsaKeyPair = dsaKeyGen.generateKeyPair();
    RsaEntrySigner dsaSigner =
        new RsaEntrySigner(
            () ->
                new TrustPackage(
                    List.of(
                        new SelfSignedBundleWithEpoch(
                            0,
                            new SelfSignedBundle(
                                DUMMY_CERT, dsaKeyPair.getPrivate(), DUMMY_TOKEN))),
                    List.of()));

    ByteString content = ByteString.copyFromUtf8("Another Test Content");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> dsaSigner.sign(content));

    assertThat(exception).hasCauseThat().isInstanceOf(InvalidKeyException.class);
  }

  @Test
  public void sign_dynamicKeyRotation_usesNewKey() throws Exception {
    ByteString content = ByteString.copyFromUtf8("Rotation Content");

    SignatureRecord firstRecord = rsaEntrySigner.sign(content);
    assertThat(verifySignature(content, firstRecord.signature(), testRsaKeyPair.getPublic()))
        .isTrue();

    // Rotate key
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair newKeyPair = keyGen.generateKeyPair();
    bundleRef.set(new SelfSignedBundle(DUMMY_CERT, newKeyPair.getPrivate(), DUMMY_TOKEN));
    epochRef.set(INITIAL_EPOCH_ID + 1);

    SignatureRecord secondRecord = rsaEntrySigner.sign(content);
    assertThat(secondRecord.keyId()).isEqualTo(String.valueOf(INITIAL_EPOCH_ID + 1));
    assertThat(verifySignature(content, secondRecord.signature(), newKeyPair.getPublic())).isTrue();
    assertThat(verifySignature(content, secondRecord.signature(), testRsaKeyPair.getPublic()))
        .isFalse();
  }
}
