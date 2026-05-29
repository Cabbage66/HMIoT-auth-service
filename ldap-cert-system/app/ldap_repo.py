import ldap
import ldap.modlist as modlist
import time


class LDAPCertRepository:
    def __init__(self, uri, bind_dn, password, cert_ou_dn):
        self.uri = uri
        self.bind_dn = bind_dn
        self.password = password
        self.cert_ou_dn = cert_ou_dn

    def _conn(self):
        last_exc = None
        for _ in range(5):
            try:
                conn = ldap.initialize(self.uri)
                conn.set_option(ldap.OPT_NETWORK_TIMEOUT, 3)
                conn.simple_bind_s(self.bind_dn, self.password)
                return conn
            except ldap.SERVER_DOWN as exc:
                last_exc = exc
                time.sleep(1)
        raise last_exc

    def upsert_certificate(self, username, serial, fingerprint_sha256, cert_der, cert_sha256):
        # Ensure the parent OU exists; otherwise `uid=...,ou=certs,...` add will fail with NO_SUCH_OBJECT.
        self._ensure_ou_exists()

        dn = f"uid={username},{self.cert_ou_dn}"
        attrs = {
            "objectClass": [b"top", b"person", b"organizationalPerson", b"inetOrgPerson", b"pkiUser"],
            "cn": [username.encode("utf-8")],
            "sn": [username.encode("utf-8")],
            "uid": [username.encode("utf-8")],
            "employeeNumber": [fingerprint_sha256.encode("utf-8")],
            "description": [f"cert_sha256={cert_sha256}".encode("utf-8")],
            # `userCertificate;binary` expects DER-encoded certificate bytes.
            "userCertificate;binary": [cert_der],
        }

        conn = self._conn()
        try:
            ldif = modlist.addModlist(attrs)
            conn.add_s(dn, ldif)
        except ldap.ALREADY_EXISTS:
            mods = [
                (ldap.MOD_REPLACE, "cn", [username.encode("utf-8")]),
                (ldap.MOD_REPLACE, "sn", [username.encode("utf-8")]),
                (ldap.MOD_REPLACE, "employeeNumber", [fingerprint_sha256.encode("utf-8")]),
                (ldap.MOD_REPLACE, "description", [f"cert_sha256={cert_sha256}".encode("utf-8")]),
                (ldap.MOD_REPLACE, "userCertificate;binary", [cert_der]),
            ]
            conn.modify_s(dn, mods)
        finally:
            conn.unbind_s()

    def find_by_username(self, username):
        conn = self._conn()
        try:
            # 绕过索引过滤，直接根据绝对路径精确敲门！
            try:
                res = conn.search_s(
                    f"uid={username},{self.cert_ou_dn}",
                    ldap.SCOPE_BASE,
                    "(objectClass=*)",
                    ["uid", "cn", "employeeNumber", "userCertificate;binary"],
                )
            except ldap.NO_SUCH_OBJECT:
                return None

            if not res:
                return None
            _, attrs = res[0]
            return {
                "uid": attrs.get("uid", [b""])[0].decode("utf-8"),
                "cn": attrs.get("cn", [b""])[0].decode("utf-8"),
                "fingerprint": attrs.get("employeeNumber", [b""])[0].decode("utf-8"),
                "cert_der": attrs.get("userCertificate;binary", [b""])[0],
            }
        finally:
            conn.unbind_s()

    def _ensure_ou_exists(self):
        conn = self._conn()
        try:
            res = conn.search_s(
                self.cert_ou_dn,
                ldap.SCOPE_BASE,
                "(objectClass=*)",
                ["dn"],
            )
            if res:
                return

            # Extract `ou` value from DN like `ou=certs,dc=example,dc=org`.
            ou_value = None
            parts = [p.strip() for p in self.cert_ou_dn.split(",")]
            for p in parts:
                if p.lower().startswith("ou="):
                    ou_value = p.split("=", 1)[1]
                    break
            if not ou_value:
                raise ValueError(f"Cannot parse OU from cert_ou_dn={self.cert_ou_dn}")

            attrs = {
                "objectClass": [b"top", b"organizationalUnit"],
                "ou": [ou_value.encode("utf-8")],
            }
            ldif = modlist.addModlist(attrs)
            conn.add_s(self.cert_ou_dn, ldif)
        except ldap.ALREADY_EXISTS:
            return
        finally:
            conn.unbind_s()

    def find_by_fingerprint(self, fingerprint_sha256):
        conn = self._conn()
        try:
            res = conn.search_s(
                self.cert_ou_dn,
                ldap.SCOPE_SUBTREE,
                f"(employeeNumber={fingerprint_sha256})",
                ["uid", "cn", "employeeNumber", "userCertificate;binary"],
            )
            if not res:
                return None
            _, attrs = res[0]
            return {
                "uid": attrs.get("uid", [b""])[0].decode("utf-8"),
                "cn": attrs.get("cn", [b""])[0].decode("utf-8"),
                "fingerprint": attrs.get("employeeNumber", [b""])[0].decode("utf-8"),
                "cert_der": attrs.get("userCertificate;binary", [b""])[0],
            }
        finally:
            conn.unbind_s()
