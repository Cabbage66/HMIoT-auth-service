-- HMIoT Auth Service baseline schema
-- Target database: MySQL 8.x
-- Charset: utf8mb4

-- 创建项目默认数据库 hmiot_auth，统一使用 utf8mb4 字符集，便于中文、符号和 JSON 文本存储。
CREATE DATABASE IF NOT EXISTS hmiot_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 切换到 hmiot_auth 数据库，后续所有建表、索引、外键和初始化数据都写入该库。
USE hmiot_auth;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 管理员账号表：保存后台管理端登录账号、角色、状态和最近登录信息，是管理员认证、JWT 签发和审计主体识别的基础。
CREATE TABLE IF NOT EXISTS sys_admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码BCrypt摘要',
    display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
    role_code VARCHAR(64) NOT NULL DEFAULT 'SYSTEM_ADMIN' COMMENT '角色编码',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '业务状态',
    last_login_at DATETIME NULL COMMENT '最近登录时间',
    last_login_ip VARCHAR(64) NULL COMMENT '最近登录IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_admin_username (username),
    KEY idx_sys_admin_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员账号表';

-- 用户账号表：保存自然人用户的基础身份、联系方式、注册来源和账号状态，支撑用户管理、认证和属性同步。
CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(255) NULL COMMENT '密码BCrypt摘要',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(32) NULL COMMENT '手机号',
    email VARCHAR(128) NULL COMMENT '邮箱',
    id_card_no VARCHAR(64) NULL COMMENT '证件号码',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '业务状态',
    register_source VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT '注册来源',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_user_account_username (username),
    KEY idx_user_account_real_name (real_name),
    KEY idx_user_account_status (status),
    KEY idx_user_account_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号表';

-- 用户扩展属性表：保存后量子属性系统同步得到的部门、岗位、属性版本和原始属性 JSON，用于阶段 2 属性更新前后对比。
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    department VARCHAR(128) NULL COMMENT '部门名称',
    position_name VARCHAR(128) NULL COMMENT '岗位名称',
    attribute_version VARCHAR(64) NULL COMMENT '属性版本',
    external_source VARCHAR(64) NULL COMMENT '外部来源',
    external_user_id VARCHAR(128) NULL COMMENT '外部用户ID',
    external_attributes JSON NULL COMMENT '外部属性JSON',
    synced_at DATETIME NULL COMMENT '同步时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_profile_user (user_id),
    KEY idx_user_profile_external_user (external_source, external_user_id),
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户扩展属性表';

-- 用户注册申请表：保存线上注册或线下录入后的待审核资料、原始请求、审核人、审核时间和拒绝原因。
CREATE TABLE IF NOT EXISTS registration_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NULL COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(32) NULL COMMENT '手机号',
    email VARCHAR(128) NULL COMMENT '邮箱',
    id_card_no VARCHAR(64) NULL COMMENT '证件号码',
    request_payload JSON NULL COMMENT '请求载荷JSON',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '业务状态',
    reviewer_id BIGINT NULL COMMENT '审核人ID',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    reject_reason VARCHAR(512) NULL COMMENT '拒绝原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_registration_status (status),
    KEY idx_registration_username (username),
    KEY idx_registration_reviewer (reviewer_id),
    CONSTRAINT fk_registration_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_registration_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户注册申请表';

