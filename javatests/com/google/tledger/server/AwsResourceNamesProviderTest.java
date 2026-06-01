/*
 * Copyright 2026 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AwsResourceNamesProviderTest {

  @Test
  public void getRecord_returnsCorrectNames() {
    TLedgerArgs args = mock(TLedgerArgs.class);
    when(args.getCertBackupBucketPrefix()).thenReturn("test-cert-backup-prefix");
    when(args.getKeyBackupBucketPrefix()).thenReturn("test-key-backup-prefix");
    when(args.getLedgerBucketPrefix()).thenReturn("test-ledger-bucket-prefix");
    when(args.getMbsKmsKeySuffix()).thenReturn("test-kms-key-suffix");

    AwsInstanceMetadata metadata =
        new AwsInstanceMetadata("us-west-2", "123456789012", "testenv", "testdomain");
    AwsResourceNamesProvider provider = new AwsResourceNamesProvider(args, metadata);

    AwsResourceNames names = provider.getRecord();

    assertEquals("test-cert-backup-prefix-123456789012-us-west-2", names.certBackupBucketName());
    assertEquals("test-key-backup-prefix-123456789012-us-west-2", names.keyBackupBucketName());
    assertEquals("test-ledger-bucket-prefix-123456789012-us-west-2", names.ledgerBucketName());
    assertEquals("arn:aws:kms:us-west-2:123456789012:test-kms-key-suffix", names.kmsKeyArn());
  }
}
