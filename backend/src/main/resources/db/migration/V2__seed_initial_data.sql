SET NAMES utf8mb4;

USE hmiot_auth;
-- 初始化默认管理员账号：admin / Admin@123456。
-- 该 SQL 只保存 BCrypt 密码摘要，不保存明文密码；若账号已存在，则刷新显示名、角色、状态并恢复逻辑删除标记。
INSERT INTO sys_admin (username, password_hash, display_name, role_code, status)
VALUES ('admin', '$2a$10$Fq2LlACeD14Zdsv0JLReZeGo11B8PPw94dbJaHDi7UbREmzQFXTAy', '系统管理员', 'SYSTEM_ADMIN', 'ENABLED')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    role_code = VALUES(role_code),
    status = VALUES(status),
    deleted = 0;

-- 初始化演示用户账号，供阶段 1 用户列表、用户详情、用户编辑接口和阶段 2 属性同步接口验收使用。
-- 三个用户覆盖 ENABLED 与 PENDING 状态、ONLINE 与 OFFLINE 注册来源；统一演示密码为 User@123456，库中仅保存 BCrypt 摘要。
INSERT INTO user_account (username, password_hash, real_name, phone, email, id_card_no, status, register_source)
VALUES
    ('demo-user-001', '$2a$10$pHfl9EfegTTOPVlGAkSN4OwAlrlMb1qcJVoYZpzE4HBXjo7KKN8x.', '张三', '13800000001', 'zhangsan@example.com', '110101199001010011', 'ENABLED', 'OFFLINE'),
    ('demo-user-002', '$2a$10$pHfl9EfegTTOPVlGAkSN4OwAlrlMb1qcJVoYZpzE4HBXjo7KKN8x.', '李四', '13800000002', 'lisi@example.com', '110101199002020022', 'ENABLED', 'ONLINE'),
    ('demo-user-003', '$2a$10$pHfl9EfegTTOPVlGAkSN4OwAlrlMb1qcJVoYZpzE4HBXjo7KKN8x.', '王五', '13800000003', 'wangwu@example.com', '110101199003030033', 'PENDING', 'ONLINE')
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    email = VALUES(email),
    id_card_no = VALUES(id_card_no),
    status = VALUES(status),
    register_source = VALUES(register_source),
    deleted = 0;

-- 为 demo-user-001 预置一条后量子属性同步前的旧属性。
-- 这条数据用于用户详情展示和属性同步接口的“更新前/更新后”对比；重复执行时按 user_id 唯一键更新为同一份演示数据。
INSERT INTO user_profile (user_id, department, position_name, attribute_version, external_source, external_user_id, external_attributes, synced_at)
SELECT id, '人机物认证部', '认证管理员', 'seed-001', 'POST_QUANTUM_ATTRIBUTE', CONCAT('pq-', id), JSON_OBJECT('clearance', 'L3', 'source', 'seed'), NOW()
FROM user_account
WHERE username = 'demo-user-001'
ON DUPLICATE KEY UPDATE
    department = VALUES(department),
    position_name = VALUES(position_name),
    attribute_version = VALUES(attribute_version),
    external_source = VALUES(external_source),
    external_user_id = VALUES(external_user_id),
    external_attributes = VALUES(external_attributes),
    synced_at = VALUES(synced_at);

-- 为 demo-user-002 预置另一条外部属性，形成不同部门、岗位和权限等级的演示样本。
-- 阶段 2 调用属性同步接口后，可通过该初始版本与 mock 外部系统返回版本判断同步链路是否生效。
INSERT INTO user_profile (user_id, department, position_name, attribute_version, external_source, external_user_id, external_attributes, synced_at)
SELECT id, '物联网安全部', '设备安全员', 'seed-002', 'POST_QUANTUM_ATTRIBUTE', CONCAT('pq-', id), JSON_OBJECT('clearance', 'L2', 'source', 'seed'), NOW()
FROM user_account
WHERE username = 'demo-user-002'
ON DUPLICATE KEY UPDATE
    department = VALUES(department),
    position_name = VALUES(position_name),
    attribute_version = VALUES(attribute_version),
    external_source = VALUES(external_source),
    external_user_id = VALUES(external_user_id),
    external_attributes = VALUES(external_attributes),
    synced_at = VALUES(synced_at);

-- 为 demo-user-001 预置一张 mock USB/证书登录验收证书。
-- 该证书的序列号和指纹与 mock 证书验签客户端保持一致，便于在未启动 ldap-cert-system 时验证阶段 3 登录链路。
INSERT INTO user_certificate (user_id, serial_number, fingerprint_sha256, certificate_pem, issuer, subject_dn, status, issued_at)
SELECT id, 'MOCK-F844FB814E50AC5D', 'F844FB814E50AC5D038D16081D8E324ACB21B18A3D1812883AF360DB7A13D5429', '-----BEGIN CERTIFICATE-----\nMOCK-demo-user-001\n-----END CERTIFICATE-----', 'mock-certificate-service', 'CN=demo-user-001', 'VALID', NOW()
FROM user_account
WHERE username = 'demo-user-001'
ON DUPLICATE KEY UPDATE
    certificate_pem = VALUES(certificate_pem),
    issuer = VALUES(issuer),
    subject_dn = VALUES(subject_dn),
    status = VALUES(status),
    issued_at = VALUES(issued_at),
    revoked_at = NULL,
    revoke_reason = NULL;
