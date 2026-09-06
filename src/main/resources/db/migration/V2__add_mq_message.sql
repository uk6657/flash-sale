CREATE TABLE IF NOT EXISTS mq_message (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '业务消息ID',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    message_body TEXT NOT NULL COMMENT '消息内容',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态：0待发送，1已发送，2发送失败，3最终失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '发送重试次数',
    fail_reason VARCHAR(512) NOT NULL DEFAULT '' COMMENT '失败原因',
    next_retry_time DATETIME(3) NULL COMMENT '下次重试时间',
    sent_at DATETIME(3) NULL COMMENT '发送成功时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mq_message_message_id (message_id),
    KEY idx_mq_message_status_retry_time (status, next_retry_time),
    KEY idx_mq_message_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MQ本地消息表';
