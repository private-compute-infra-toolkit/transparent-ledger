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

import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.kmsclient.KmsClientInterface;
import com.google.kmsclient.aws.AwsKmsClientModule;
import com.google.mbs.DummyMeasurementBoundCertificateProvider;
import com.google.mbs.KeyBackupBucketPropertiesFactory;
import com.google.mbs.KmsMeasurementBoundCertificateProvider;
import com.google.mbs.MbsCertificateFactory;
import com.google.mbs.MeasurementBoundCertificateProvider;
import com.google.mbs.attestationcollection.AttestationCollector;
import com.google.mbs.attestationcollection.aws.AwsAttestationModule;
import com.google.protobuf.util.JsonFormat;
import com.google.tledger.adapters.entryid.Sha256EntryIdProvider;
import com.google.tledger.adapters.ledger.S3Ledger;
import com.google.tledger.adapters.signature.RsaEntrySigner;
import com.google.tledger.annotations.LedgerBucketName;
import com.google.tledger.domain.TLedger;
import com.google.tledger.domain.TLedgerService;
import com.google.tledger.domain.ports.EntryIdProvider;
import com.google.tledger.domain.ports.EntrySigner;
import com.google.tledger.domain.ports.Ledger;
import com.google.tlog.TlogEntry;
import com.google.tlog.TransparencyLogClient;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class TLedgerModule extends AbstractModule {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final TLedgerArgs tLedgerArgs;
  private final AwsInstanceMetadata awsInstanceMetadata;
  private final AwsResourceNames awsResourceNames;

  public TLedgerModule(TLedgerArgs tLedgerArgs, AwsInstanceMetadata awsInstanceMetadata) {
    this.tLedgerArgs = tLedgerArgs;
    this.awsInstanceMetadata = awsInstanceMetadata;

    AwsResourceNamesProvider provider =
        new AwsResourceNamesProvider(tLedgerArgs, awsInstanceMetadata);
    this.awsResourceNames = provider.getRecord();
    logger.atInfo().log("Resolved AWS resource names: %s", awsResourceNames);
  }

  @Override
  protected void configure() {
    bind(TLedgerArgs.class).toInstance(tLedgerArgs);
    bind(TLedger.class).to(TLedgerService.class);
    bind(EntryIdProvider.class).to(Sha256EntryIdProvider.class);
    bind(Ledger.class).to(S3Ledger.class);
    bind(String.class)
        .annotatedWith(LedgerBucketName.class)
        .toInstance(awsResourceNames.ledgerBucketName());
    bind(EntrySigner.class).to(RsaEntrySigner.class);

    install(new AwsKmsClientModule(awsInstanceMetadata.region()));
    install(new AwsAttestationModule());
  }

  @Provides
  @Singleton
  public TransparencyLogClient provideTransparencyLogClient() {
    // TODO: Provided custom Tlog implementation that posts to the ledger.
    return new TransparencyLogClient() {
      @Override
      public TlogEntry recordCertificate(X509Certificate c, PrivateKey k) {
        return new TlogEntry("{\"status\":\"dummy\"}");
      }

      @Override
      public Optional<TlogEntry> getTlogEntryByCertificate(X509Certificate c) {
        return Optional.of(new TlogEntry("{\"status\":\"dummy\"}"));
      }
    };
  }

  @Provides
  @Singleton
  public MeasurementBoundCertificateProvider provideMbs(
      TLedgerArgs args,
      S3Client s3Client,
      KmsClientInterface kmsClient,
      TransparencyLogClient transparencyLogClient,
      AttestationCollector attestationCollector) {
    if (args.isLocalMode()) {
      return new DummyMeasurementBoundCertificateProvider();
    }

    String resourceNamesJson = new Gson().toJson(awsResourceNames);
    byte[] userData = resourceNamesJson.getBytes(StandardCharsets.UTF_8);
    Optional<GeneralNames> noSan = Optional.empty();

    MeasurementBoundCertificateProvider provider =
        new KmsMeasurementBoundCertificateProvider(
            kmsClient,
            s3Client,
            new KeyBackupBucketPropertiesFactory(
                    awsResourceNames.certBackupBucketName(), awsResourceNames.keyBackupBucketName())
                .create(),
            awsResourceNames.kmsKeyArn(),
            userData,
            transparencyLogClient,
            attestationCollector,
            MbsCertificateFactory.createSelfSignedCertificatesFactory(
                new MbsCertificateFactory.CertSignatureSpec("RSA", 4096, "SHA256withRSA"),
                new X500Name("C=US, O=Google LLC, CN=TLedger"),
                Duration.ofDays(120),
                noSan,
                KeyUsage.digitalSignature));

    provider.loadOrGenerateCertificate();
    return provider;
  }

  @Provides
  @Singleton
  public S3Client provideS3Client(TLedgerArgs tLedgerArgs) {
    Region region = Region.of(awsInstanceMetadata.region());
    logger.atInfo().log("Creating S3Client in AWS region: %s", region.toString());

    return S3Client.builder().region(region).build();
  }

  @Provides
  @Singleton
  public JsonFormat.Printer provideJsonFromatPrinter() {
    return JsonFormat.printer().alwaysPrintFieldsWithNoPresence();
  }

  @Provides
  @Singleton
  public JsonFormat.Parser provideJsonFromatParser() {
    return JsonFormat.parser().ignoringUnknownFields();
  }
}
