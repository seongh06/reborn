-- ================================================
-- SmartThings OAuth 자격증명 테이블 추가 (#130)
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 신규 테이블도 수동으로 적용해야 한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 002_add_smart_things_credential.sql
-- ================================================

CREATE TABLE IF NOT EXISTS `smart_things_credential`
(
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '자격증명 PK',
    `place_id`      BIGINT        NOT NULL COMMENT '장소 FK (장소당 1개)',
    `access_token`  VARCHAR(1024) NOT NULL COMMENT 'SmartThings OAuth AccessToken',
    `refresh_token` VARCHAR(1024) NOT NULL COMMENT 'SmartThings OAuth RefreshToken',
    `expires_at`    DATETIME(6)   NOT NULL COMMENT 'AccessToken 만료 시각',
    `created_at`    DATETIME(6)   NOT NULL COMMENT '연동일시',
    `updated_at`    DATETIME(6)   NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_smart_things_credential_place` (`place_id`),
    CONSTRAINT `fk_smart_things_credential_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '장소별 SmartThings OAuth 자격증명';

-- device.device_type은 VARCHAR(20)이라 SMART_THINGS 값 추가에 스키마 변경 불필요.

-- 적용 후 확인
-- SHOW CREATE TABLE smart_things_credential\G