-- 统一实体身份表：抽象用户、设备、终端、物联对象等认证实体，提供跨模块统一编码和外部系统映射。
CREATE TABLE IF NOT EXISTS entity_identity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    entity_type VARCHAR(32) NOT NULL COMMENT '实体类型',
    entity_code VARCHAR(128) NOT NULL COMMENT '实体编码',
    entity_name VARCHAR(128) NOT NULL COMMENT '实体名称',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '业务状态',
    source_system VARCHAR(64) NULL COMMENT '来源系统',
    external_id VARCHAR(128) NULL COMMENT '外部ID',
    metadata JSON NULL COMMENT '扩展元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_entity_identity_code (entity_type, entity_code),
    KEY idx_entity_identity_status (entity_type, status),
    KEY idx_entity_identity_external (source_system, external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一实体身份表';

-- 设备实体表：保存终端、物联网设备、摄像头和感知节点等设备的编码、类型、指纹、证书主题和状态。
CREATE TABLE IF NOT EXISTS device_entity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    entity_id BIGINT NULL COMMENT '实体身份ID',
    device_code VARCHAR(128) NOT NULL COMMENT '设备编码',
    device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
    device_type VARCHAR(32) NOT NULL COMMENT '设备类型',
    device_fingerprint VARCHAR(255) NULL COMMENT '设备指纹',
    cert_subject VARCHAR(255) NULL COMMENT '证书主体',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '业务状态',
    source_system VARCHAR(64) NULL COMMENT '来源系统',
    description VARCHAR(512) NULL COMMENT '描述信息',
    metadata JSON NULL COMMENT '扩展元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_device_entity_code (device_code),
    KEY idx_device_entity_type_status (device_type, status),
    KEY idx_device_entity_fingerprint (device_fingerprint),
    CONSTRAINT fk_device_entity_identity FOREIGN KEY (entity_id) REFERENCES entity_identity(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备实体表';

-- 终端信息表：保存 Web、Android、IoT 等客户端终端的编码、系统版本、客户端版本、最近访问 IP 和设备关联。
CREATE TABLE IF NOT EXISTS terminal_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    terminal_code VARCHAR(128) NOT NULL COMMENT '终端编码',
    device_entity_id BIGINT NULL COMMENT '设备实体ID',
    user_agent VARCHAR(512) NULL COMMENT '用户代理',
    os_name VARCHAR(64) NULL COMMENT '操作系统名称',
    os_version VARCHAR(64) NULL COMMENT '操作系统版本',
    client_type VARCHAR(32) NOT NULL COMMENT '客户端类型',
    client_version VARCHAR(64) NULL COMMENT '客户端版本',
    ip_address VARCHAR(64) NULL COMMENT 'IP地址',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '业务状态',
    last_seen_at DATETIME NULL COMMENT '最近在线时间',
    metadata JSON NULL COMMENT '扩展元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_terminal_code (terminal_code),
    KEY idx_terminal_device (device_entity_id),
    KEY idx_terminal_status (status),
    CONSTRAINT fk_terminal_device FOREIGN KEY (device_entity_id) REFERENCES device_entity(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='终端信息表';

-- 物联网设备状态表：保存设备最新连接状态、信任状态、上报时间和最后一次上报报文。
CREATE TABLE IF NOT EXISTS iot_device_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_entity_id BIGINT NOT NULL COMMENT '设备实体ID',
    connect_status VARCHAR(32) NOT NULL DEFAULT 'OFFLINE' COMMENT '连接状态',
    trust_status VARCHAR(32) NULL COMMENT '信任状态',
    last_report_at DATETIME NULL COMMENT '最近上报时间',
    last_payload JSON NULL COMMENT '最近上报报文',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_iot_device_status_device (device_entity_id),
    KEY idx_iot_device_status (connect_status),
    CONSTRAINT fk_iot_status_device FOREIGN KEY (device_entity_id) REFERENCES device_entity(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物联网设备状态表';

-- 绑定关系表：保存用户、终端、设备、应用等主体之间的绑定、解绑、有效期和操作原因。
CREATE TABLE IF NOT EXISTS binding_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    subject_type VARCHAR(32) NOT NULL COMMENT '主体类型',
    subject_id BIGINT NOT NULL COMMENT '主体ID',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    relation_type VARCHAR(32) NOT NULL COMMENT '关系类型',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '业务状态',
    effective_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
    expired_at DATETIME NULL COMMENT '失效时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    reason VARCHAR(512) NULL COMMENT '原因说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_binding_active (subject_type, subject_id, target_type, target_id, relation_type, status),
    KEY idx_binding_subject (subject_type, subject_id, status),
    KEY idx_binding_target (target_type, target_id, status),
    KEY idx_binding_created_by (created_by),
    CONSTRAINT fk_binding_created_by FOREIGN KEY (created_by) REFERENCES sys_admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定关系表';

-- 用户证书表：保存用户数字证书序列号、指纹、PEM 内容、有效期和吊销信息，供后续证书认证阶段使用。
CREATE TABLE IF NOT EXISTS user_certificate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    serial_number VARCHAR(128) NOT NULL COMMENT '证书序列号',
    fingerprint_sha256 VARCHAR(128) NOT NULL COMMENT '证书SHA256指纹',
    certificate_pem MEDIUMTEXT NOT NULL COMMENT 'PEM证书内容',
    issuer VARCHAR(255) NULL COMMENT '签发者',
    subject_dn VARCHAR(255) NULL COMMENT '证书主体DN',
    status VARCHAR(32) NOT NULL DEFAULT 'VALID' COMMENT '业务状态',
    issued_at DATETIME NULL COMMENT '签发时间',
    expires_at DATETIME NULL COMMENT '过期时间',
    revoked_at DATETIME NULL COMMENT '吊销时间',
    revoke_reason VARCHAR(512) NULL COMMENT '吊销原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_certificate_serial (serial_number),
    UNIQUE KEY uk_user_certificate_fingerprint (fingerprint_sha256),
    KEY idx_user_certificate_user_status (user_id, status),
    CONSTRAINT fk_user_certificate_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户证书表';

-- 生物特征模板表：保存指纹、人脸、声纹等模板摘要、外部模板引用、模型来源、审核状态和版本信息。
CREATE TABLE IF NOT EXISTS biometric_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    biometric_type VARCHAR(32) NOT NULL COMMENT '生物特征类型',
    template_digest VARCHAR(255) NULL COMMENT '模板摘要',
    template_ref VARCHAR(255) NULL COMMENT '模板引用',
    model_source VARCHAR(64) NULL COMMENT '模型来源',
    model_version VARCHAR(64) NULL COMMENT '模型版本',
    threshold DECIMAL(10,6) NULL COMMENT '阈值',
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    template_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '模板状态',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    reviewed_by BIGINT NULL COMMENT '审核人ID',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    revoked_at DATETIME NULL COMMENT '吊销时间',
    reject_reason VARCHAR(512) NULL COMMENT '拒绝原因',
    metadata JSON NULL COMMENT '扩展元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_biometric_user_type (user_id, biometric_type, template_status),
    KEY idx_biometric_audit_status (audit_status),
    KEY idx_biometric_reviewed_by (reviewed_by),
    CONSTRAINT fk_biometric_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_biometric_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生物特征模板表';

-- 生物认证记录表：保存每次生物认证的模板、分数、阈值、结果、失败原因和外部模型原始响应。
CREATE TABLE IF NOT EXISTS biometric_auth_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NULL COMMENT '用户ID',
    biometric_type VARCHAR(32) NOT NULL COMMENT '生物特征类型',
    template_id BIGINT NULL COMMENT '模板ID',
    request_id VARCHAR(128) NULL COMMENT '请求ID',
    score DECIMAL(12,6) NULL COMMENT '认证分数',
    threshold DECIMAL(12,6) NULL COMMENT '阈值',
    auth_result VARCHAR(32) NOT NULL COMMENT '认证结果',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    model_source VARCHAR(64) NULL COMMENT '模型来源',
    raw_response JSON NULL COMMENT '原始响应JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_biometric_auth_user (user_id, biometric_type, created_at),
    KEY idx_biometric_auth_result (auth_result, created_at),
    KEY idx_biometric_auth_template (template_id),
    CONSTRAINT fk_biometric_auth_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_biometric_auth_template FOREIGN KEY (template_id) REFERENCES biometric_template(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生物认证记录表';

-- 应用资源表：保存被保护应用、API 或菜单资源的编码、访问地址、安全等级、最低信任等级和启停状态。
CREATE TABLE IF NOT EXISTS app_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    app_code VARCHAR(128) NOT NULL COMMENT '应用编码',
    app_name VARCHAR(128) NOT NULL COMMENT '应用名称',
    resource_type VARCHAR(32) NOT NULL DEFAULT 'APP' COMMENT '资源类型',
    access_url VARCHAR(512) NULL COMMENT '访问地址',
    security_level INT NOT NULL DEFAULT 1 COMMENT '安全等级',
    required_trust_level INT NOT NULL DEFAULT 0 COMMENT '最低信任等级',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '业务状态',
    description VARCHAR(512) NULL COMMENT '描述信息',
    metadata JSON NULL COMMENT '扩展元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_app_resource_code (app_code),
    KEY idx_app_resource_status (status),
    KEY idx_app_resource_level (security_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用资源表';

-- 认证方式表：保存口令、证书、USB PIN、生物特征、动态生物特征等认证方式字典。
CREATE TABLE IF NOT EXISTS auth_method (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    method_code VARCHAR(64) NOT NULL COMMENT '认证方式编码',
    method_name VARCHAR(128) NOT NULL COMMENT '认证方式名称',
    method_type VARCHAR(32) NOT NULL COMMENT '认证方式类型',
    compare_mode VARCHAR(32) NULL COMMENT '比对模式',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    description VARCHAR(512) NULL COMMENT '描述信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_auth_method_code (method_code),
    KEY idx_auth_method_enabled (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证方式表';

-- 认证策略表：保存不同登录、单点登录和设备访问场景下的用户范围、设备范围、认证链路和失败处置策略。
CREATE TABLE IF NOT EXISTS auth_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    policy_code VARCHAR(128) NOT NULL COMMENT '策略编码',
    policy_name VARCHAR(128) NOT NULL COMMENT '策略名称',
    scene_type VARCHAR(32) NOT NULL COMMENT '场景类型',
    app_resource_id BIGINT NULL COMMENT '应用资源ID',
    user_scope JSON NULL COMMENT '用户范围JSON',
    device_scope JSON NULL COMMENT '设备范围JSON',
    auth_chain JSON NOT NULL COMMENT '认证链路JSON',
    min_trust_level INT NOT NULL DEFAULT 0 COMMENT '最低信任等级',
    session_ttl_seconds INT NOT NULL DEFAULT 7200 COMMENT '会话有效期秒数',
    action_on_fail VARCHAR(32) NOT NULL DEFAULT 'DENY' COMMENT '失败处置动作',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '业务状态',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_by BIGINT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_auth_policy_code (policy_code),
    KEY idx_auth_policy_scene_status (scene_type, status),
    KEY idx_auth_policy_app (app_resource_id),
    CONSTRAINT fk_auth_policy_app FOREIGN KEY (app_resource_id) REFERENCES app_resource(id),
    CONSTRAINT fk_auth_policy_creator FOREIGN KEY (created_by) REFERENCES sys_admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证策略表';

-- 安全基线表：保存面向终端、设备或跨域对象的安全规则配置、适用范围、风险等级和命中处置动作。
CREATE TABLE IF NOT EXISTS security_baseline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    baseline_code VARCHAR(128) NOT NULL COMMENT '基线编码',
    baseline_name VARCHAR(128) NOT NULL COMMENT '基线名称',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
    target_scope JSON NULL COMMENT '目标范围JSON',
    rule_config JSON NOT NULL COMMENT '规则配置JSON',
    risk_level INT NOT NULL DEFAULT 0 COMMENT '风险等级',
    action_on_hit VARCHAR(32) NOT NULL DEFAULT 'WARN' COMMENT '命中处置动作',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '业务状态',
    created_by BIGINT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_security_baseline_code (baseline_code),
    KEY idx_security_baseline_target (target_type, status),
    CONSTRAINT fk_security_baseline_creator FOREIGN KEY (created_by) REFERENCES sys_admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全基线表';

-- 策略下发记录表：记录认证策略或安全基线向终端、设备、应用、域等目标发布后的执行状态和回执。
CREATE TABLE IF NOT EXISTS policy_publish_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    policy_type VARCHAR(32) NOT NULL COMMENT '策略类型',
    policy_id BIGINT NOT NULL COMMENT '策略ID',
    version_no INT NOT NULL COMMENT '版本号',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
    target_id BIGINT NULL COMMENT '目标ID',
    publish_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '下发状态',
    execute_result VARCHAR(32) NULL COMMENT '执行结果',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    published_by BIGINT NULL COMMENT '下发人ID',
    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
    ack_at DATETIME NULL COMMENT '确认时间',
    KEY idx_policy_publish_policy (policy_type, policy_id, version_no),
    KEY idx_policy_publish_target (target_type, target_id, publish_status),
    CONSTRAINT fk_policy_publish_admin FOREIGN KEY (published_by) REFERENCES sys_admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='策略下发记录表';

-- 认证会话表：保存一次登录、SSO 或设备访问认证过程中的会话号、当前步骤、上下文、状态和有效期。
CREATE TABLE IF NOT EXISTS auth_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id VARCHAR(128) NOT NULL COMMENT '会话ID',
    user_id BIGINT NULL COMMENT '用户ID',
    terminal_id BIGINT NULL COMMENT '终端ID',
    app_resource_id BIGINT NULL COMMENT '应用资源ID',
    scene_type VARCHAR(32) NOT NULL COMMENT '场景类型',
    status VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '业务状态',
    current_step VARCHAR(64) NULL COMMENT '当前步骤',
    auth_context JSON NULL COMMENT '认证上下文JSON',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    UNIQUE KEY uk_auth_session_id (session_id),
    KEY idx_auth_session_user (user_id, status),
    KEY idx_auth_session_terminal (terminal_id),
    KEY idx_auth_session_expires (expires_at),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_auth_session_terminal FOREIGN KEY (terminal_id) REFERENCES terminal_info(id),
    CONSTRAINT fk_auth_session_app FOREIGN KEY (app_resource_id) REFERENCES app_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证会话表';

-- 身份票据表：保存认证成功后签发的票据、签发方、受众方、信任等级、签名摘要、载荷和生命周期。
CREATE TABLE IF NOT EXISTS identity_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    ticket_id VARCHAR(128) NOT NULL COMMENT '票据ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    terminal_id BIGINT NULL COMMENT '终端ID',
    issuer_system VARCHAR(128) NOT NULL COMMENT '签发系统',
    audience_system VARCHAR(128) NULL COMMENT '受众系统',
    trust_level INT NOT NULL DEFAULT 0 COMMENT '信任等级',
    trust_score DECIMAL(12,6) NULL COMMENT '信任评分',
    signature_digest VARCHAR(255) NOT NULL COMMENT '签名摘要',
    ticket_payload JSON NOT NULL COMMENT '票据载荷JSON',
    status VARCHAR(32) NOT NULL DEFAULT 'VALID' COMMENT '业务状态',
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    revoked_at DATETIME NULL COMMENT '吊销时间',
    UNIQUE KEY uk_identity_ticket_id (ticket_id),
    KEY idx_identity_ticket_user (user_id, status),
    KEY idx_identity_ticket_expires (expires_at),
    KEY idx_identity_ticket_audience (audience_system, status),
    CONSTRAINT fk_identity_ticket_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_identity_ticket_terminal FOREIGN KEY (terminal_id) REFERENCES terminal_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='身份票据表';

-- 票据验证记录表：记录目标系统验证身份票据的结果、失败原因、来源 IP、关联资源和验证时间。
CREATE TABLE IF NOT EXISTS ticket_verify_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    ticket_id VARCHAR(128) NOT NULL COMMENT '票据ID',
    verifier_system VARCHAR(128) NOT NULL COMMENT '验证系统',
    app_resource_id BIGINT NULL COMMENT '应用资源ID',
    verify_result VARCHAR(32) NOT NULL COMMENT '验证结果',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    request_ip VARCHAR(64) NULL COMMENT '请求IP',
    verified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '验证时间',
    KEY idx_ticket_verify_ticket (ticket_id, verified_at),
    KEY idx_ticket_verify_result (verify_result, verified_at),
    KEY idx_ticket_verify_app (app_resource_id),
    CONSTRAINT fk_ticket_verify_app FOREIGN KEY (app_resource_id) REFERENCES app_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='票据验证记录表';

-- 单点登录记录表：保存跨系统访问中的来源系统、目标系统、应用资源、票据标识和处理结果。
CREATE TABLE IF NOT EXISTS sso_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NULL COMMENT '用户ID',
    source_system VARCHAR(128) NOT NULL COMMENT '来源系统',
    target_system VARCHAR(128) NOT NULL COMMENT '目标系统',
    app_resource_id BIGINT NULL COMMENT '应用资源ID',
    ticket_id VARCHAR(128) NULL COMMENT '票据ID',
    sso_result VARCHAR(32) NOT NULL COMMENT '单点登录结果',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_sso_user (user_id, created_at),
    KEY idx_sso_system (source_system, target_system, created_at),
    KEY idx_sso_result (sso_result, created_at),
    CONSTRAINT fk_sso_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_sso_app FOREIGN KEY (app_resource_id) REFERENCES app_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='单点登录记录表';

-- 终端事件表：保存终端环境、设备风险、安全基线命中等事件，用于信任评估和审计追踪。
CREATE TABLE IF NOT EXISTS terminal_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    terminal_id BIGINT NULL COMMENT '终端ID',
    user_id BIGINT NULL COMMENT '用户ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    event_level VARCHAR(32) NOT NULL DEFAULT 'INFO' COMMENT '事件等级',
    event_time DATETIME NOT NULL COMMENT '事件时间',
    event_payload JSON NULL COMMENT '事件内容JSON',
    normalized_payload JSON NULL COMMENT '标准化事件内容',
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    KEY idx_terminal_event_terminal (terminal_id, event_time),
    KEY idx_terminal_event_user (user_id, event_time),
    KEY idx_terminal_event_type (event_type, event_time),
    CONSTRAINT fk_terminal_event_terminal FOREIGN KEY (terminal_id) REFERENCES terminal_info(id),
    CONSTRAINT fk_terminal_event_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='终端事件表';

-- 信任评估请求表：记录调用 UEBA 或信任评估服务前的请求摘要、业务键和请求时间。
CREATE TABLE IF NOT EXISTS trust_assessment_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    request_id VARCHAR(128) NOT NULL COMMENT '请求ID',
    user_id BIGINT NULL COMMENT '用户ID',
    terminal_id BIGINT NULL COMMENT '终端ID',
    app_resource_id BIGINT NULL COMMENT '应用资源ID',
    model_source VARCHAR(64) NOT NULL DEFAULT 'UEBA4.2' COMMENT '模型来源',
    model_version VARCHAR(64) NULL COMMENT '模型版本',
    input_digest VARCHAR(255) NULL COMMENT '输入摘要',
    input_payload JSON NULL COMMENT '输入载荷JSON',
    request_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    UNIQUE KEY uk_trust_request_id (request_id),
    KEY idx_trust_request_user (user_id, request_time),
    KEY idx_trust_request_terminal (terminal_id, request_time),
    CONSTRAINT fk_trust_request_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_trust_request_terminal FOREIGN KEY (terminal_id) REFERENCES terminal_info(id),
    CONSTRAINT fk_trust_request_app FOREIGN KEY (app_resource_id) REFERENCES app_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信任评估请求表';

-- 信任评估结果表：保存信任评估返回的分数、等级、风险标签、处置建议和原始响应。
CREATE TABLE IF NOT EXISTS trust_assessment_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    request_id BIGINT NOT NULL COMMENT '请求ID',
    trust_score DECIMAL(12,6) NULL COMMENT '信任评分',
    trust_level INT NOT NULL DEFAULT 0 COMMENT '信任等级',
    risk_level VARCHAR(32) NULL COMMENT '风险等级',
    is_anomaly TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否异常',
    anomaly_indicators JSON NULL COMMENT '异常指标JSON',
    alerts JSON NULL COMMENT '告警信息JSON',
    decision VARCHAR(32) NOT NULL DEFAULT 'ALLOW' COMMENT '评估决策',
    reject_reason VARCHAR(512) NULL COMMENT '拒绝原因',
    raw_response JSON NULL COMMENT '原始响应JSON',
    assessed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
    KEY idx_trust_result_request (request_id),
    KEY idx_trust_result_level (trust_level, assessed_at),
    KEY idx_trust_result_decision (decision, assessed_at),
    CONSTRAINT fk_trust_result_request FOREIGN KEY (request_id) REFERENCES trust_assessment_request(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信任评估结果表';

-- 外部集成调用表：记录后量子属性、证书、声纹、UEBA 等外部系统调用的请求摘要、响应摘要、结果和耗时。
CREATE TABLE IF NOT EXISTS external_integration_call (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    integration_type VARCHAR(64) NOT NULL COMMENT '集成类型',
    target_system VARCHAR(128) NOT NULL COMMENT '目标系统',
    request_id VARCHAR(128) NULL COMMENT '请求ID',
    business_key VARCHAR(128) NULL COMMENT '业务键',
    request_summary JSON NULL COMMENT '请求摘要JSON',
    response_summary JSON NULL COMMENT '响应摘要JSON',
    call_result VARCHAR(32) NOT NULL COMMENT '调用结果',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    finished_at DATETIME NULL COMMENT '结束时间',
    KEY idx_external_call_type (integration_type, started_at),
    KEY idx_external_call_business (business_key),
    KEY idx_external_call_result (call_result, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部集成调用表';

-- 跨域认证记录表：保存跨域认证或跨域票据验证中的域编码、远端系统、操作类型、结果和失败原因。
CREATE TABLE IF NOT EXISTS cross_domain_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NULL COMMENT '用户ID',
    source_system VARCHAR(128) NOT NULL COMMENT '来源系统',
    target_system VARCHAR(128) NOT NULL COMMENT '目标系统',
    target_app VARCHAR(128) NULL COMMENT '目标应用',
    ticket_id VARCHAR(128) NULL COMMENT '票据ID',
    mutual_auth_result VARCHAR(32) NOT NULL COMMENT '双向认证结果',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    raw_response JSON NULL COMMENT '原始响应JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_cross_domain_user (user_id, created_at),
    KEY idx_cross_domain_system (source_system, target_system, created_at),
    KEY idx_cross_domain_result (mutual_auth_result, created_at),
    CONSTRAINT fk_cross_domain_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨域认证记录表';

-- 审计日志表：统一记录管理员、用户、系统等主体的关键操作、对象、结果、来源 IP、链路追踪 ID 和详情。
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    actor_type VARCHAR(32) NOT NULL COMMENT '操作者类型',
    actor_id BIGINT NULL COMMENT '操作者ID',
    actor_name VARCHAR(128) NULL COMMENT '操作者名称',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    object_type VARCHAR(64) NULL COMMENT '对象类型',
    object_id VARCHAR(128) NULL COMMENT '对象ID',
    operation_result VARCHAR(32) NOT NULL COMMENT '操作结果',
    fail_reason VARCHAR(512) NULL COMMENT '失败原因',
    request_ip VARCHAR(64) NULL COMMENT '请求IP',
    trace_id VARCHAR(128) NULL COMMENT '链路追踪ID',
    detail JSON NULL COMMENT '审计详情JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_audit_actor (actor_type, actor_id, created_at),
    KEY idx_audit_operation (operation_type, created_at),
    KEY idx_audit_object (object_type, object_id),
    KEY idx_audit_result (operation_result, created_at),
    KEY idx_audit_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志表';

-- 初始化认证方式字典：预置阶段规划中会用到的口令、证书、USB PIN、生物特征、动态生物特征和设备状态认证方式。
INSERT INTO auth_method (method_code, method_name, method_type, compare_mode, enabled, sort_order, description)
VALUES
    ('PASSWORD', '用户名口令', 'KNOWLEDGE', 'SERVER', 1, 10, '用户名和密码认证'),
    ('CERTIFICATE', '数字证书', 'CERTIFICATE', 'SERVER', 1, 20, '数字证书认证'),
    ('USB_PIN', 'USB认证模块+PIN', 'CERTIFICATE', 'SERVER', 1, 30, 'USB认证模块挑战签名与PIN认证'),
    ('FINGERPRINT', '指纹', 'BIOMETRIC', 'LOCAL', 1, 40, '终端本地指纹比对'),
    ('FACE', '人脸', 'BIOMETRIC', 'REMOTE', 1, 50, '人脸采集与远端比对'),
    ('VOICEPRINT', '声纹', 'BIOMETRIC', 'REMOTE', 1, 60, '声纹模型比对'),
    ('DYNAMIC_BIOMETRIC', '动态生物特征', 'BIOMETRIC', 'EXTERNAL', 1, 70, '步态等动态生物特征认证'),
    ('IOT_DEVICE', '物联网设备状态', 'DEVICE', 'SERVER', 1, 80, '物联网设备合法性和绑定状态校验')
ON DUPLICATE KEY UPDATE
    method_name = VALUES(method_name),
    method_type = VALUES(method_type),
    compare_mode = VALUES(compare_mode),
    enabled = VALUES(enabled),
    sort_order = VALUES(sort_order),
    description = VALUES(description);

-- 初始化演示应用资源：提供后续认证策略、票据和 SSO 阶段可引用的默认业务资源。
INSERT INTO app_resource (app_code, app_name, resource_type, security_level, required_trust_level, status, description)
VALUES
    ('APP_DEMO_A', '模拟应用A', 'APP', 1, 0, 'ENABLED', '用于基础登录后访问演示'),
    ('APP_DEMO_B', '模拟应用B', 'APP', 2, 1, 'ENABLED', '用于跨人机物系统访问演示'),
    ('APP_CROSS_DOMAIN_C', '跨域应用C', 'APP', 2, 1, 'ENABLED', '用于异构跨域访问演示')
ON DUPLICATE KEY UPDATE
    app_name = VALUES(app_name),
    resource_type = VALUES(resource_type),
    security_level = VALUES(security_level),
    required_trust_level = VALUES(required_trust_level),
    status = VALUES(status),
    description = VALUES(description);

SET FOREIGN_KEY_CHECKS = 1;
