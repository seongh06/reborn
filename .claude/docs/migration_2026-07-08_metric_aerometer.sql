-- ================================================
-- ReBorn 운영 DB 마이그레이션
-- 이슈 #85: data/sensor 도메인 metric 통합 및 기기 등록 구조 정리
--
-- ⚠️ 실행 전 반드시 백업할 것. 아래 스크립트는 Claude가 직접 실행하지 않으며,
--    운영자가 점검 창(다운타임 또는 낮은 트래픽 시간대)에 수동으로 실행해야 합니다.
-- ================================================

-- 1. sensor_logs 테이블 → metric_logs로 이름 변경
--    (테이블 내용/데이터는 그대로 유지, 이름과 인덱스/제약조건명만 변경)
RENAME TABLE `sensor_logs` TO `metric_logs`;

ALTER TABLE `metric_logs`
    RENAME INDEX `idx_sensor_device_created` TO `idx_metric_device_created`;

ALTER TABLE `metric_logs`
    DROP FOREIGN KEY `fk_sensor_device`,
    ADD CONSTRAINT `fk_metric_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL;

-- 2. device.device_type의 KIOSK 값 → AEROMETER로 갱신
UPDATE `device` SET `device_type` = 'AEROMETER' WHERE `device_type` = 'KIOSK';

-- 3. 테이블 코멘트 갱신 (선택 사항, 운영에 영향 없음)
ALTER TABLE `metric_logs` COMMENT = '메트릭 수집 로그';
ALTER TABLE `device` COMMENT = '기기 (Arduino / Aerometer)';
