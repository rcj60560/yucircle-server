USE yucircle;

SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS post_like;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS sms_code;
DROP TABLE IF EXISTS user;

SET FOREIGN_KEY_CHECKS=1;

CREATE TABLE user (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    phone           VARCHAR(11)  NOT NULL,
    nickname        VARCHAR(50),
    avatar          VARCHAR(500),
    bio             VARCHAR(200),
    badminton_level VARCHAR(20),
    is_active       TINYINT(1)   DEFAULT 1,
    created_at      DATETIME,
    updated_at      DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sms_code (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    phone      VARCHAR(11) NOT NULL,
    code       VARCHAR(6)  NOT NULL,
    attempts   INT         DEFAULT 0,
    is_used    TINYINT(1)  DEFAULT 0,
    expires_at DATETIME    NOT NULL,
    created_at DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    images        VARCHAR(2000),
    category      VARCHAR(50),
    like_count    INT          DEFAULT 0,
    dislike_count INT          DEFAULT 0,
    comment_count INT          DEFAULT 0,
    view_count    INT          DEFAULT 0,
    status        VARCHAR(20)  DEFAULT 'published',
    created_at    DATETIME,
    updated_at    DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comment (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    post_id       BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    content       TEXT   NOT NULL,
    parent_id     BIGINT,
    root_id       BIGINT,
    like_count    INT    DEFAULT 0,
    dislike_count INT    DEFAULT 0,
    status        VARCHAR(20) DEFAULT 'published',
    created_at    DATETIME,
    updated_at    DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_like (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    object_type VARCHAR(20) NOT NULL,
    object_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    type        VARCHAR(20) NOT NULL,
    created_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_object_user (object_type, object_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
