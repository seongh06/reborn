-- ================================================
-- device 테이블에 IR 에어컨 제어(#288) 관련 컬럼 추가
--
-- has_ir_control: 이 ARDUINO 기기에 IR 송신 모듈이 실제로 물려있는지 - 관리자가 등록 시 설정하는
-- 신뢰 기반 플래그(서버가 물리적으로 확인할 방법 없음). SmartThings/AEROMETER/AI_SPEAKER는 항상 false.
-- pending_ir_command: 관리자가 보낸 IR 명령 중 아두이노가 아직 폴링해가지 않은 것. 폴링 시 1회성으로
-- 소비되고 즉시 NULL로 비워진다(큐 길이 1 - MVP 스코프).
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 012_add_device_ir_control.sql
-- ================================================

ALTER TABLE `device`
    ADD COLUMN `has_ir_control` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'IR 송신 모듈 보유 여부 (ARDUINO 전용)' AFTER `is_online`,
    ADD COLUMN `pending_ir_command` VARCHAR(20) NULL COMMENT '대기 중인 IR 명령 (폴링 시 1회성 소비)' AFTER `has_ir_control`;

-- 적용 후 확인
-- SHOW CREATE TABLE device\G
