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
import static org.mockito.Mockito.when;

import com.beust.jcommander.JCommander;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.google.mbs.MeasurementBoundCertificate;
import com.google.mbs.MeasurementBoundCertificateProvider;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import com.google.tledger.annotations.LedgerBucketName;
import com.google.tledger.v1.CreateEntryRequest;
import com.google.tledger.v1.Entry;
import com.google.tledger.v1.GetEntryRequest;
import com.google.tledger.v1.TransparentLedgerServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Duration;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@RunWith(JUnit4.class)
public class ServerS3IntegrationTest {

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock private MeasurementBoundCertificateProvider mbsProvider;
  @Mock private MeasurementBoundCertificate mbs;
  private KeyPair testRsaKeyPair;

  private TransparentLedgerServiceGrpc.TransparentLedgerServiceBlockingStub tledgerClient;
  public static LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.14.0"))
          .withServices(LocalStackContainer.Service.S3)
          .withStartupTimeout(Duration.ofSeconds(120));

  private static S3Client s3Client;
  private static final String BUCKET_NAME = "test-bucket";
  private static final String AWS_ACCOUNT_ID = "123456987";

  @BeforeClass
  public static void setUpClass() {
    localstack.start();
    initializeS3Client();
  }

  @Before
  public void setUp() throws Exception {
    createTestBucket();
    initializeMocks();
    startServerAndSetupClient(BUCKET_NAME);
  }

  private static void initializeS3Client() {
    s3Client =
        S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .forcePathStyle(true)
            .region(Region.of(localstack.getRegion()))
            .build();
  }

