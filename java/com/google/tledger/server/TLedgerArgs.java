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

package com.google.tledger.server;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class TLedgerArgs {
  @Parameter(
      names = {"--help", "-h"},
      help = true,
      description = "Display help information")
  private boolean help;

  @Parameter(names = "--local", description = "Run in local mode without cloud dependencies")
  private boolean local = false;

  @Parameter(
      names = "--ledger-bucket-prefix",
      description = "Prefix for the S3 bucket for the ledger")
  private String ledgerBucketPrefix = "tldgr-ledger";

  @Parameter(names = "--mbs-kms-key-suffix", description = "Suffix (alias) of the MBS KMS key")
  private String mbsKmsKeySuffix = "alias/tldgr-key-encryption-key";

  @Parameter(
      names = "--cert-backup-bucket-prefix",
      description = "Prefix for the S3 bucket for PES public artifacts")
  private String certBackupBucketPrefix = "tldgr-root-cert-backup";

  @Parameter(
      names = "--key-backup-bucket-prefix",
      description = "Prefix for the MBS S3 key backup bucket")
  private String keyBackupBucketPrefix = "tldgr-root-key-backup";

  public boolean isHelp() {
    return help;
  }

  public boolean isLocalMode() {
    return local;
  }

  public String getLedgerBucketPrefix() {
    return ledgerBucketPrefix;
  }

  public String getMbsKmsKeySuffix() {
    return mbsKmsKeySuffix;
  }

  public String getKeyBackupBucketPrefix() {
    return keyBackupBucketPrefix;
  }

  public String getCertBackupBucketPrefix() {
    return certBackupBucketPrefix;
  }
}
