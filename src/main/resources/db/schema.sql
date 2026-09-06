CREATE DATABASE IF NOT EXISTS flash_sale
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE flash_sale;

DROP TABLE IF EXISTS flash_sale_result;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS mq_dead_message;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS flash_sale_activity;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    nickname VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户昵称',
    role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '用户角色：USER普通用户，ADMIN管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：1启用，0禁用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

CREATE TABLE product (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    name VARCHAR(128) NOT NULL COMMENT '商品名称',
    description VARCHAR(512) NOT NULL DEFAULT '' COMMENT '商品描述',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品原价',
    stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '商品总库存',
    lock_stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '被冻结的库存',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态：1上架，0下架',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_name (name),
    KEY idx_product_status_id (status, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品表';

CREATE TABLE flash_sale_activity (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    sale_price DECIMAL(10, 2) NOT NULL COMMENT '秒杀价格',
    sale_stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    start_time DATETIME(3) NOT NULL COMMENT '活动开始时间',
    end_time DATETIME(3) NOT NULL COMMENT '活动结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '活动状态：1启用，0禁用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_activity_product_status (product_id, status),
    KEY idx_activity_status_id (status, id),
    KEY idx_activity_status_time (status, start_time, end_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '秒杀活动表';

CREATE TABLE orders (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    activity_id BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    price DECIMAL(10, 2) NOT NULL COMMENT '订单价格',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3支付失败',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no),
    UNIQUE KEY uk_orders_user_activity (user_id, activity_id),
    KEY idx_orders_activity_id (activity_id),
    KEY idx_orders_user_id_id (user_id, id),
    KEY idx_orders_product_id (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单表';

CREATE TABLE flash_sale_result (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    activity_id BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '秒杀结果状态：0排队中，1成功，2失败',
    message VARCHAR(255) NOT NULL DEFAULT '' COMMENT '结果说明',
    order_id BIGINT UNSIGNED NULL COMMENT '成功后关联的订单ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_user_activity (user_id, activity_id),
    KEY idx_result_activity_status (activity_id, status),
    KEY idx_result_order_id (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '秒杀结果表';

CREATE TABLE operation_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    user_id BIGINT UNSIGNED NULL COMMENT '用户ID',
    operation VARCHAR(128) NOT NULL COMMENT '操作名称',
    request_uri VARCHAR(255) NOT NULL COMMENT '请求地址',
    request_method VARCHAR(16) NOT NULL COMMENT '请求方法',
    ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP地址',
    trace_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '请求链路追踪ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_log_user_id (user_id),
    KEY idx_log_trace_id (trace_id),
    KEY idx_log_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表';
