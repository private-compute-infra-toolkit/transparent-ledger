# Transparent Ledger (TLedger)

TLedger is a secure, tamper-evident Java service designed for high-integrity data logging. It utilizes a
**Write-Once-Read-Many (WORM)** storage model to ensure records remain immutable and verifiable. The API allows users to
post arbitrary blobs of data directly to the ledger, making it versatile for any data format.

## Security & Root of Trust

- **TEE & MBS:** TLedger runs in a TEE (e.g., AWS Nitro Enclave). Its root of trust is established by the
  **Measurement Bound Service (MBS)**, which binds signing keys to the TEE's measurements. This
  ensures only authorized binaries can sign ledger entries.
- **Tamper-Evidence:** Data is stored in **AWS S3 with Compliance Mode Object Lock**, preventing modification or
  deletion. Every entry is cryptographically signed by the TEE-secured key.
- **Auditability:** All bucket events are logged for external auditing by CloudTrail. The ledger is designed for public
  accessibility
  and periodic audits.
