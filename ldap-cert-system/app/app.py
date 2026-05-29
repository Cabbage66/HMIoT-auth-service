import base64
import os
import tempfile
import secrets
from datetime import datetime, timedelta, timezone

from flask import Flask, jsonify, request
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import dsa, ec, ed25519, ed448, padding, rsa

from config import Config
from ldap_repo import LDAPCertRepository
from openssl_service import OpenSSLService

app = Flask(__name__)
app.config.from_object(Config)

openssl_service = OpenSSLService(
    cert_storage_path=app.config["CERT_STORAGE_PATH"],
    ca_key=app.config["CA_KEY_PATH"],
    ca_cert=app.config["CA_CERT_PATH"],
    openssl_cnf=app.config["OPENSSL_CNF"],
)
ldap_repo = LDAPCertRepository(
    uri=app.config["LDAP_URI"],
    bind_dn=app.config["LDAP_BIND_DN"],
    password=app.config["LDAP_BIND_PASSWORD"],
    cert_ou_dn=app.config["LDAP_CERT_OU_DN"],
)

LOGIN_CHALLENGES = {}
CHALLENGE_TTL_SECONDS = 120


@app.get("/health")
def health():
    return jsonify({"status": "ok"})


@app.post("/register")
def register():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    public_key_pem = data.get("public_key_pem")
    if not username or not public_key_pem:
        return jsonify({"error": "username and public_key_pem are required"}), 400

    cert_meta = openssl_service.issue_certificate_from_public_key(username, public_key_pem)
    ldap_repo.upsert_certificate(
        username=cert_meta["username"],
        serial=cert_meta["serial"],
        fingerprint_sha256=cert_meta["fingerprint_sha256"],
        cert_der=cert_meta["cert_der"],
        cert_sha256=cert_meta["cert_sha256"],
    )

    return jsonify(
        {
            "username": cert_meta["username"],
            "serial": cert_meta["serial"],
            "fingerprint_sha256": cert_meta["fingerprint_sha256"],
            "certificate_pem": cert_meta["cert_pem"].decode("utf-8"),
        }
    )


@app.post("/login")
def login():
    data = request.get_json(silent=True) or {}
    phase = data.get("phase")
    username = data.get("username")
    if not phase or not username:
        return jsonify({"error": "phase and username are required"}), 400

    if phase == "request":
        ldap_record = ldap_repo.find_by_username(username)
        if not ldap_record:
            return jsonify({"error": "user not found"}), 404

        challenge = secrets.token_urlsafe(32)
        challenge_id = secrets.token_hex(16)
        expires_at = datetime.now(timezone.utc) + timedelta(seconds=CHALLENGE_TTL_SECONDS)
        LOGIN_CHALLENGES[challenge_id] = {
            "username": username,
            "challenge": challenge,
            "expires_at": expires_at,
        }
        return jsonify(
            {
                "challenge_id": challenge_id,
                "challenge": challenge,
                "expires_in_seconds": CHALLENGE_TTL_SECONDS,
            }
        )

    if phase == "verify":
        challenge_id = data.get("challenge_id")
        signature_b64 = data.get("signature_b64")
        if not challenge_id or not signature_b64:
            return jsonify({"error": "challenge_id and signature_b64 are required"}), 400

        challenge_info = LOGIN_CHALLENGES.get(challenge_id)
        if not challenge_info:
            return jsonify({"verified": False, "reason": "invalid challenge_id"}), 400
        if challenge_info["username"] != username:
            return jsonify({"verified": False, "reason": "challenge user mismatch"}), 400
        if datetime.now(timezone.utc) > challenge_info["expires_at"]:
            LOGIN_CHALLENGES.pop(challenge_id, None)
            return jsonify({"verified": False, "reason": "challenge expired"}), 401

        ldap_record = ldap_repo.find_by_username(username)
        if not ldap_record:
            return jsonify({"verified": False, "reason": "user not found"}), 404

        cert_der = ldap_record["cert_der"]
        cert_obj = x509.load_der_x509_certificate(cert_der)
        cert_pem_bytes = cert_obj.public_bytes(serialization.Encoding.PEM)
        with tempfile.NamedTemporaryFile(delete=False, suffix=".pem") as tf:
            tf.write(cert_pem_bytes)
            tmp_cert_path = tf.name

        try:
            ca_ok = openssl_service.verify_cert_with_ca(tmp_cert_path)
            if not ca_ok:
                return jsonify({"verified": False, "reason": "invalid certificate chain"}), 401

            public_key = cert_obj.public_key()
            signature = base64.b64decode(signature_b64.encode("utf-8"))
            message = challenge_info["challenge"].encode("utf-8")

            if isinstance(public_key, ec.EllipticCurvePublicKey):
                public_key.verify(signature, message, ec.ECDSA(hashes.SHA256()))
            elif isinstance(public_key, rsa.RSAPublicKey):
                public_key.verify(signature, message, padding.PKCS1v15(), hashes.SHA256())
            elif isinstance(public_key, dsa.DSAPublicKey):
                public_key.verify(signature, message, hashes.SHA256())
            elif isinstance(public_key, (ed25519.Ed25519PublicKey, ed448.Ed448PublicKey)):
                public_key.verify(signature, message)
            else:
                return jsonify({"verified": False, "reason": "unsupported public key type"}), 400

            LOGIN_CHALLENGES.pop(challenge_id, None)
            serial_hex = format(cert_obj.serial_number, "X")
            return jsonify(
                {
                    "verified": True,
                    "username": username,
                    "serial": serial_hex,
                    "fingerprint_sha256": ldap_record["fingerprint"],
                }
            )
        except Exception as exc:
            return jsonify({"verified": False, "reason": f"signature verify failed: {exc}"}), 401
        finally:
            if os.path.exists(tmp_cert_path):
                os.remove(tmp_cert_path)

    return jsonify({"error": "unsupported phase, use request or verify"}), 400


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
