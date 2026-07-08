-- ================================================
-- ReBorn Database Schema
-- Updated: 2025-05-27
-- ================================================

-- ------------------------------------------------
-- 1. user
-- refreshToken 제거 → Redis 관리로 이관
-- ------------------------------------------------
CREATE TABLE `user`
(
    `userId`    VARCHAR(255) NOT NULL COMMENT '소셜 로그인 고유 ID (kakao_123 / google_456)',
    `name`      VARCHAR(50)  NOT NULL COMMENT '사용자 이름',
    `email`     VARCHAR(100) NULL COMMENT '이메일 (소셜 제공 시)',
    `fcmToken`  VARCHAR(255) NULL COMMENT 'FCM Push 알림 토큰',
    `createdAt` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '가입일시',
    PRIMARY KEY (`userId`)
) COMMENT = '사용자 (refreshToken은 Redis에서 관리)';

-- ------------------------------------------------
-- 2. place
-- qrCode 추가 → QR 웹페이지 장소 식별용
-- ------------------------------------------------
CREATE TABLE `place`
(
    `placeId`   INT          NOT NULL AUTO_INCREMENT COMMENT '장소 ID',
    `name`      VARCHAR(100) NOT NULL COMMENT '장소명 (예: 강남 스터디카페 1호점)',
    `placeType` VARCHAR(20)  NOT NULL COMMENT '장소 유형 (CAFE / OFFICE / HOME 등)',
    `latitude`  DOUBLE       NOT NULL COMMENT '위도',
    `longitude` DOUBLE       NOT NULL COMMENT '경도',
    `qrCode`    VARCHAR(100) NULL COMMENT 'QR 웹페이지 식별 고유 코드 (UUID 등)',
    PRIMARY KEY (`placeId`),
    UNIQUE KEY `UQ_place_qrCode` (`qrCode`)
) COMMENT = '장소 정보';

-- ------------------------------------------------
-- 3. user_place_mapping
-- 사용자 ↔ 장소 다대다 관계 + 권한 관리
-- ------------------------------------------------
CREATE TABLE `user_place_mapping`
(
    `userId`      VARCHAR(255) NOT NULL COMMENT '사용자 ID',
    `placeId`     INT          NOT NULL COMMENT '장소 ID',
    `accessLevel` VARCHAR(20)  NOT NULL COMMENT '권한 (ADMIN / USER)',
    PRIMARY KEY (`userId`, `placeId`),
    CONSTRAINT `FK_user_TO_mapping`
        FOREIGN KEY (`userId`) REFERENCES `user` (`userId`) ON DELETE CASCADE,
    CONSTRAINT `FK_place_TO_mapping`
        FOREIGN KEY (`placeId`) REFERENCES `place` (`placeId`) ON DELETE CASCADE
) COMMENT = '사용자-장소 권한 매핑';

-- ------------------------------------------------
-- 4. device
-- deviceType 추가  → ARDUINO / AEROMETER 구분
-- appToken 추가   → 공기계 앱 WebSocket 연결 식별자
-- isOnline 추가   → 공기계 앱 온라인 상태
-- ------------------------------------------------
CREATE TABLE `device`
(
    `deviceId`   VARCHAR(50)  NOT NULL COMMENT '기기 고유 ID',
    `placeId`    INT          NOT NULL COMMENT '장소 ID',
    `deviceName` VARCHAR(50)  NOT NULL COMMENT '기기 이름 겸 방 이름 (예: 거실, 안방)',
    `deviceType` VARCHAR(20)  NOT NULL DEFAULT 'ARDUINO' COMMENT '기기 유형 (ARDUINO / AEROMETER)',
    `appToken`   VARCHAR(255) NULL COMMENT '공기계 앱 WebSocket 연결 식별자 (AEROMETER 전용)',
    `isOnline`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '현재 온라인 여부 (0: 오프라인, 1: 온라인)',
    `createdAt`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '기기 등록일시',
    PRIMARY KEY (`deviceId`),
    CONSTRAINT `FK_place_TO_device`
        FOREIGN KEY (`placeId`) REFERENCES `place` (`placeId`) ON DELETE CASCADE
) COMMENT = '기기 정보 (Arduino 센서 및 공기계 앱)';

-- ------------------------------------------------
-- 5. metric_logs
-- 복합 인덱스 추가 → 최신 데이터 조회 성능 확보
-- device_id NULL 허용 → 기기 삭제 시 로그 보존
-- ------------------------------------------------
CREATE TABLE `metric_logs`
(
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '로그 ID',
    `device_id`   VARCHAR(50)   NULL     COMMENT '기기 ID',
    `temperature` DECIMAL(4, 2) NULL     COMMENT '온도 (°C)',
    `humidity`    DECIMAL(4, 2) NULL     COMMENT '습도 (%)',
    `illuminance` INT           NULL     COMMENT '조도 (lux)',
    `occupancy`   INT           NULL     COMMENT '재실 인원 수',
    `created_at`  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '수집일시',
    `updated_at`  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (`id`),
    INDEX `idx_metric_logs_device_id_created_at` (`device_id`, `created_at` DESC) COMMENT '기기별 최신 로그 조회 최적화',
    CONSTRAINT `FK_device_TO_metric_logs`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`deviceId`) ON DELETE SET NULL
) COMMENT = '메트릭 수집 로그';

-- ------------------------------------------------
-- 6. feedback
-- userAgent 추가  → 브라우저 핑거프린트 보조
-- sessionToken 추가 → QR 접속 시 발급 임시 토큰 (중복 방지)
-- ------------------------------------------------
CREATE TABLE `feedback`
(
    `feedbackId`   INT          NOT NULL AUTO_INCREMENT COMMENT '피드백 ID',
    `deviceId`     VARCHAR(50)  NOT NULL COMMENT '기기 ID',
    `title`        VARCHAR(150) NOT NULL COMMENT '피드백 제목',
    `content`      TEXT         NULL COMMENT '피드백 내용',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태 (PENDING / APPROVED / REJECTED)',
    `userIp`       VARCHAR(45)  NOT NULL COMMENT '작성자 IP (IPv6 포함)',
    `userAgent`    VARCHAR(255) NULL COMMENT '브라우저 User-Agent (중복 방지 보조)',
    `sessionToken` VARCHAR(100) NULL COMMENT 'QR 접속 시 발급된 임시 세션 토큰',
    `createdAt`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작성일시',
    PRIMARY KEY (`feedbackId`),
    INDEX `idx_feedback_deviceId_status` (`deviceId`, `status`) COMMENT '기기별 상태 필터 조회 최적화',
    CONSTRAINT `FK_device_TO_feedback`
        FOREIGN KEY (`deviceId`) REFERENCES `device` (`deviceId`) ON DELETE CASCADE
) COMMENT = '방문자 피드백';


-- ================================================
-- Redis 관리 항목 (DDL 외)
-- ================================================
-- KEY  : refresh:{userId}
-- VALUE: refreshToken (String)
-- TTL  : 14일 (1,209,600초)
--
-- KEY  : session:qr:{sessionToken}
-- VALUE: placeId (String)
-- TTL  : 1시간 (3,600초) — QR 접속 임시 세션
-- ================================================
