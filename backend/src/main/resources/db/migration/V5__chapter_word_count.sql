ALTER TABLE generation_config
    ADD COLUMN chapter_word_count INT DEFAULT 2000 COMMENT '每章目标字数（默认2000字）' AFTER max_tokens;

UPDATE generation_config SET max_tokens = 8192 WHERE max_tokens <= 2000;
