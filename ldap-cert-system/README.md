# Flask + OpenSSL + LDAP 证书系统（容器化）

这个项目实现了你描述的最小系统：
- 接口 1：用户注册时提交用户名+公钥，签发 X.509 证书（自签 CA）
- 接口 2：用户登录挑战认证（下发 challenge + 提交签名验证）
- 证书入 LDAP、出 LDAP
- OpenSSL 证书格式转换脚本（PEM/DER/Base64）
- Flask 后端 + Docker 容器化
- LDAP 索引（支持百万级证书场景下的快速等值查询）

## 目录结构

- `app/` Flask 服务与 LDAP/OpenSSL 逻辑
- `scripts/` CA 初始化、格式转换、LDAP 入库/出库脚本
- `ldap/bootstrap/` LDAP 初始化 LDIF（OU + 索引）
- `openssl/openssl.cnf` OpenSSL 配置
- `certs/` 证书目录（CA、签发证书、请求、导出）

## 快速启动

1) 初始化 CA

```bash
cd /home/ldap/ldap-cert-system
./scripts/init_ca.sh
```

2) 启动服务

```bash
docker compose up --build -d
```

3) 健康检查

```bash
curl http://127.0.0.1:5000/health
```

## API

### 1. 用户注册并签发证书

`POST /register`

请求：

```json
{
  "username": "alice",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----..."
}
```

返回（示例）：

```json
{
  "username": "alice",
  "serial": "2A3F...",
  "fingerprint_sha256": "A1B2...",
  "certificate_pem": "-----BEGIN CERTIFICATE-----..."
}
```

说明：
- 服务端根据 `username + public_key_pem` 生成并签发 X.509 用户证书
- 将证书写入 LDAP（`userCertificate;binary`）
- 同时写入可索引属性：`uid=username`、`serialNumber`、`employeeNumber(指纹)`

### 2. 用户登录挑战认证

`POST /login`（两阶段）

第一阶段，请求挑战码：

```json
{
  "phase": "request",
  "username": "alice"
}
```

返回挑战码（示例）：

```json
{
  "challenge_id": "ab12...",
  "challenge": "qwerty...",
  "expires_in_seconds": 120
}
```

第二阶段，提交签名：

```json
{
  "phase": "verify",
  "username": "alice",
  "challenge_id": "ab12...",
  "signature_b64": "BASE64_SIGNATURE"
}
```

返回（示例）：

```json
{
  "verified": true,
  "username": "alice",
  "serial": "2A3F...",
  "fingerprint_sha256": "A1B2..."
}
```

验证流程：
- 服务端返回随机 challenge
- 客户端用私钥对 challenge 做签名并回传
- 服务端从 LDAP 取用户证书，先用 CA 验证证书链，再用证书公钥验签

## 证书格式转换脚本

```bash
# PEM -> DER
./scripts/convert_cert.sh pem2der certs/issued/alice.cert.pem certs/exports/alice.cert.der

# DER -> PEM
./scripts/convert_cert.sh der2pem certs/exports/alice.cert.der certs/exports/alice.cert.pem

# PEM 文件 -> Base64 文本
./scripts/convert_cert.sh pem2b64 certs/issued/alice.cert.pem certs/exports/alice.cert.b64

# Base64 文本 -> 文件
./scripts/convert_cert.sh b64tofile certs/exports/alice.cert.b64 certs/exports/alice.cert.fromb64.pem
```

## LDAP 入库/出库脚本

### 入库（LDIF）

```bash
./scripts/ldap_import_cert.sh \
  ldap://127.0.0.1:389 \
  cn=admin,dc=example,dc=org \
  admin \
  your_data.ldif
```

### 出库（查询）

```bash
./scripts/ldap_export_cert.sh \
  ldap://127.0.0.1:389 \
  cn=admin,dc=example,dc=org \
  admin \
  ou=certs,dc=example,dc=org \
  '(employeeNumber=A1B2C3...)'
```

## 百万证书检索建议

已内置索引（`ldap/bootstrap/02-add-indexes.ldif`）：
- `uid eq`
- `serialNumber eq`
- `employeeNumber eq`

这三项对应业务中的高频精确匹配键（用户标识、序列号、指纹），可显著降低全表扫描。

进一步建议（生产）：
- 采用 `mdb` 后端（当前即 mdb）
- 增大 `olcDbMaxSize` 以容纳百万数据量
- 将数据盘放 SSD
- 对查询走连接池（应用侧）
- 限制返回属性列表，避免返回大字段
- 做压测并根据命中模式调整索引

## 停止服务

```bash
docker compose down
```

