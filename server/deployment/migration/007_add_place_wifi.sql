-- ================================================
-- place 테이블에 아두이노/AI스피커 SoftAP 프로비저닝용 WiFi 컬럼 추가
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 컬럼 변경도 수동으로 적용해야 한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 007_add_place_wifi.sql
-- ================================================

ALTER TABLE `place`
    ADD COLUMN `wifi_ssid` VARCHAR(64) NULL COMMENT '이 장소 기기들이 사용할 홈 WiFi SSID(#219)' AFTER `description`,
    ADD COLUMN `wifi_password` VARCHAR(64) NULL COMMENT '이 장소 기기들이 사용할 홈 WiFi 비밀번호(평문 - 기존 SmartThings 자격증명과 동일 수준)' AFTER `wifi_ssid`;

-- 적용 후 확인
-- SHOW CREATE TABLE place\G
