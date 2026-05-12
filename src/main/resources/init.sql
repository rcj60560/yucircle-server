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

-- Phase 4: 球馆表
CREATE TABLE IF NOT EXISTS `venue` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100) NOT NULL COMMENT '球馆名称',
    `city`       VARCHAR(50)  NOT NULL COMMENT '城市',
    `address`    VARCHAR(200)          COMMENT '地址',
    `latitude`   DECIMAL(10, 8)        COMMENT '纬度',
    `longitude`  DECIMAL(11, 8)        COMMENT '经度',
    `court_count` INT         NOT NULL COMMENT '场地数',
    `status`     VARCHAR(20)  DEFAULT 'active' COMMENT '状态',
    `created_at` DATETIME     COMMENT '创建时间',
    `updated_at` DATETIME     COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球馆表';

-- 俱乐部表
CREATE TABLE IF NOT EXISTS `club` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `venue_id`          BIGINT       NOT NULL COMMENT '球馆ID',
    `name`              VARCHAR(100) NOT NULL COMMENT '俱乐部名称',
    `creator_id`        BIGINT       NOT NULL COMMENT '创建者用户ID',
    `description`       VARCHAR(500)          COMMENT '俱乐部描述',
    `status`            VARCHAR(20)  DEFAULT 'active' COMMENT '状态: pending/active/frozen/deleted',
    `broadcast_credits` INT          DEFAULT 0 COMMENT '广播次数预留',
    `created_at`        DATETIME     COMMENT '创建时间',
    `updated_at`        DATETIME     COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_venue_id` (`venue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='俱乐部表';

-- 活动表
CREATE TABLE IF NOT EXISTS `activity` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `venue_id`         BIGINT       NOT NULL COMMENT '球馆ID',
    `club_id`          BIGINT                 COMMENT '俱乐部ID, 可为空',
    `creator_id`       BIGINT       NOT NULL COMMENT '活动创建者用户ID',
    `title`            VARCHAR(200) NOT NULL COMMENT '活动标题',
    `activity_date`    DATE         NOT NULL COMMENT '活动日期',
    `time_slot`        VARCHAR(20)  DEFAULT 'EVENING' COMMENT '时间段',
    `level_requirement` VARCHAR(20)           COMMENT '等级要求',
    `match_type`       VARCHAR(20)  DEFAULT 'DOUBLES' COMMENT '比赛类型',
    `court_count`      INT          NOT NULL COMMENT '场地数',
    `max_per_court`    INT          DEFAULT 6 COMMENT '每场最大人数',
    `status`           VARCHAR(20)  DEFAULT 'open' COMMENT '状态',
    `created_at`       DATETIME     COMMENT '创建时间',
    `updated_at`       DATETIME     COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_club_id` (`club_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- 活动坑位表
CREATE TABLE IF NOT EXISTS `activity_slot` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `activity_id`     BIGINT       NOT NULL COMMENT '活动ID',
    `court_number`    INT          NOT NULL COMMENT '场地号',
    `slot_number`     INT          NOT NULL COMMENT '坑位号',
    `user_id`         BIGINT                 COMMENT '用户ID',
    `slot_status`     VARCHAR(20)  DEFAULT 'empty' COMMENT '坑位状态: empty/reserved/confirmed/locked',
    `lock_expires_at` DATETIME               COMMENT '锁定过期时间',
    `created_at`      DATETIME     COMMENT '创建时间',
    `updated_at`      DATETIME     COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_court_slot` (`activity_id`, `court_number`, `slot_number`),
    UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动坑位表';

-- 活动在线状态表
CREATE TABLE IF NOT EXISTS `activity_presence` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `activity_id`   BIGINT       NOT NULL COMMENT '活动ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `last_ping_at`  DATETIME     NOT NULL COMMENT '最后心跳时间',
    `created_at`    DATETIME     COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动在线状态表';

-- 活动聊天消息表
CREATE TABLE IF NOT EXISTS `activity_message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `activity_id` BIGINT       NOT NULL COMMENT '活动ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `content`     VARCHAR(500) NOT NULL COMMENT '消息内容',
    `created_at`  DATETIME     COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动聊天消息表';

-- 俱乐部广播表
CREATE TABLE IF NOT EXISTS `broadcast` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `club_id`    BIGINT       NOT NULL COMMENT '俱乐部ID',
    `sender_id`  BIGINT       NOT NULL COMMENT '发送者用户ID',
    `content`    VARCHAR(500) NOT NULL COMMENT '广播内容',
    `expires_at` DATETIME     NOT NULL COMMENT '过期时间',
    `created_at` DATETIME     COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_club_id` (`club_id`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='俱乐部广播表';

-- 初始化测试数据
INSERT INTO `venue` (`name`, `city`, `address`, `court_count`, `status`, `created_at`, `updated_at`)
VALUES ('晴天羽毛球馆', '成都', '成都市武侯区', 10, 'active', NOW(), NOW());

INSERT INTO `club` (`venue_id`, `name`, `creator_id`, `description`, `status`, `created_at`, `updated_at`)
VALUES
    (1, '俱乐部 A', 1, '俱乐部 A 描述', 'active', NOW(), NOW()),
    (1, '俱乐部 B', 1, '俱乐部 B 描述', 'active', NOW(), NOW()),
    (1, '俱乐部 C', 1, '俱乐部 C 描述', 'active', NOW(), NOW());

INSERT INTO `activity` (`venue_id`, `club_id`, `creator_id`, `title`, `activity_date`, `time_slot`, `match_type`, `court_count`, `max_per_court`, `status`, `created_at`, `updated_at`)
VALUES
    (1, 1, 1, '俱乐部 A 今晚活动', CURDATE(), 'EVENING', 'DOUBLES', 10, 6, 'open', NOW(), NOW()),
    (1, 2, 1, '俱乐部 B 今晚活动', CURDATE(), 'EVENING', 'DOUBLES', 10, 6, 'open', NOW(), NOW()),
    (1, 3, 1, '俱乐部 C 今晚活动', CURDATE(), 'EVENING', 'DOUBLES', 10, 6, 'open', NOW(), NOW());
