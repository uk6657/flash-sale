CREATE TABLE IF NOT EXISTS mq_consume_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '业务消息ID',
    queue_name VARCHAR(128) NOT NULL COMMENT '消费队列',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    order_id BIGINT UNSIGNED NULL COMMENT '关联订单ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '消费状态：1消费成功',
    consume_time DATETIME(3) NOT NULL COMMENT '消费成功时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_consume_log_message_id (message_id),
    KEY idx_consume_log_queue_time (queue_name, consume_time),
    KEY idx_consume_log_order_id (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MQ消费幂等日志表';
