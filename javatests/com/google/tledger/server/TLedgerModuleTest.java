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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.beust.jcommander.JCommander;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.mbs.MbsCertificateFactory;
import com.google.mbs.domain.CertificateMonitor;
import com.google.mbs.domain.MeasurementBoundCertificateProvider;
import com.google.mbs.domain.MeasurementBoundCertificateReloader;
import com.google.mbs.qualifier.InstanceId;
import com.google.mbs.qualifier.KmsKeyArn;
import com.google.mbs.qualifier.PrivateBackupBucket;
import com.google.mbs.qualifier.PublicBackupBucket;
import com.google.protobuf.util.JsonFormat;
import com.google.tledger.adapters.SystemMetrics;
import com.google.tledger.adapters.entryid.Sha256EntryIdProvider;
import com.google.tledger.adapters.ledger.S3Ledger;
import com.google.tledger.adapters.signature.RsaEntrySigner;
import com.google.tledger.annotations.LedgerBucketName;
import com.google.tledger.domain.TLedger;
import com.google.tledger.domain.TLedgerService;
import com.google.tledger.domain.ports.EntryIdProvider;
import com.google.tledger.domain.ports.EntrySigner;
import com.google.tledger.domain.ports.Ledger;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@RunWith(JUnit4.class)
public class TLedgerModuleTest {

  private TLedgerArgs mockArgs;
  private Injector injector;

  private static final String TEST_BUCKET_PREFIX = "hi-mom-bucket";
  private static final String TEST_REGION = Region.US_EAST_1.toString();
  private static final String TEST_ACCOUNT_ID = "123456789012";

  @Before
  public void setUp() {
    mockArgs = new TLedgerArgs();
    JCommander.newBuilder()
        .addObject(mockArgs)
        .build()
        .parse("--local", "--ledger-bucket-prefix=" + TEST_BUCKET_PREFIX);

    injector =
        Guice.createInjector(
            new TLedgerModule(
                mockArgs,
                new AwsInstanceMetadata(
                    TEST_REGION, TEST_ACCOUNT_ID, "testenv", "testdomain", "i-testinstance")));
  }

  @Test
  public void injector_canBeCreated() {
    assertThat(injector).isNotNull();
  }

  @Test
  public void configure_bindsTLedgerArgs() {
    TLedgerArgs instance = injector.getInstance(TLedgerArgs.class);
    assertThat(instance).isSameInstanceAs(mockArgs);
  }

  @Test
  public void configure_bindsTLedgerService() {
    TLedger instance = injector.getInstance(TLedger.class);
    assertThat(instance).isInstanceOf(TLedgerService.class);
  }

  @Test
  public void configure_bindsEntryIdProvider() {
    EntryIdProvider instance = injector.getInstance(EntryIdProvider.class);
    assertThat(instance).isInstanceOf(Sha256EntryIdProvider.class);
  }

  @Test
  public void configure_bindsLedger() {
    Ledger instance = injector.getInstance(Ledger.class);
    assertThat(instance).isInstanceOf(S3Ledger.class);
  }

  @Test
  public void configure_bindsEntrySigner() {
    EntrySigner instance = injector.getInstance(EntrySigner.class);
    assertThat(instance).isInstanceOf(RsaEntrySigner.class);
  }

  @Test
  public void configure_bindsLedgerBucketName() {
    String bucketName = injector.getInstance(Key.get(String.class, LedgerBucketName.class));
    assertThat(bucketName)
        .isEqualTo(TEST_BUCKET_PREFIX + "-" + TEST_ACCOUNT_ID + "-" + TEST_REGION);
  }

  @Test
  public void provideS3Client_providesNonNull() {
    S3Client client = injector.getInstance(S3Client.class);
    assertThat(client).isNotNull();
  }

  @Test
  public void provideS3Client_isSingleton() {
    S3Client client1 = injector.getInstance(S3Client.class);
    S3Client client2 = injector.getInstance(S3Client.class);
    assertThat(client1).isSameInstanceAs(client2);
  }

  @Test
  public void provideJsonFormatPrinter_providesNonNull() {
    JsonFormat.Printer printer = injector.getInstance(JsonFormat.Printer.class);
    assertThat(printer).isNotNull();
  }

  @Test
  public void provideJsonFormatPrinter_isSingleton() {
    JsonFormat.Printer printer1 = injector.getInstance(JsonFormat.Printer.class);
    JsonFormat.Printer printer2 = injector.getInstance(JsonFormat.Printer.class);
    assertThat(printer1).isSameInstanceAs(printer2);
  }

  @Test
  public void provideJsonFormatParser_providesNonNull() {
    JsonFormat.Parser parser = injector.getInstance(JsonFormat.Parser.class);
    assertThat(parser).isNotNull();
  }

  @Test
  public void provideJsonFormatParser_isSingleton() {
    JsonFormat.Parser parser1 = injector.getInstance(JsonFormat.Parser.class);
    JsonFormat.Parser parser2 = injector.getInstance(JsonFormat.Parser.class);
    assertThat(parser1).isSameInstanceAs(parser2);
  }

  @Test
  public void providePrometheusMeterRegistry_providesNonNull() {
    PrometheusMeterRegistry registry = injector.getInstance(PrometheusMeterRegistry.class);
    assertThat(registry).isNotNull();
  }

