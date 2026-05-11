-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS yucircle DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yucircle;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`           VARCHAR(11)  NOT NULL COMMENT '手机号',
    `nickname`        VARCHAR(50)           COMMENT '昵称',
    `avatar`          VARCHAR(500)          COMMENT '头像URL',
    `bio`             VARCHAR(200)          COMMENT '个人简介',
    `badminton_level` VARCHAR(20)           COMMENT '羽毛球水平',
    `is_active`       TINYINT(1)   DEFAULT 1 COMMENT '是否激活',
    `created_at`      DATETIME              COMMENT '创建时间',
    `updated_at`      DATETIME              COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 短信验证码表
CREATE TABLE IF NOT EXISTS `sms_code` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `phone`      VARCHAR(11) NOT NULL COMMENT '手机号',
    `code`       VARCHAR(6)  NOT NULL COMMENT '验证码',
    `attempts`   INT         DEFAULT 0 COMMENT '验证次数',
    `is_used`    TINYINT(1)  DEFAULT 0 COMMENT '是否已使用',
    `expires_at` DATETIME    NOT NULL COMMENT '过期时间',
    `created_at` DATETIME    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码';

-- 帖子表
CREATE TABLE IF NOT EXISTS `post` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '发帖用户ID',
    `title`         VARCHAR(200) NOT NULL COMMENT '标题',
    `content`       TEXT         NOT NULL COMMENT '内容',
    `images`        VARCHAR(2000)         COMMENT '图片URL逗号分隔',
    `category`      VARCHAR(50)           COMMENT '分类',
    `like_count`    INT          DEFAULT 0 COMMENT '点赞数',
    `dislike_count` INT          DEFAULT 0 COMMENT '踩数',
    `comment_count` INT          DEFAULT 0 COMMENT '评论数',
    `view_count`    INT          DEFAULT 0 COMMENT '浏览量',
    `status`        VARCHAR(20)  DEFAULT 'published' COMMENT '状态',
    `created_at`    DATETIME     COMMENT '创建时间',
    `updated_at`    DATETIME     COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `post_id`       BIGINT NOT NULL COMMENT '帖子ID',
    `user_id`       BIGINT NOT NULL COMMENT '评论用户ID',
    `content`       TEXT   NOT NULL COMMENT '内容',
    `parent_id`     BIGINT         COMMENT '父评论ID',
    `root_id`       BIGINT         COMMENT '根评论ID',
    `like_count`    INT    DEFAULT 0,
    `dislike_count` INT    DEFAULT 0,
    `status`        VARCHAR(20) DEFAULT 'published',
    `created_at`    DATETIME,
    `updated_at`    DATETIME,
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 点赞/踩表
CREATE TABLE IF NOT EXISTS `post_like` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `object_type` VARCHAR(20) NOT NULL COMMENT 'post或comment',
    `object_id`   BIGINT      NOT NULL COMMENT '对象ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `type`        VARCHAR(20) NOT NULL COMMENT 'like或dislike',
    `created_at`  DATETIME,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_object_user` (`object_type`, `object_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞踩记录';
