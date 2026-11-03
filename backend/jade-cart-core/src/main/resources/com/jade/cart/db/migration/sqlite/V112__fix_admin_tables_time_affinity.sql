-- =====================================================
-- V112: 底座 sys_* 表时间列亲和修复 (SQLite 方言)
-- 底座 sqlite 迁移把时间列建成 TEXT 亲和, xerial 写入的 epoch 毫秒
-- 被转成数字字符串, 读取 getTimestamp 解析失败 (500)。
-- 重建为 TIMESTAMP (NUMERIC 亲和), 存量数据自愈: 纯数字串会转为
-- INTEGER 存储, 日期串原样保留, 两种格式均可解析。
-- =====================================================

-- (PRAGMA foreign_keys 非事务语句不能与 DDL 混用; sys_* 表无外键, 无需关闭)

-- sys_config: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_config" RENAME TO "sys_config__old";
CREATE TABLE sys_config (
    id INTEGER PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(20) DEFAULT 'system',  -- system / business
    is_builtin SMALLINT DEFAULT 0,              -- 1=系统内置 0=自定义
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE (config_key, deleted)
);
INSERT INTO "sys_config" ("id", "config_name", "config_key", "config_value", "config_type", "is_builtin", "remark", "created_at", "updated_at", "deleted") SELECT "id", "config_name", "config_key", "config_value", "config_type", "is_builtin", "remark", "created_at", "updated_at", "deleted" FROM "sys_config__old";
DROP TABLE "sys_config__old";

-- sys_dept: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_dept" RENAME TO "sys_dept__old";
CREATE TABLE sys_dept (
    id INTEGER PRIMARY KEY,
    tenant_id BIGINT,
    parent_id BIGINT DEFAULT 0,
    dept_name VARCHAR(50) NOT NULL,
    dept_code VARCHAR(50),
    sort_order INT DEFAULT 0,
    leader_user_id BIGINT,
    phone VARCHAR(20),
    email VARCHAR(100),
    status SMALLINT DEFAULT 1,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE (tenant_id, dept_code, deleted)
);
INSERT INTO "sys_dept" ("id", "tenant_id", "parent_id", "dept_name", "dept_code", "sort_order", "leader_user_id", "phone", "email", "status", "remark", "created_at", "updated_at", "deleted") SELECT "id", "tenant_id", "parent_id", "dept_name", "dept_code", "sort_order", "leader_user_id", "phone", "email", "status", "remark", "created_at", "updated_at", "deleted" FROM "sys_dept__old";
DROP TABLE "sys_dept__old";

-- sys_dict_data: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_dict_data" RENAME TO "sys_dict_data__old";
CREATE TABLE sys_dict_data (
    id INTEGER PRIMARY KEY,
    dict_type VARCHAR(100) NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    dict_value VARCHAR(100) NOT NULL,
    css_class VARCHAR(50),
    sort_order INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    is_default SMALLINT DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);
INSERT INTO "sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "css_class", "sort_order", "status", "is_default", "remark", "created_at", "updated_at", "deleted") SELECT "id", "dict_type", "dict_label", "dict_value", "css_class", "sort_order", "status", "is_default", "remark", "created_at", "updated_at", "deleted" FROM "sys_dict_data__old";
DROP TABLE "sys_dict_data__old";

-- sys_dict_type: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_dict_type" RENAME TO "sys_dict_type__old";
CREATE TABLE sys_dict_type (
    id INTEGER PRIMARY KEY,
    dict_name VARCHAR(100) NOT NULL,
    dict_type VARCHAR(100) NOT NULL,
    status SMALLINT DEFAULT 1,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE (dict_type, deleted)
);
INSERT INTO "sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "created_at", "updated_at", "deleted") SELECT "id", "dict_name", "dict_type", "status", "remark", "created_at", "updated_at", "deleted" FROM "sys_dict_type__old";
DROP TABLE "sys_dict_type__old";