  @Test
  public void configure_bindsMeasurementBoundCertificateProvider() {
    MeasurementBoundCertificateProvider provider =
        injector.getInstance(MeasurementBoundCertificateProvider.class);
    MeasurementBoundCertificateReloader reloader =
        injector.getInstance(MeasurementBoundCertificateReloader.class);
    assertThat(provider).isNotNull();
    assertThat(reloader).isNotNull();

    // In MBS 0.9.0+, getCertificate() is decoupled from reloadCertificate() and throws
    // IllegalStateException until the certificate is loaded or generated.
    assertThrows(IllegalStateException.class, provider::getActiveTrustPackage);

    reloader.reloadCertificate();
    assertThat(provider.getActiveTrustPackage().bundles().get(0)).isNotNull();
    assertThat(provider.getActiveTrustPackage().bundles().get(0).getCertificate()).isNotNull();
    assertThat(provider.getActiveTrustPackage().bundles().get(0).getPrivateKey()).isNotNull();
  }

  @Test
  public void configure_bindsCertificateMonitor() {
    CertificateMonitor monitor = injector.getInstance(CertificateMonitor.class);
    assertThat(monitor).isNotNull();
  }

  @Test
  public void configure_kmsMode_bindsRequiredDependencies() {
    TLedgerArgs kmsArgs = new TLedgerArgs();
    JCommander.newBuilder()
        .addObject(kmsArgs)
        .build()
        .parse("--ledger-bucket-prefix=" + TEST_BUCKET_PREFIX);

    Injector kmsInjector =
        Guice.createInjector(
            new TLedgerModule(
                kmsArgs,
                new AwsInstanceMetadata(
                    TEST_REGION, TEST_ACCOUNT_ID, "testenv", "testdomain", "i-testinstance")));

    assertThat(kmsInjector).isNotNull();
    assertThat(kmsInjector.getInstance(Key.get(String.class, InstanceId.class)))
        .isEqualTo("i-testinstance");
    assertThat(kmsInjector.getInstance(Key.get(String.class, KmsKeyArn.class))).isNotNull();
    assertThat(kmsInjector.getInstance(Key.get(String.class, PublicBackupBucket.class)))
        .isNotNull();
    assertThat(kmsInjector.getInstance(Key.get(String.class, PrivateBackupBucket.class)))
        .isNotNull();
    assertThat(kmsInjector.getInstance(com.google.mbs.domain.Metrics.class))
        .isInstanceOf(SystemMetrics.class);
    assertThat(kmsInjector.getInstance(MeasurementBoundCertificateProvider.class)).isNotNull();
    assertThat(kmsInjector.getInstance(MeasurementBoundCertificateReloader.class)).isNotNull();
    assertThat(kmsInjector.getInstance(CertificateMonitor.class)).isNotNull();
  }

  @Test
  public void provideMbsCertificateFactory_generatesCertificateWithCorrectSpiffeIdInSan()
      throws Exception {
    TLedgerModule module =
        new TLedgerModule(
            mockArgs,
            new AwsInstanceMetadata(
                TEST_REGION, TEST_ACCOUNT_ID, "testenv", "testdomain", "i-testinstance"));

    MbsCertificateFactory factory = module.provideMbsCertificateFactory();
    X509Certificate rootCert = factory.generate().certificate();

    assertThat(rootCert.getSubjectX500Principal().getName())
        .isEqualTo("CN=TLedger,O=Google LLC,C=US");

    String expectedSpiffeId =
        "spiffe://tledger.testenv.testdomain/operator/pcit.goog/123456789012/publisher/google.com/pcit-release-bot/workload/transparent-ledger";

    byte[] sanExtensionValue = rootCert.getExtensionValue(Extension.subjectAlternativeName.getId());
    assertThat(sanExtensionValue).isNotNull();

    GeneralNames names =
        GeneralNames.getInstance(ASN1OctetString.getInstance(sanExtensionValue).getOctets());
    assertThat(names.getNames()).hasLength(1);

    GeneralName sanEntry = names.getNames()[0];
    assertThat(sanEntry.getTagNo()).isEqualTo(GeneralName.uniformResourceIdentifier);
    assertThat(sanEntry.getName().toString()).isEqualTo(expectedSpiffeId);
  }

  @Test
  public void provideMbsCertificateFactory_prodEnv_stripsAwsSubdomain() throws Exception {
    TLedgerModule module =
        new TLedgerModule(
            mockArgs,
            new AwsInstanceMetadata(
                TEST_REGION, TEST_ACCOUNT_ID, "prod", "aws.pcit.goog", "i-testinstance"));

    MbsCertificateFactory factory = module.provideMbsCertificateFactory();
    X509Certificate rootCert = factory.generate().certificate();

    String expectedSpiffeId =
        "spiffe://tledger.pcit.goog/operator/pcit.goog/123456789012/publisher/google.com/pcit-release-bot/workload/transparent-ledger";

    byte[] sanExtensionValue = rootCert.getExtensionValue(Extension.subjectAlternativeName.getId());
    assertThat(sanExtensionValue).isNotNull();

    GeneralNames names =
        GeneralNames.getInstance(ASN1OctetString.getInstance(sanExtensionValue).getOctets());
    GeneralName sanEntry = names.getNames()[0];
    assertThat(sanEntry.getTagNo()).isEqualTo(GeneralName.uniformResourceIdentifier);
    assertThat(sanEntry.getName().toString()).isEqualTo(expectedSpiffeId);
  }
}
