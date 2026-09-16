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

load("@rules_java//java:defs.bzl", "java_binary", "java_library")

java_library(
    name = "tledger_cmd_args",
    srcs = [
        "TLedgerArgs.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:com_beust_jcommander",
    ],
)

java_library(
    name = "server",
    srcs = [
        "AwsInstanceMetadata.java",
        "AwsResourceNames.java",
        "AwsResourceNamesProvider.java",
        "CertificateValidityReporter.java",
        "ImdsClient.java",
        "TLedgerModule.java",
        "TLedgerServer.java",
        "TransparentLedgerGrpcHandler.java",
    ],
    plugins = [":auto_value_plugin"],
    visibility = ["//visibility:public"],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        ":tledger_cmd_args",
        "//java/com/google/tledger/adapters",
        "//java/com/google/tledger/adapters/entryid",
        "//java/com/google/tledger/adapters/ledger",
        "//java/com/google/tledger/adapters/signature",
        "//java/com/google/tledger/annotations",
        "//java/com/google/tledger/domain",
        "//java/com/google/tledger/domain/metric",
        "//java/com/google/tledger/domain/model",
        "//java/com/google/tledger/domain/ports",
        "//java/com/google/tledger/proto:entry_java_proto",
        "//java/com/google/tledger/proto:tledger_java_grpc",
        "//java/com/google/tledger/proto:tledger_java_proto",
        "@com_google_protobuf//java/util",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//services:health",
        "@io_grpc_grpc_java//services:reflection",
        "@io_grpc_grpc_java//stub",
        "@io_grpc_grpc_proto//:health_java_proto",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_inject_guice",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_linecorp_armeria_armeria",
        "@maven//:com_linecorp_armeria_armeria_grpc",
        "@maven//:com_linecorp_armeria_armeria_prometheus1",
        "@maven//:io_micrometer_micrometer_core",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:jakarta_inject_jakarta_inject_api",
        "@maven//:org_bouncycastle_bcpkix_jdk18on",
        "@maven//:org_bouncycastle_bcprov_jdk18on",
        "@maven//:software_amazon_awssdk_auth",
        "@maven//:software_amazon_awssdk_imds",
        "@maven//:software_amazon_awssdk_regions",
        "@maven//:software_amazon_awssdk_s3",
        "@maven//:software_amazon_awssdk_sdk_core",
        "@pcit_mbs//java/com/google/kmsclient",
        "@pcit_mbs//java/com/google/kmsclient/aws",
        "@pcit_mbs//java/com/google/mbs:certmanagement",
        "@pcit_mbs//java/com/google/mbs:dummy_cert_provider",
        "@pcit_mbs//java/com/google/mbs:module",
        "@pcit_mbs//java/com/google/mbs/attestationcollection",
        "@pcit_mbs//java/com/google/mbs/attestationcollection/aws",
        "@pcit_mbs//java/com/google/platform/aws/nsm",
    ],
)

genrule(
    name = "copy_logback_xml",
    srcs = ["@rules_aws//build_defs/aws/enclave:logback.xml"],
    outs = ["logback.xml"],
    cmd = "cp $< $@",
)

java_binary(
    name = "server_main",
    srcs = [
        "ServerMain.java",
    ],
    main_class = "com.google.tledger.server.ServerMain",
    resource_strip_prefix = "java/com/google/tledger/server",
    resources = [":logback.xml"],
    visibility = ["//visibility:public"],
    runtime_deps = [
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
        "@maven//:com_google_flogger_flogger_slf4j_backend",
        "@maven//:net_logstash_logback_logstash_logback_encoder",
    ],
    deps = [
        ":server",
        ":tledger_cmd_args",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//services:health",
        "@io_grpc_grpc_java//services:reflection",
        "@io_grpc_grpc_proto//:health_java_proto",
        "@maven//:com_beust_jcommander",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_inject_guice",
        "@maven//:com_linecorp_armeria_armeria",
        "@maven//:com_linecorp_armeria_armeria_grpc",
        "@maven//:com_linecorp_armeria_armeria_prometheus1",
        "@maven//:io_grpc_grpc_netty_shaded",
        "@maven//:io_micrometer_micrometer_core",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
    ],
)
