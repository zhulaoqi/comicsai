-- V2__add_admin_and_oauth.sql

-- admin_user table
CREATE TABLE IF NOT EXISTS `admin_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(100) NOT NULL DEFAULT '管理员',
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- oauth_account table: links social accounts to users
CREATE TABLE IF NOT EXISTS `oauth_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` VARCHAR(20) NOT NULL COMMENT 'github / google',
    `provider_user_id` VARCHAR(255) NOT NULL,
    `email` VARCHAR(255) NULL,
    `nickname` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_user_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default admin account: admin@comicsai.com / admin123456
INSERT INTO `admin_user` (`email`, `password_hash`, `nickname`, `created_at`, `updated_at`)
VALUES (
    'admin@comicsai.com',
    '$2a$10$aIjRHPSOmVC6TnMxE0Ced.o35vvcNjpqb4Mu4Y/hUKs9udUBxnLKi',
    '超级管理员',
    NOW(),
    NOW()
);
