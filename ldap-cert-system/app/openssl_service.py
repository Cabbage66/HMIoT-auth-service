import hashlib
import os
import subprocess
from datetime import datetime, timedelta, timezone

from cryptography import x509
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.serialization import load_pem_private_key, load_pem_public_key
from cryptography.x509.oid import NameOID


class OpenSSLService:
    def __init__(self, cert_storage_path: str, ca_key: str, ca_cert: str, openssl_cnf: str):
        self.cert_storage_path = cert_storage_path
        self.ca_key = ca_key
        self.ca_cert = ca_cert
        self.openssl_cnf = openssl_cnf

    def _run(self, cmd):
        proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
        if proc.returncode != 0:
            raise RuntimeError(f"Command failed: {' '.join(cmd)}\\n{proc.stderr}")
        return proc.stdout

    def issue_certificate_from_public_key(self, username: str, public_key_pem: str):
        ts = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        issued_dir = os.path.join(self.cert_storage_path, "issued")
        os.makedirs(issued_dir, exist_ok=True)
        cert_path = os.path.join(issued_dir, f"{username}.{ts}.cert.pem")
        ca_cert_obj = x509.load_pem_x509_certificate(self._read_file_bytes(self.ca_cert))
        ca_key_obj = load_pem_private_key(self._read_file_bytes(self.ca_key), password=None)
        user_public_key = load_pem_public_key(public_key_pem.encode("utf-8"))

        cert = (
            x509.CertificateBuilder()
            .subject_name(
                x509.Name(
                    [
                        x509.NameAttribute(NameOID.COUNTRY_NAME, "CN"),
                        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "Example"),
                        x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "Users"),
                        x509.NameAttribute(NameOID.COMMON_NAME, username),
                    ]
                )
            )
            .issuer_name(ca_cert_obj.subject)
            .public_key(user_public_key)
            .serial_number(x509.random_serial_number())
            .not_valid_before(datetime.now(timezone.utc) - timedelta(minutes=5))
            .not_valid_after(datetime.now(timezone.utc) + timedelta(days=365))
            .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
            .sign(private_key=ca_key_obj, algorithm=ca_cert_obj.signature_hash_algorithm)
        )
        cert_pem = cert.public_bytes(serialization.Encoding.PEM)
        # OpenLDAP's `userCertificate;binary` attribute expects DER-encoded certificate bytes.
        cert_der = cert.public_bytes(serialization.Encoding.DER)
        with open(cert_path, "wb") as f:
            f.write(cert_pem)

        serial = self._run(["openssl", "x509", "-in", cert_path, "-noout", "-serial"]).strip().split("=")[1]
        fingerprint = self._run([
            "openssl", "x509", "-in", cert_path, "-noout", "-fingerprint", "-sha256"
        ]).strip().split("=")[1].replace(":", "")

        return {
            "username": username,
            "serial": serial,
            "fingerprint_sha256": fingerprint,
            "cert_path": cert_path,
            "cert_pem": cert_pem,
            "cert_der": cert_der,
            # Hash the canonical certificate bytes (DER) for stable comparisons.
            "cert_sha256": hashlib.sha256(cert_der).hexdigest(),
        }

    def _read_file_bytes(self, path: str) -> bytes:
        with open(path, "rb") as f:
            return f.read()

    def cert_to_der(self, pem_path: str, der_path: str):
        self._run(["openssl", "x509", "-in", pem_path, "-outform", "DER", "-out", der_path])

    def der_to_pem(self, der_path: str, pem_path: str):
        self._run(["openssl", "x509", "-inform", "DER", "-in", der_path, "-out", pem_path])

    def verify_cert_with_ca(self, cert_path: str):
        out = self._run(["openssl", "verify", "-CAfile", self.ca_cert, cert_path])
        return "OK" in out
