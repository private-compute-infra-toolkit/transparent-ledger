# Changelog

All notable changes to this project will be documented in this file. See [commit-and-tag-version](https://github.com/absolute-version/commit-and-tag-version) for commit guidelines.

## 0.6.0 (2026-09-16)


### Dependencies

* **deps:** Update api to release-0.6.0
* **deps:** Update container-tools to release-0.8.0
* **deps:** Update DevKit to release-3.11.0

## 0.5.0 (2026-07-24)


### Features

* Update root certificate validity to expire on 2027-02-02

## 0.4.0 (2026-07-15)


### Dependencies

* **deps:** Update DevKit to release-3.10.0
* **deps:** Update DevKit to release-3.9.0


### Features

* Add metrics-exporter configuration
* Change MBS metrics from counter to gauge
* Expose main certificate validity metric
* Fix cloudwatch-agent metrics namespace
* Integrate MBS metrics
* Introduce metrics module and endpoint
* Introduce Metrics module to support custom metrics
* Update cert validity to be 180 days
* Update container-tools and fix prometheus setup
* Update container-tools to fix enclave watcher resets
* Update MBS. Set SKI in root cert. Set cache-control on backup
* Update root certificate validity to expire on 2027-02-01

## 0.3.0 (2026-06-01)


### Dependencies

* **deps:** Update DevKit to release-3.8.0


### Features

* Update operator/ part of Tledger's root cert spiffe_id
* update root cert SPIFFE ID format


### Bug Fixes

* Bump submodule mbs to store root_cert as PEM format
* tledger name mismatch between ami build and terraform

## 0.2.0 (2026-05-27)


### Dependencies

* **deps:** Update DevKit to release-3.6.0
* **deps:** Update DevKit to release-3.7.0


### Features

* enable enclave logging and clean up build defs boilerplate

## 0.1.0 (2026-05-12)


### Features

* Initial release
