ALTER TABLE novel_chapter
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '章节状态：DRAFT/PUBLISHED' AFTER chapter_summary;

CREATE INDEX idx_novel_chapter_status ON novel_chapter (status);
