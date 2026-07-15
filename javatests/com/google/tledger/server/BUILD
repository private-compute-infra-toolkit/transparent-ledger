# Copyright 2025 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "ServerS3IntegrationTest",
    size = "medium",
    srcs = ["ServerS3IntegrationTest.java"],
    deps = [
        "//java/com/google/tledger/annotations",
        "//java/com/google/tledger/domain/ports",
        "//java/com/google/tledger/proto:entry_java_proto",
        "//java/com/google/tledger/proto:tledger_java_grpc",
        "//java/com/google/tledger/proto:tledger_java_proto",
        "//java/com/google/tledger/server",
        "//java/com/google/tledger/server:tledger_cmd_args",
        "@com_google_protobuf//java/util",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@io_grpc_grpc_java//testing",
        "@maven//:com_beust_jcommander",
        "@maven//:com_google_inject_guice",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:com_linecorp_armeria_armeria",
        "@maven//:com_linecorp_armeria_armeria_grpc",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_testcontainers_localstack",
        "@maven//:org_testcontainers_testcontainers",
        "@maven//:software_amazon_awssdk_auth",
        "@maven//:software_amazon_awssdk_regions",
        "@maven//:software_amazon_awssdk_s3",
        "@maven//:software_amazon_awssdk_sdk_core",
        "@pcit_mbs//java/com/google/mbs:certmanagement",
        "@pcit_mbs//java/com/google/mbs:dummy_cert_provider",
    ],
)

java_test(
    name = "TLedgerModuleTest",
    size = "small",
    srcs = ["TLedgerModuleTest.java"],
    deps = [
        "//java/com/google/tledger/adapters/entryid",
        "//java/com/google/tledger/adapters/ledger",
        "//java/com/google/tledger/adapters/signature",
        "//java/com/google/tledger/annotations",
        "//java/com/google/tledger/domain",
        "//java/com/google/tledger/domain/model",
        "//java/com/google/tledger/domain/ports",
        "//java/com/google/tledger/server",
        "//java/com/google/tledger/server:tledger_cmd_args",
        "@com_google_protobuf//java/util",
        "@maven//:com_beust_jcommander",
        "@maven//:com_google_inject_guice",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:software_amazon_awssdk_regions",
        "@maven//:software_amazon_awssdk_s3",
        "@maven//:software_amazon_awssdk_sdk_core",
        "@pcit_mbs//java/com/google/mbs:certmanagement",
        "@pcit_mbs//java/com/google/mbs:dummy_cert_provider",
    ],
)

java_test(
    name = "AwsResourceNamesProviderTest",
    size = "small",
    srcs = ["AwsResourceNamesProviderTest.java"],
    test_class = "com.google.tledger.server.AwsResourceNamesProviderTest",
    deps = [
        "//java/com/google/tledger/server",
        "//java/com/google/tledger/server:tledger_cmd_args",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)

java_test(
    name = "CertificateValidityReporterTest",
    size = "small",
    srcs = ["CertificateValidityReporterTest.java"],
    test_class = "com.google.tledger.server.CertificateValidityReporterTest",
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        "//java/com/google/tledger/domain/metric",
        "//java/com/google/tledger/server",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
