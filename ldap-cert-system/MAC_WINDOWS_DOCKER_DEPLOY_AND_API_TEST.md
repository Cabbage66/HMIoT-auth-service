# macOS / Windows Docker 部署与接口测试指南

本文基于项目现有的 `DEPLOYMENT_AND_API.md` 与 `docker-compose.yml` 整理，面向 macOS 与 Windows 环境，说明如何使用 Docker 部署服务并完成接口联调测试。

## 1. 项目与端口说明

- HTTP 服务地址：`http://<server>:5000`

- OpenLDAP 地址：`ldap://<server>:389`

- 主要容器：

  - `cert-ldap`（OpenLDAP）

  - `cert-app`（Flask 应用）

`docker-compose.yml` 默认端口映射：

- `389:389`（LDAP）

- `5000:5000`（API）

***

## 2. macOS 部署步骤

### 2.1 前置条件

- 已安装 Docker Desktop（包含 `docker compose`）

- 已安装 `openssl`

- 建议有 `python3`（用于组装 JSON 请求）

### 2.2 启动步骤

```bash
cd /你的项目路径/ldap-cert-system

# 如无执行权限，先赋权
chmod +x ./scripts/init_ca.sh

# 初始化本地 CA（生成 certs/ca/ca.key.pem 与 certs/ca/ca.cert.pem）
./scripts/init_ca.sh

# 启动 ldap + app
docker compose up --build -d
```

### 2.3 健康检查

```bash
curl -s http://127.0.0.1:5001/health
```

预期返回：

```json
{"status":"ok"}
```

***

## 3. Windows 部署步骤

推荐优先使用 **WSL2 + Docker Desktop**（与 Linux/macOS 命令一致，最稳定）。

### 3.1 方式 A（推荐）：WSL2 终端执行

```bash
cd /mnt/c/你的项目路径/ldap-cert-system
chmod +x ./scripts/init_ca.sh
./scripts/init_ca.sh
docker compose up --build -d
curl -s http://127.0.0.1:5001/health
```

### 3.2 方式 B：PowerShell 执行 Docker 命令

说明：`init_ca.sh` 是 Shell 脚本，建议在 Git Bash 或 WSL 中执行初始化 CA。完成后可在 PowerShell 中管理容器。

```powershell
cd C:你的项目路径\ldap-cert-system
docker compose up --build -d
curl.exe -s http://127.0.0.1:5001/health
```

***

## 4. 接口测试（完整联调流程）

接口包括：

- `GET /health`

- `POST /register`

- `POST /login`（`request -> verify` 两阶段）

以下示例基于 Bash（macOS / Linux / WSL 均可）。

### 4.1 生成测试密钥对

```bash
mkdir -p /tmp/cert-client && cd /tmp/cert-client
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out alice_private.pem
openssl pkey -pubout -in alice_private.pem -out alice_public.pem
```

### 4.2 调用注册接口 `POST /register`

```bash
curl -s -X POST http://127.0.0.1:5001/register \
  -H 'Content-Type: application/json' \
  -d "$(python3 - <<'PY'
import json
pub=open('/tmp/cert-client/alice_public.pem','r',encoding='utf-8').read()
print(json.dumps({"username":"alice","public_key_pem":pub}))
PY
)"
```

成功响应通常包含：

- `username`

- `serial`

- `fingerprint_sha256`

- `certificate_pem`

### 4.3 调用登录接口 `POST /login`（两阶段）

#### 阶段 1：请求 challenge

```bash
REQ="$(curl -s -X POST http://127.0.0.1:5001/login \
  -H 'Content-Type: application/json' \
  -d '{"phase":"request","username":"alice"}')"

CHALLENGE_ID="$(python3 -c "import json;print(json.loads('''$REQ''')['challenge_id'])")"
CHALLENGE="$(python3 -c "import json;print(json.loads('''$REQ''')['challenge'])")"
```

#### 阶段 2：私钥签名并提交 verify

```bash
printf '%s' "$CHALLENGE" > /tmp/cert-client/challenge.txt

openssl dgst -sha256 -sign /tmp/cert-client/alice_private.pem \
  -out /tmp/cert-client/sig.bin /tmp/cert-client/challenge.txt

# 使用 openssl base64 -A 兼容 macOS/Linux，避免 base64 参数差异
SIG_B64="$(openssl base64 -A -in /tmp/cert-client/sig.bin)"

curl -s -X POST http://127.0.0.1:5001/login \
  -H 'Content-Type: application/json' \
  -d "$(python3 - <<PY
import json
print(json.dumps({
  "phase":"verify",
  "username":"alice",
  "challenge_id":"$CHALLENGE_ID",
  "signature_b64":"$SIG_B64"
}))
PY
)"
```

成功预期（示例）：

```json
{
  "verified": true,
  "username": "alice",
  "serial": "...",
  "fingerprint_sha256": "..."
}
```

***

## 5. 故障排查

### 5.1 `500` 错误

查看应用日志：

```bash
docker logs cert-app --tail 200
```

常见原因：

- LDAP 写入失败（OU/schema 问题）

- 证书签发或证书链校验失败

### 5.2 `invalid challenge_id`

常见原因：

- 两阶段间隔超时（默认挑战 TTL 为 120 秒）

- `challenge_id` 与用户不匹配

- 多实例/多 worker 情况下挑战未共享（当前实现为进程内存存储）

### 5.3 端口占用

若本机已占用 `389` 或 `5000`，请调整 `docker-compose.yml` 的端口映射后重启容器：

```bash
docker compose down
docker compose up --build -d
```

***

## 6. 常用命令速查

```bash
# 启动
docker compose up --build -d

# 查看状态
docker compose ps

# 查看日志
docker logs cert-app --tail 200
docker logs cert-ldap --tail 200

# 停止并删除容器
docker compose down
```
