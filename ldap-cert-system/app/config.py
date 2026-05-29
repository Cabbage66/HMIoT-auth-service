import os


class Config:
    LDAP_URI = os.getenv("LDAP_URI", "ldap://ldap:389")
    LDAP_BIND_DN = os.getenv("LDAP_BIND_DN", "cn=admin,dc=example,dc=org")
    LDAP_BIND_PASSWORD = os.getenv("LDAP_BIND_PASSWORD", "admin")
    LDAP_BASE_DN = os.getenv("LDAP_BASE_DN", "dc=example,dc=org")
    LDAP_CERT_OU_DN = os.getenv("LDAP_CERT_OU_DN", "ou=certs,dc=example,dc=org")

    CERT_STORAGE_PATH = os.getenv("CERT_STORAGE_PATH", "/app/certs")
    OPENSSL_CNF = os.getenv("OPENSSL_CNF", "/app/openssl/openssl.cnf")
    CA_KEY_PATH = os.getenv("CA_KEY_PATH", "/app/certs/ca/ca.key.pem")
    CA_CERT_PATH = os.getenv("CA_CERT_PATH", "/app/certs/ca/ca.cert.pem")
