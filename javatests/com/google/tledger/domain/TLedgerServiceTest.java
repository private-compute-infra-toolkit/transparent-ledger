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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.google.mbs.MeasurementBoundCertificate;
import com.google.mbs.MeasurementBoundCertificateProvider;
import com.google.protobuf.ByteString;
import com.google.tledger.domain.model.Entry;
import com.google.tledger.domain.ports.EntryIdProvider;
import com.google.tledger.domain.ports.Ledger;
import com.google.tledger.server.AwsInstanceMetadata;
import com.google.tledger.server.TLedgerArgs;
import com.google.tledger.server.TLedgerModule;
import com.google.tledger.testing.InMemoryLedger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class TLedgerServiceTest {

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private EntryIdProvider mockIdProvider;
  @Mock private MeasurementBoundCertificateProvider mbcProvider;
  @Mock private MeasurementBoundCertificate certificateBundle;
  private InMemoryLedger inMemoryLedger;
  private KeyPair testRsaKeyPair;

  private TLedgerService service;

  private static final ByteString TEST_MESSAGE = ByteString.copyFromUtf8("bits and bytes");
  private static final String TEST_ENTRY_ID = "test-id";
  private static final String TEST_ENTRY_NAME = "entries/" + TEST_ENTRY_ID;
  private static final String TEST_ALGORITHM_NAME = "SHA256withRSA";
  private static final Entry TEST_ENTRY =
      new Entry(TEST_ENTRY_NAME, TEST_MESSAGE, ByteString.EMPTY, "");

  @Before
  public void setUp() throws Exception {
    inMemoryLedger = new InMemoryLedger();

    TLedgerArgs args = new TLedgerArgs();

    Injector injector =
        Guice.createInjector(
            Modules.override(
                    new TLedgerModule(args, new AwsInstanceMetadata("us-east-1", "123456789012")))
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(EntryIdProvider.class).toInstance(mockIdProvider);
                        bind(Ledger.class).toInstance(inMemoryLedger);
                        bind(MeasurementBoundCertificateProvider.class).toInstance(mbcProvider);
                      }
                    }));
    service = injector.getInstance(TLedgerService.class);

    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    testRsaKeyPair = keyGen.generateKeyPair();

    when(mbcProvider.loadOrGenerateCertificate()).thenReturn(certificateBundle);
    when(certificateBundle.getPrivateKey()).thenReturn(testRsaKeyPair.getPrivate());
  }

  private boolean verifySignature(ByteString content, ByteString signature, PublicKey publicKey) {
    try {
      Signature sigVerifier = Signature.getInstance("SHA256withRSA");
      sigVerifier.initVerify(publicKey);
      sigVerifier.update(content.toByteArray());
      return sigVerifier.verify(signature.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException("Verification failed", e);
    }
  }

  @Test
  public void createEntry_success_writesToLedger() {
    when(mockIdProvider.get(TEST_MESSAGE)).thenReturn(TEST_ENTRY_ID);

    Entry result = service.createEntry(TEST_MESSAGE);

    Optional<Entry> storedEntry = inMemoryLedger.read(TEST_ENTRY_NAME);
    assertThat(storedEntry).isPresent();
    assertThat(storedEntry.get().name()).isEqualTo(TEST_ENTRY_NAME);
    assertThat(storedEntry.get().rawEntry()).isEqualTo(TEST_MESSAGE);
    assertThat(storedEntry.get().signature()).isEqualTo(result.signature());

    assertThat(result.name()).isEqualTo(TEST_ENTRY_NAME);
    assertThat(result.rawEntry()).isEqualTo(TEST_MESSAGE);
    assertThat(result.signingAlgorithm()).isEqualTo(TEST_ALGORITHM_NAME);

    assertThat(verifySignature(TEST_MESSAGE, result.signature(), testRsaKeyPair.getPublic()))
        .isTrue();
  }

  @Test
  public void getEntry_exists_returnsEntry() {
    inMemoryLedger.write(TEST_ENTRY, TEST_ENTRY_NAME);

    Optional<Entry> result = service.getEntry(TEST_ENTRY_NAME);

    assertThat(result).hasValue(TEST_ENTRY);
  }

  @Test
  public void getEntry_notFound_returnsEmpty() {
    Optional<Entry> result = service.getEntry(TEST_ENTRY_NAME);

    assertThat(result).isEmpty();
  }
}
