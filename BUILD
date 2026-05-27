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

load("@bazel_skylib//rules:common_settings.bzl", "string_flag")
load("//:build_defs/tledger_aws_enclave.bzl", "tledger_aws_eif_and_ami")

package(default_visibility = ["//visibility:public"])

exports_files(["java/com/google/tledger/logback.xml"])

string_flag(
    name = "ami_name_flag",
    build_setting_default = "tledger-enclave",
)

string_flag(
    name = "aws_region_flag",
    build_setting_default = "us-east-1",
)

string_flag(
    name = "subnet_id_flag",
    build_setting_default = "",
)

tledger_aws_eif_and_ami(
    name = "tledger_aws_dev",
    enable_worker_debug_mode = True,
)

tledger_aws_eif_and_ami(
    name = "tledger_aws_release",
)

tledger_aws_eif_and_ami(
    name = "tledger_aws_integration_test",
    enable_worker_debug_mode = True,
    jar_args = [
        "--ledger-bucket-prefix=tledger-integration-tests-ledger",
        "--cert-backup-bucket-prefix=tledger-integration-tests-key-backup",
        "--key-backup-bucket-prefix=tledger-integration-tests-key-backup",
        "--mbs-kms-key-suffix=alias/tledegr-integration-tests-key-encryption-key",
    ],
)
