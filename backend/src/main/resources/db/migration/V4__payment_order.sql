-- 支付订单表：对接虎皮椒等第三方支付
CREATE TABLE payment_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL COMMENT '商户订单号（唯一）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PAID/EXPIRED/FAILED',
    channel VARCHAR(16) NOT NULL DEFAULT 'xunhupay' COMMENT '支付渠道',
    transaction_id VARCHAR(64) DEFAULT NULL COMMENT '第三方支付流水号',
    qrcode_url VARCHAR(512) DEFAULT NULL COMMENT '支付二维码链接',
    pay_url VARCHAR(512) DEFAULT NULL COMMENT '手机端支付跳转链接',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME DEFAULT NULL COMMENT '支付完成时间',
    expired_at DATETIME DEFAULT NULL COMMENT '过期时间',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 给 recharge_record 加一个 order_no 字段关联支付订单
ALTER TABLE recharge_record ADD COLUMN order_no VARCHAR(32) DEFAULT NULL COMMENT '关联支付订单号' AFTER balance_after;
