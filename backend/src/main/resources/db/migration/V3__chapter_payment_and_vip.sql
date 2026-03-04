-- 作品级别：免费章节数 + 默认每章价格
ALTER TABLE content
    ADD COLUMN free_chapter_count INT NOT NULL DEFAULT 0 AFTER is_paid,
    ADD COLUMN default_chapter_price DECIMAL(10, 2) DEFAULT NULL AFTER free_chapter_count;

-- 章节级别：可单独设价（NULL 表示使用作品默认价格）
ALTER TABLE novel_chapter
    ADD COLUMN price DECIMAL(10, 2) DEFAULT NULL AFTER chapter_summary;

-- 用户 VIP 到期时间（NULL 表示非 VIP）
ALTER TABLE user
    ADD COLUMN vip_expire_at DATETIME DEFAULT NULL AFTER balance;

-- 章节解锁记录
CREATE TABLE IF NOT EXISTS chapter_unlock (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT         NOT NULL,
    chapter_id    BIGINT         NOT NULL,
    price_paid    DECIMAL(10, 2) NOT NULL,
    unlocked_at   DATETIME       NOT NULL,
    UNIQUE KEY uk_user_chapter (user_id, chapter_id),
    INDEX idx_user (user_id),
    INDEX idx_chapter (chapter_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
