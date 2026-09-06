CREATE TABLE IF NOT EXISTS mq_dead_message (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务消息ID',
    queue_name VARCHAR(128) NOT NULL COMMENT '死信来源队列',
    exchange_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '原始交换机',
    routing_key VARCHAR(128) NOT NULL DEFAULT '' COMMENT '原始路由键',
    message_body TEXT NOT NULL COMMENT '消息内容',
    fail_reason VARCHAR(512) NOT NULL DEFAULT '' COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0未处理，1已处理',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dead_message_message_id (message_id),
    KEY idx_dead_message_status_id (status, id),
    KEY idx_dead_message_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MQ死信消息表';
