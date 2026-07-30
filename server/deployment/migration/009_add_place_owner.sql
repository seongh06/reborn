-- ================================================
-- user_place_mapping 테이블에 방장(is_owner) 컬럼 추가
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 컬럼 변경도 수동으로 적용해야 한다.
--
-- 기존에 등록된 장소는 방장이 아무도 없는 상태이므로, 장소별로 가장 먼저 등록된
-- ADMIN(= id가 가장 작은 ADMIN 매핑, 대부분 장소 등록자 본인)을 방장으로 백필한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 009_add_place_owner.sql
-- ================================================

ALTER TABLE `user_place_mapping`
    ADD COLUMN `is_owner` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '방장 여부 - 장소당 1명, 하드 삭제/방장 위임 권한' AFTER `access_level`;

UPDATE `user_place_mapping` upm
    JOIN (
        SELECT place_id, MIN(id) AS first_admin_mapping_id
        FROM `user_place_mapping`
        WHERE access_level = 'ADMIN'
        GROUP BY place_id
    ) first_admin ON upm.id = first_admin.first_admin_mapping_id
SET upm.is_owner = 1;

-- 적용 후 확인
-- SHOW CREATE TABLE user_place_mapping\G
-- SELECT place_id, COUNT(*) FROM user_place_mapping WHERE is_owner = 1 GROUP BY place_id HAVING COUNT(*) <> 1;