-- sys_ldap_config: TEXT 时间列 -> TIMESTAMP (updated_at)
ALTER TABLE "sys_ldap_config" RENAME TO "sys_ldap_config__old";
CREATE TABLE sys_ldap_config (
    id                 INTEGER      PRIMARY KEY,
    ldap_url           VARCHAR(300) NOT NULL,
    base_dn            VARCHAR(300),
    bind_dn            VARCHAR(300),
    bind_password      VARCHAR(512),
    user_search_filter VARCHAR(300) NOT NULL DEFAULT '(uid={0})',
    auto_create_user   BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at         TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP
, role_mappings TEXT);
INSERT INTO "sys_ldap_config" ("id", "ldap_url", "base_dn", "bind_dn", "bind_password", "user_search_filter", "auto_create_user", "enabled", "updated_at", "role_mappings") SELECT "id", "ldap_url", "base_dn", "bind_dn", "bind_password", "user_search_filter", "auto_create_user", "enabled", "updated_at", "role_mappings" FROM "sys_ldap_config__old";
DROP TABLE "sys_ldap_config__old";

-- sys_login_log: TEXT 时间列 -> TIMESTAMP (login_time)
ALTER TABLE "sys_login_log" RENAME TO "sys_login_log__old";
CREATE TABLE sys_login_log (
    id INTEGER PRIMARY KEY,
    tenant_id BIGINT,
    username VARCHAR(50),
    ip VARCHAR(50),
    location VARCHAR(200),
    browser VARCHAR(100),
    os VARCHAR(100),
    status SMALLINT DEFAULT 1,              -- 1=成功 0=失败
    msg VARCHAR(500),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO "sys_login_log" ("id", "tenant_id", "username", "ip", "location", "browser", "os", "status", "msg", "login_time") SELECT "id", "tenant_id", "username", "ip", "location", "browser", "os", "status", "msg", "login_time" FROM "sys_login_log__old";
DROP TABLE "sys_login_log__old";

-- sys_mail_config: TEXT 时间列 -> TIMESTAMP (updated_at)
ALTER TABLE "sys_mail_config" RENAME TO "sys_mail_config__old";
CREATE TABLE sys_mail_config (
    id            INTEGER      PRIMARY KEY,
    host          VARCHAR(200) NOT NULL,
    port          INTEGER      NOT NULL DEFAULT 465,
    username      VARCHAR(200),
    password      VARCHAR(512),
    from_address  VARCHAR(200),
    encrypt_type  VARCHAR(20)  NOT NULL DEFAULT 'SSL',
    enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at    TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO "sys_mail_config" ("id", "host", "port", "username", "password", "from_address", "encrypt_type", "enabled", "updated_at") SELECT "id", "host", "port", "username", "password", "from_address", "encrypt_type", "enabled", "updated_at" FROM "sys_mail_config__old";
DROP TABLE "sys_mail_config__old";

-- sys_menu: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_menu" RENAME TO "sys_menu__old";
CREATE TABLE sys_menu (
    id INTEGER PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    menu_name VARCHAR(50) NOT NULL,
    menu_type VARCHAR(1) NOT NULL,           -- M=目录 C=菜单 F=按钮
    path VARCHAR(200),                    -- 前端路由
    component VARCHAR(200),               -- 前端组件路径
    icon VARCHAR(50),
    perms VARCHAR(100),                   -- 权限标识 system:user:list
    sort_order INT DEFAULT 0,
    visible SMALLINT DEFAULT 1,
    status SMALLINT DEFAULT 1,
    is_cache SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);
INSERT INTO "sys_menu" ("id", "parent_id", "menu_name", "menu_type", "path", "component", "icon", "perms", "sort_order", "visible", "status", "is_cache", "created_at", "updated_at", "deleted") SELECT "id", "parent_id", "menu_name", "menu_type", "path", "component", "icon", "perms", "sort_order", "visible", "status", "is_cache", "created_at", "updated_at", "deleted" FROM "sys_menu__old";
DROP TABLE "sys_menu__old";

-- sys_notice: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_notice" RENAME TO "sys_notice__old";
CREATE TABLE sys_notice (
    id INTEGER PRIMARY KEY,
    tenant_id BIGINT,
    notice_title VARCHAR(200) NOT NULL,
    notice_type SMALLINT DEFAULT 1,         -- 1=通知 2=公告
    notice_content TEXT,
    status SMALLINT DEFAULT 1,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);
INSERT INTO "sys_notice" ("id", "tenant_id", "notice_title", "notice_type", "notice_content", "status", "created_by", "created_at", "updated_at", "deleted") SELECT "id", "tenant_id", "notice_title", "notice_type", "notice_content", "status", "created_by", "created_at", "updated_at", "deleted" FROM "sys_notice__old";
DROP TABLE "sys_notice__old";

-- sys_oper_log: TEXT 时间列 -> TIMESTAMP (oper_time)
ALTER TABLE "sys_oper_log" RENAME TO "sys_oper_log__old";
CREATE TABLE sys_oper_log (
    id INTEGER PRIMARY KEY,
    tenant_id BIGINT,
    title VARCHAR(100),
    business_type SMALLINT DEFAULT 0,        -- 0=其它 1=新增 2=修改 3=删除 4=查询 5=导出
    method VARCHAR(200),                     -- 调用方法/类
    request_method VARCHAR(10),              -- HTTP method
    request_url VARCHAR(500),
    request_param TEXT,
    response_result TEXT,
    error_msg TEXT,
    user_id BIGINT,
    username VARCHAR(50),
    ip VARCHAR(50),
    location VARCHAR(200),
    user_agent VARCHAR(500),
    duration_ms BIGINT,
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO "sys_oper_log" ("id", "tenant_id", "title", "business_type", "method", "request_method", "request_url", "request_param", "response_result", "error_msg", "user_id", "username", "ip", "location", "user_agent", "duration_ms", "oper_time") SELECT "id", "tenant_id", "title", "business_type", "method", "request_method", "request_url", "request_param", "response_result", "error_msg", "user_id", "username", "ip", "location", "user_agent", "duration_ms", "oper_time" FROM "sys_oper_log__old";
DROP TABLE "sys_oper_log__old";

-- sys_oss: TEXT 时间列 -> TIMESTAMP (upload_time)
ALTER TABLE "sys_oss" RENAME TO "sys_oss__old";
CREATE TABLE sys_oss (
    id INTEGER PRIMARY KEY,
    tenant_id BIGINT,
    file_name VARCHAR(200),
    original_name VARCHAR(200),
    file_suffix VARCHAR(20),
    file_size BIGINT,
    url VARCHAR(500),
    storage_type VARCHAR(20) DEFAULT 'LOCAL',  -- LOCAL / MINIO / ALIYUN / S3
    storage_path VARCHAR(500),
    bucket VARCHAR(100),
    content_type VARCHAR(100),
    upload_by VARCHAR(50),
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);
INSERT INTO "sys_oss" ("id", "tenant_id", "file_name", "original_name", "file_suffix", "file_size", "url", "storage_type", "storage_path", "bucket", "content_type", "upload_by", "upload_time", "deleted") SELECT "id", "tenant_id", "file_name", "original_name", "file_suffix", "file_size", "url", "storage_type", "storage_path", "bucket", "content_type", "upload_by", "upload_time", "deleted" FROM "sys_oss__old";
DROP TABLE "sys_oss__old";

-- sys_patient: TEXT 时间列 -> TIMESTAMP (created_at)
ALTER TABLE "sys_patient" RENAME TO "sys_patient__old";
CREATE TABLE sys_patient (
    id          INTEGER      PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    id_card     VARCHAR(512),
    phone       VARCHAR(512),
    diagnosis   VARCHAR(256),
    created_at  TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO "sys_patient" ("id", "name", "id_card", "phone", "diagnosis", "created_at") SELECT "id", "name", "id_card", "phone", "diagnosis", "created_at" FROM "sys_patient__old";
DROP TABLE "sys_patient__old";

-- sys_project: TEXT 时间列 -> TIMESTAMP (created_at)
ALTER TABLE "sys_project" RENAME TO "sys_project__old";
CREATE TABLE sys_project (
    id          INTEGER      PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    created_at  TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP
, deleted BOOLEAN NOT NULL DEFAULT FALSE);
INSERT INTO "sys_project" ("id", "tenant_id", "name", "description", "created_at", "deleted") SELECT "id", "tenant_id", "name", "description", "created_at", "deleted" FROM "sys_project__old";
DROP TABLE "sys_project__old";

-- sys_role: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_role" RENAME TO "sys_role__old";
CREATE TABLE sys_role (
    id INTEGER PRIMARY KEY,
    tenant_id BIGINT,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    role_sort INT DEFAULT 0,
    data_scope VARCHAR(20) DEFAULT 'ALL',  -- ALL / DEPT_AND_CHILD / DEPT / SELF
    status SMALLINT DEFAULT 1,             -- 0=停用 1=正常
    remark VARCHAR(500),
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE (tenant_id, role_code, deleted)
);
INSERT INTO "sys_role" ("id", "tenant_id", "role_name", "role_code", "role_sort", "data_scope", "status", "remark", "created_by", "created_at", "updated_by", "updated_at", "deleted") SELECT "id", "tenant_id", "role_name", "role_code", "role_sort", "data_scope", "status", "remark", "created_by", "created_at", "updated_by", "updated_at", "deleted" FROM "sys_role__old";
DROP TABLE "sys_role__old";

-- sys_task: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_task" RENAME TO "sys_task__old";
CREATE TABLE sys_task (
    id INTEGER PRIMARY KEY,
    tenant_id BIGINT,
    task_name VARCHAR(100) NOT NULL,
    task_group VARCHAR(50) DEFAULT 'DEFAULT',
    invoke_target VARCHAR(500) NOT NULL,    -- bean.method 或 class.method
    cron_expression VARCHAR(100),
    misfire_policy VARCHAR(20) DEFAULT '3', -- 1=立即执行 2=执行一次 3=放弃执行
    concurrent SMALLINT DEFAULT 0,          -- 0=禁止 1=允许
    status SMALLINT DEFAULT 1,              -- 0=暂停 1=运行
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);
INSERT INTO "sys_task" ("id", "tenant_id", "task_name", "task_group", "invoke_target", "cron_expression", "misfire_policy", "concurrent", "status", "remark", "created_at", "updated_at", "deleted") SELECT "id", "tenant_id", "task_name", "task_group", "invoke_target", "cron_expression", "misfire_policy", "concurrent", "status", "remark", "created_at", "updated_at", "deleted" FROM "sys_task__old";
DROP TABLE "sys_task__old";

-- sys_tenant: TEXT 时间列 -> TIMESTAMP (created_at)
ALTER TABLE "sys_tenant" RENAME TO "sys_tenant__old";
CREATE TABLE sys_tenant (
    id          INTEGER      PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 1,
    created_at  TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO "sys_tenant" ("id", "code", "name", "status", "created_at") SELECT "id", "code", "name", "status", "created_at" FROM "sys_tenant__old";
DROP TABLE "sys_tenant__old";

-- sys_user: TEXT 时间列 -> TIMESTAMP (created_at, updated_at)
ALTER TABLE "sys_user" RENAME TO "sys_user__old";
CREATE TABLE sys_user (
    id          INTEGER      PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(32),
    status      SMALLINT     NOT NULL DEFAULT 1,
    created_at  TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE
, tenant_id BIGINT, auth_source VARCHAR(20) NOT NULL DEFAULT 'local');
INSERT INTO "sys_user" ("id", "username", "password", "nickname", "email", "phone", "status", "created_at", "updated_at", "deleted", "tenant_id", "auth_source") SELECT "id", "username", "password", "nickname", "email", "phone", "status", "created_at", "updated_at", "deleted", "tenant_id", "auth_source" FROM "sys_user__old";
DROP TABLE "sys_user__old";


-- 共重建 17 表: sys_config, sys_dept, sys_dict_data, sys_dict_type, sys_ldap_config, sys_login_log, sys_mail_config, sys_menu, sys_notice, sys_oper_log, sys_oss, sys_patient, sys_project, sys_role, sys_task, sys_tenant, sys_user
