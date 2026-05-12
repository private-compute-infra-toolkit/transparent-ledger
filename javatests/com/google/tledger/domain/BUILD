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
    name = "TLedgerServiceTest",
    size = "small",
    srcs = ["TLedgerServiceTest.java"],
    deps = [
        "//java/com/google/tledger/domain",
        "//java/com/google/tledger/domain/model",
        "//java/com/google/tledger/domain/ports",
        "//java/com/google/tledger/proto:entry_java_proto",
        "//java/com/google/tledger/server",
        "//java/com/google/tledger/server:tledger_cmd_args",
        "//java/com/google/tledger/testing",
        "@maven//:com_beust_jcommander",
        "@maven//:com_google_inject_guice",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@pcit_mbs//java/com/google/mbs:certmanagement",
    ],
)
