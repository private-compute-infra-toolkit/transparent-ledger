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

package com.google.tledger.adapters.ledger;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import com.google.tledger.adapters.EntryMapper;
import com.google.tledger.domain.model.Entry;
import com.google.tledger.domain.ports.LedgerException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RunWith(MockitoJUnitRunner.class)
public class S3LedgerTest {

  private static final String BUCKET_NAME = "test-ledger-bucket";
  private static final String NAME = "object/123";
  private final Entry TEST_ENTRY =
      new Entry(
          NAME, ByteString.copyFrom("raw entry", StandardCharsets.UTF_8), ByteString.EMPTY, "");
  private final JsonFormat.Printer printer = JsonFormat.printer();
  private final JsonFormat.Parser parser = JsonFormat.parser();
  private String TEST_ENTRY_JSON;
  @Mock private S3Client s3Client;

  private S3Ledger s3Ledger;

  @Before
  public void setUp() throws Exception {
    TEST_ENTRY_JSON = printer.print(EntryMapper.toProto(TEST_ENTRY));
    s3Ledger = new S3Ledger(s3Client, BUCKET_NAME, parser, printer);
  }

  @Test
  public void write_callWithCorrectDetails() throws Exception {
    s3Ledger.write(TEST_ENTRY, NAME);

    ArgumentCaptor<PutObjectRequest> putReqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

    verify(s3Client).putObject(putReqCaptor.capture(), bodyCaptor.capture());

    PutObjectRequest capturedRequest = putReqCaptor.getValue();
    assertThat(capturedRequest.bucket()).isEqualTo(BUCKET_NAME);
    assertThat(capturedRequest.key()).isEqualTo(NAME);
    assertThat(capturedRequest.checksumAlgorithm()).isEqualTo(ChecksumAlgorithm.SHA256);

    String actualContent =
        new String(
            bodyCaptor.getValue().contentStreamProvider().newStream().readAllBytes(),
            StandardCharsets.UTF_8);

    assertThat(actualContent).isEqualTo(TEST_ENTRY_JSON);
  }

  @Test
  public void read_keyExists() {
    ResponseBytes<GetObjectResponse> mockResponseBytes =
        ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), TEST_ENTRY_JSON.getBytes(StandardCharsets.UTF_8));

    when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(mockResponseBytes);

    Optional<Entry> result = s3Ledger.read(NAME);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(TEST_ENTRY);

    ArgumentCaptor<GetObjectRequest> getReqCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
    verify(s3Client).getObject(getReqCaptor.capture(), any(ResponseTransformer.class));

    assertThat(getReqCaptor.getValue().bucket()).isEqualTo(BUCKET_NAME);
    assertThat(getReqCaptor.getValue().key()).isEqualTo(NAME);
  }

  @Test
  public void read_returnsEmpty_whenNoSuchKeyException() {
    when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenThrow(NoSuchKeyException.builder().build());

    Optional<Entry> result = s3Ledger.read(NAME);

    assertThat(result).isEmpty();
  }

  @Test(expected = LedgerException.class)
  public void read_otherS3Exceptions() {
    when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenThrow(S3Exception.builder().message("test error").build());

    s3Ledger.read(NAME);
  }

  @Test(expected = LedgerException.class)
  public void read_invalidJson() {
    ResponseBytes<GetObjectResponse> mockResponseBytes =
        ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), "invalid JSON".getBytes(StandardCharsets.UTF_8));

    when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(mockResponseBytes);

    s3Ledger.read(NAME);
  }

  @Test(expected = LedgerException.class)
  public void write_s3Exceptions() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(S3Exception.builder().message("test error").build());

    s3Ledger.write(TEST_ENTRY, NAME);
  }
}
