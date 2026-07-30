-- ================================================
-- feedback 테이블에 place_id 컬럼 추가
--
-- 배경: QR 피드백 제출 시 deviceId 없이도(장소에 기기가 없거나 사용자가 지목하지
-- 않음) 저장할 수 있도록 바꿨는데(#259), 기존에는 장소 소속을 device.place로만
-- 알아냈다. device가 없는 피드백은 findAllByDevice_PlaceId 같은 프로퍼티 경로
-- 조회(암묵적 INNER JOIN)에서 통째로 누락돼 관리자 목록/개수/승인·거절에서 보이지도
-- 처리되지도 않는 버그가 있었음(CodeRabbit 리뷰로 발견) - place_id를 별도 FK로 둬서 해결.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 011_add_feedback_place_id.sql
-- ================================================

ALTER TABLE `feedback`
    ADD COLUMN `place_id` BIGINT NULL COMMENT '장소 FK - device가 없어도 항상 채워짐(목록/개수 조회 기준)' AFTER `device_id`;

-- 기기 삭제(ON DELETE SET NULL)로 device_id가 이미 NULL인 행 중, 그 기기가 속했던 장소마저
-- 나중에 통째로 삭제된(place_id 자체를 복원할 방법이 없는) 완전 고아 피드백은 정리한다.
-- place_id NOT NULL 제약을 걸 수 있게 하기 위한 불가피한 조치 - 실행 전 몇 건인지 먼저 확인할 것.
-- SELECT id, content, status, created_at FROM feedback WHERE device_id IS NULL;
DELETE FROM `feedback` WHERE `device_id` IS NULL;

-- 이 마이그레이션 시점까지는 deviceId가 필수였으므로(#259 이전), 위에서 정리한 고아 행을
-- 제외한 나머지는 전부 device_id가 있다 - 그 device가 속한 place로 백필한다.
UPDATE `feedback` f
    JOIN `device` d ON f.device_id = d.id
SET f.place_id = d.place_id
WHERE f.place_id IS NULL;

-- 적용 전 확인 - 위 DELETE/UPDATE 이후에도 결과가 있으면 안 됨(있으면 원인 파악 후 진행)
-- SELECT id, device_id FROM feedback WHERE place_id IS NULL;

ALTER TABLE `feedback`
    MODIFY COLUMN `place_id` BIGINT NOT NULL COMMENT '장소 FK - device가 없어도 항상 채워짐(목록/개수 조회 기준)',
    ADD KEY `idx_feedback_place_status` (`place_id`, `status`),
    ADD CONSTRAINT `fk_feedback_place` FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE;

-- 적용 후 확인
-- SHOW CREATE TABLE feedback\G
