-- ================================================
-- 운영 DB 스키마 drift 수정 (#118)
--
-- 배경: device/user_place_mapping 테이블이 schema.sql이 아니라 과거 Hibernate
-- ddl-auto 자동 생성으로 만들어져 있었음(FK 이름이 FKxxxxx 식의 Hibernate
-- 자동 생성 이름인 것으로 확인). schema.sql은 CREATE TABLE IF NOT EXISTS라
-- 이미 존재하는 테이블에는 이후 변경사항(#85 KIOSK→AEROMETER 리네이밍,
-- FK CASCADE 추가 등)이 전혀 반영되지 않았음.
--
-- 적용 전 실제 운영 스키마와 FK 제약 이름이 이 스크립트의 DROP 구문과
-- 일치하는지 반드시 `SHOW CREATE TABLE device;` / `SHOW CREATE TABLE
-- user_place_mapping;`으로 먼저 확인할 것 (2026-07-10 SSH 확인 기준으로
-- 작성됨 — 환경마다 자동 생성된 FK 이름이 다를 수 있음).
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 001_fix_device_place_schema_drift.sql
-- ================================================

-- 1. device.device_type: enum('ARDUINO','KIOSK') → VARCHAR(20)
--    'AEROMETER' 값을 받을 수 없어 페어링(#09) 시 "Data truncated for column" 500 발생
ALTER TABLE `device`
    MODIFY COLUMN `device_type` VARCHAR(20) NOT NULL COMMENT '기기 유형 (ARDUINO / AEROMETER)';

-- 2. device.place_id FK: ON DELETE CASCADE 추가
ALTER TABLE `device`
    DROP FOREIGN KEY `FKt46luvhssp2815k6rucvbt6mx`;
ALTER TABLE `device`
    ADD CONSTRAINT `fk_device_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE;

-- 3. user_place_mapping.place_id FK: ON DELETE CASCADE 추가
ALTER TABLE `user_place_mapping`
    DROP FOREIGN KEY `FKs3gf3mwsyh9ojktqlb6wxufit`;
ALTER TABLE `user_place_mapping`
    ADD CONSTRAINT `fk_upm_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE;

-- 4. user_place_mapping.user_id FK: ON DELETE CASCADE 추가
ALTER TABLE `user_place_mapping`
    DROP FOREIGN KEY `FKjb48nl4wu105we3aq5fkckfvs`;
ALTER TABLE `user_place_mapping`
    ADD CONSTRAINT `fk_upm_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

-- 5. metric_logs.device_id FK: ON DELETE SET NULL 추가
--    (기기 삭제 시 로그 데이터는 보존하되 device_id만 NULL 처리 — schema.sql 의도와 일치)
ALTER TABLE `metric_logs`
    DROP FOREIGN KEY `FKq718g976k3c7pv708e3yj719m`;
ALTER TABLE `metric_logs`
    ADD CONSTRAINT `fk_metric_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL;

-- 6. feedback.device_id FK: ON DELETE SET NULL 추가
--    (기기 삭제 시 피드백 데이터는 보존하되 device_id만 NULL 처리 — schema.sql 의도와 일치)
ALTER TABLE `feedback`
    DROP FOREIGN KEY `FK9vufeuteaay6v5yu6pjltkdmo`;
ALTER TABLE `feedback`
    ADD CONSTRAINT `fk_feedback_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL;

-- 적용 후 확인
-- SHOW CREATE TABLE device\G
-- SHOW CREATE TABLE user_place_mapping\G
-- SHOW CREATE TABLE metric_logs\G
-- SHOW CREATE TABLE feedback\G