  private void createTestBucket() {
    ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(BUCKET_NAME).build();
    try {
      ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
      for (S3Object s3Object : listRes.contents()) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(s3Object.key()).build());
      }
    } catch (Exception e) {
      // Bucket does not exist on the first run
    }

    try {
      s3Client.createBucket(b -> b.bucket(BUCKET_NAME));
    } catch (Exception e) {
      // Bucket exists on following runs
    }
  }

  private void initializeMocks() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    testRsaKeyPair = keyGen.generateKeyPair();

    when(mbsProvider.loadOrGenerateCertificate()).thenReturn(mbs);
    when(mbs.getPrivateKey()).thenReturn(testRsaKeyPair.getPrivate());
  }

  private void startServerAndSetupClient(String bucketName) throws Exception {
    TLedgerArgs args = new TLedgerArgs();
    JCommander.newBuilder()
        .addObject(args)
        .build()
        .parse("--local", "--ledger-bucket-prefix=" + bucketName);

    // This is a workaround to a localstack problem with supporting SHA256
    // The code sets the checksum algorithm to SHA1 on any putObject request
    S3Client s3ClientToUse =
        (S3Client)
            java.lang.reflect.Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[] {S3Client.class},
                (proxy, method, methodArgs) -> {
                  if (method.getName().equals("putObject")) {
                    PutObjectRequest putReq = (PutObjectRequest) methodArgs[0];

                    methodArgs[0] =
                        putReq.toBuilder().checksumAlgorithm(ChecksumAlgorithm.SHA1).build();
                  }
                  try {
                    return method.invoke(s3Client, methodArgs);
                  } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                  }
                });

    Injector injector =
        Guice.createInjector(
            Modules.override(
                    new TLedgerModule(
                        args, new AwsInstanceMetadata(localstack.getRegion(), AWS_ACCOUNT_ID)))
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(String.class)
                            .annotatedWith(LedgerBucketName.class)
                            .toInstance(bucketName);
                        bind(S3Client.class).toInstance(s3ClientToUse);
                        bind(MeasurementBoundCertificateProvider.class).toInstance(mbsProvider);
                      }
                    }));

    TransparentLedgerGrpcHandler handler = injector.getInstance(TransparentLedgerGrpcHandler.class);

    String serverName = InProcessServerBuilder.generateName();

    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(handler)
            .build()
            .start());

    tledgerClient =
        TransparentLedgerServiceGrpc.newBlockingStub(
            grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build()));
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

  private Entry getEntryFromS3(String key) throws Exception {
    GetObjectRequest objectRequest =
        GetObjectRequest.builder().bucket(BUCKET_NAME).key(key).build();
    String entryJson =
        s3Client.getObject(objectRequest, ResponseTransformer.toBytes()).asUtf8String();
    Entry.Builder entryBuilder = Entry.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(entryJson, entryBuilder);
    return entryBuilder.build();
  }

  private void createEntryInS3(Entry entry) throws Exception {
    String entryJson = JsonFormat.printer().print(entry);
    s3Client.putObject(
        PutObjectRequest.builder().bucket(BUCKET_NAME).key(entry.getName()).build(),
        RequestBody.fromString(entryJson));
  }

  private List<String> listKeysInS3() {
    ListObjectsV2Response listRes =
        s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build());
    return listRes.contents().stream().map(S3Object::key).toList();
  }

  @Test
  public void createEntry_emptyByteString_throwsInvalidArgument() {
    CreateEntryRequest createRequest =
        CreateEntryRequest.newBuilder()
            .setEntry(Entry.newBuilder().setRawEntry(ByteString.EMPTY))
            .build();

    StatusRuntimeException exception =
        assertThrows(StatusRuntimeException.class, () -> tledgerClient.createEntry(createRequest));
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
    assertThat(listKeysInS3()).isEmpty();
  }

  @Test
  public void getEntry_emptyNameString_throwsInvalidArgument() {
    GetEntryRequest getRequest = GetEntryRequest.newBuilder().setName("").build();

    StatusRuntimeException exception =
        assertThrows(StatusRuntimeException.class, () -> tledgerClient.getEntry(getRequest));
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
  }

  @Test
  public void createEntry_shouldReturnCorrectEntry() throws Exception {
    ByteString rawEntryToCreate = ByteString.copyFromUtf8("Test Content for create");
    CreateEntryRequest createRequest =
        CreateEntryRequest.newBuilder()
            .setEntry(Entry.newBuilder().setRawEntry(rawEntryToCreate))
            .build();

    Entry createdEntry = tledgerClient.createEntry(createRequest);

    assertThat(createdEntry).isNotNull();
    assertThat(createdEntry.getRawEntry()).isEqualTo(rawEntryToCreate);
    assertThat(createdEntry.getName()).isNotEmpty();
    assertThat(
            verifySignature(
                rawEntryToCreate, createdEntry.getSignature(), testRsaKeyPair.getPublic()))
        .isTrue();

    List<String> keys = listKeysInS3();
    assertThat(keys).hasSize(1);
    String objectKey = keys.get(0);
    assertThat(createdEntry.getName()).isEqualTo(objectKey);

    Entry persistedEntry = getEntryFromS3(objectKey);
    assertThat(createdEntry).isEqualTo(persistedEntry);
  }

  @Test
  public void getEntry_shouldReturnExistingEntry() throws Exception {
    ByteString rawEntry = ByteString.copyFromUtf8("Existing Content");
    Entry existingEntry =
        Entry.newBuilder()
            .setName("hash/existing123")
            .setRawEntry(rawEntry)
            .setSignature(ByteString.copyFromUtf8("fakeSignature"))
            .build();
    createEntryInS3(existingEntry);

    GetEntryRequest request = GetEntryRequest.newBuilder().setName(existingEntry.getName()).build();
    Entry fetchedEntry = tledgerClient.getEntry(request);

    assertThat(fetchedEntry).isEqualTo(existingEntry);
  }

  @Test
  public void getEntry_shouldReturnCreatedEntry() {
    ByteString rawEntryToCreate = ByteString.copyFromUtf8("Test Content for create");
    CreateEntryRequest createRequest =
        CreateEntryRequest.newBuilder()
            .setEntry(Entry.newBuilder().setRawEntry(rawEntryToCreate))
            .build();
    Entry createdEntry = tledgerClient.createEntry(createRequest);

    GetEntryRequest request = GetEntryRequest.newBuilder().setName(createdEntry.getName()).build();
    Entry fetchedEntry = tledgerClient.getEntry(request);

    assertThat(fetchedEntry).isEqualTo(createdEntry);
  }

  @Test
  public void getEntry_notFound_shouldThrowNotFoundException() {
    String nonExistentName = "hash/12345678";
    GetEntryRequest request = GetEntryRequest.newBuilder().setName(nonExistentName).build();

    StatusRuntimeException exception =
        assertThrows(StatusRuntimeException.class, () -> tledgerClient.getEntry(request));
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
  }

  @Test
  public void getEntry_LedgerError_shouldThrowUnavailableException() throws Exception {
    startServerAndSetupClient("non-existent-bucket");
    GetEntryRequest request = GetEntryRequest.newBuilder().setName("name").build();

    StatusRuntimeException exception =
        assertThrows(StatusRuntimeException.class, () -> tledgerClient.getEntry(request));
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
  }

  @Test
  public void writeEntry_LedgerError_shouldThrowUnavailableException() throws Exception {
    startServerAndSetupClient("non-existent-bucket");
    CreateEntryRequest createRequest =
        CreateEntryRequest.newBuilder()
            .setEntry(
                Entry.newBuilder()
                    .setRawEntry(ByteString.copyFrom("Hi mom!", StandardCharsets.UTF_8))
                    .build())
            .build();

    StatusRuntimeException exception =
        assertThrows(StatusRuntimeException.class, () -> tledgerClient.createEntry(createRequest));
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
  }
}
