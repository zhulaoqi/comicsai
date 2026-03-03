-- V1__create_tables.sql (H2 compatible version for tests)

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(100) NOT NULL,
    `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`email`)
);

CREATE TABLE IF NOT EXISTS `storyline` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `genre` VARCHAR(100) NOT NULL,
    `content_type` VARCHAR(20) NOT NULL,
    `character_settings` TEXT NOT NULL,
    `worldview` TEXT NOT NULL,
    `plot_outline` TEXT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DISABLED',
    `latest_chapter_summary` TEXT NULL,
    `generated_count` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `storyline_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `storyline_id` BIGINT NOT NULL,
    `version_number` INT NOT NULL,
    `snapshot_json` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_storyline_version_storyline` FOREIGN KEY (`storyline_id`) REFERENCES `storyline` (`id`)
);

CREATE TABLE IF NOT EXISTS `generation_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `storyline_id` BIGINT NOT NULL,
    `text_provider` VARCHAR(50) NOT NULL,
    `text_model` VARCHAR(100) NOT NULL,
    `image_provider` VARCHAR(50) NULL,
    `image_model` VARCHAR(100) NULL,
    `temperature` DOUBLE DEFAULT 0.7,
    `max_tokens` INT DEFAULT 2000,
    `image_style` VARCHAR(100) NULL,
    `image_size` VARCHAR(20) NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`storyline_id`),
    CONSTRAINT `fk_generation_config_storyline` FOREIGN KEY (`storyline_id`) REFERENCES `storyline` (`id`)
);

CREATE TABLE IF NOT EXISTS `content` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `storyline_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content_type` VARCHAR(20) NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `cover_url` VARCHAR(500) NOT NULL,
    `description` TEXT NULL,
    `is_paid` BOOLEAN NOT NULL DEFAULT FALSE,
    `price` DECIMAL(10,2) NULL,
    `generated_at` DATETIME NOT NULL,
    `published_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_content_storyline` FOREIGN KEY (`storyline_id`) REFERENCES `storyline` (`id`)
);

CREATE TABLE IF NOT EXISTS `comic_page` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT NOT NULL,
    `page_number` INT NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `dialogue_text` TEXT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_comic_page_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`)
);

CREATE TABLE IF NOT EXISTS `novel_chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT NOT NULL,
    `chapter_number` INT NOT NULL,
    `chapter_title` VARCHAR(255) NOT NULL,
    `chapter_text` TEXT NOT NULL,
    `chapter_summary` TEXT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_novel_chapter_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`)
);

CREATE TABLE IF NOT EXISTS `view_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT NOT NULL,
    `user_id` BIGINT NULL,
    `duration_seconds` INT NULL,
    `viewed_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_view_event_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`)
);

CREATE TABLE IF NOT EXISTS `token_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT NULL,
    `storyline_id` BIGINT NOT NULL,
    `provider_name` VARCHAR(50) NOT NULL,
    `model_name` VARCHAR(100) NOT NULL,
    `input_tokens` INT NOT NULL,
    `output_tokens` INT NOT NULL,
    `estimated_cost` DECIMAL(10,4) NOT NULL,
    `called_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_token_usage_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`),
    CONSTRAINT `fk_token_usage_storyline` FOREIGN KEY (`storyline_id`) REFERENCES `storyline` (`id`)
);

CREATE TABLE IF NOT EXISTS `recharge_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(10,2) NOT NULL,
    `balance_after` DECIMAL(10,2) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_recharge_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

CREATE TABLE IF NOT EXISTS `content_unlock` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `content_id` BIGINT NOT NULL,
    `price_paid` DECIMAL(10,2) NOT NULL,
    `unlocked_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`user_id`, `content_id`),
    CONSTRAINT `fk_content_unlock_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_content_unlock_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`id`)
);
