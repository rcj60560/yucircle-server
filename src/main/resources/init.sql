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

-- ============ BWF 数据落盘（2026-08-19，设计见 doc/06）============
CREATE TABLE IF NOT EXISTS `bwf_tournament` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `tmt_id`          INT          NOT NULL COMMENT 'BWF数字赛事id',
    `name`            VARCHAR(200) NOT NULL COMMENT '赛事名',
    `level`           VARCHAR(20)           COMMENT 'major/super1000/super750/super500/super300/finals/other',
    `start_date`      DATE                  COMMENT '开始日期',
    `end_date`        DATE                  COMMENT '结束日期',
    `city`            VARCHAR(100)          COMMENT '城市',
    `country`         VARCHAR(100)          COMMENT '国家',
    `prize_money`     INT          DEFAULT 0 COMMENT '总奖金(美元)',
    `code`            VARCHAR(64)           COMMENT 'extranet UUID赛事码',
    `has_live_scores` TINYINT(1)   DEFAULT 0 COMMENT '是否有live比分',
    `created_at`      DATETIME              COMMENT '创建时间',
    `updated_at`      DATETIME              COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tmt_id` (`tmt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BWF赛事';

CREATE TABLE IF NOT EXISTS `bwf_ranking_entry` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `discipline`       VARCHAR(4)   NOT NULL COMMENT 'ms/ws/md/wd/xd',
    `publication_date` DATE         NOT NULL COMMENT '发布日期（周key的date）',
    `rank_num`         INT          NOT NULL COMMENT '排名（rank 为 MySQL8 保留字）',
    `rank_change`      INT          DEFAULT 0 COMMENT '升降',
    `country`          VARCHAR(50)           COMMENT '国家码',
    `player_name`      VARCHAR(200) NOT NULL COMMENT '球员名（双打拼接 A / B）',
    `points`           INT          DEFAULT 0 COMMENT '积分',
    `created_at`       DATETIME              COMMENT '创建时间',
    `updated_at`       DATETIME              COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pub` (`discipline`, `publication_date`, `rank_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BWF排名快照';

CREATE TABLE IF NOT EXISTS `bwf_match` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tmt_id`        INT          NOT NULL COMMENT 'BWF数字赛事id',
    `match_code`    VARCHAR(20)  NOT NULL COMMENT '场次码（赛事内唯一）',
    `order_no`      INT                   COMMENT '官方对阵顺序（day-matches 原序）',
    `match_date`    DATE                  COMMENT '赛事日（按日查询用）',
    `event`         VARCHAR(4)            COMMENT 'MS/WS/MD/WD/XD',
    `round_name`    VARCHAR(20)           COMMENT 'R64/QF/SF/F',
    `court_name`    VARCHAR(50)           COMMENT '场地',
    `match_time`    DATETIME              COMMENT '开赛时间(本地)',
    `status`        VARCHAR(2)            COMMENT 'F=已结束 P=进行中 N=未开赛',
    `winner`        TINYINT      DEFAULT 0 COMMENT '1/2，0=未定',
    `duration_min`  INT                   COMMENT '时长(分钟)',
    `team1_country` VARCHAR(10)           COMMENT '方1国家码',
    `team1_players` VARCHAR(200)          COMMENT '方1球员（双打拼接）',
    `team1_seed`    VARCHAR(4)            COMMENT '方1种子',
    `team2_country` VARCHAR(10)           COMMENT '方2国家码',
    `team2_players` VARCHAR(200)          COMMENT '方2球员（双打拼接）',
    `team2_seed`    VARCHAR(4)            COMMENT '方2种子',
    `score_text`    VARCHAR(50)           COMMENT '局分文本，如 21-15 19-21 21-18',
    `created_at`    DATETIME              COMMENT '创建时间',
    `updated_at`    DATETIME              COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_match` (`tmt_id`, `match_code`),
    KEY `idx_tmt_date` (`tmt_id`, `match_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BWF单场赛果';

CREATE TABLE IF NOT EXISTS `bwf_match_game` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `match_id`    BIGINT NOT NULL COMMENT 'bwf_match.id',
    `game_no`     INT    NOT NULL COMMENT '局号(1起)',
    `team1_score` INT             COMMENT '方1局分',
    `team2_score` INT             COMMENT '方2局分',
    `created_at`  DATETIME        COMMENT '创建时间',
    `updated_at`  DATETIME        COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game` (`match_id`, `game_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BWF局';

CREATE TABLE IF NOT EXISTS `bwf_match_point` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT,
    `game_id`    BIGINT NOT NULL COMMENT 'bwf_match_game.id',
    `ordering`   INT    NOT NULL COMMENT '第几分(1起)',
    `team1`      INT    DEFAULT 0 COMMENT '方1累计得分',
    `team2`      INT    DEFAULT 0 COMMENT '方2累计得分',
    `created_at` DATETIME        COMMENT '创建时间',
    `updated_at` DATETIME        COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_point` (`game_id`, `ordering`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BWF逐分序列（仅Grade 1大赛）';
