-- 약관을 개정해 새 버전 row를 추가해도 조회/필수 동의 검증이 타입별 최신(활성) 버전만
-- 대상으로 하도록 활성 여부 컬럼을 추가한다. 기존 row는 전부 현재 활성 버전이므로 기본값 true.
--
-- 이후 실제로 약관을 개정할 때는(V5와 마찬가지로 세션매니저를 통한 수동 운영 SQL), 신버전
-- INSERT와 구버전 비활성화 UPDATE를 반드시 하나의 트랜잭션으로 묶어서 실행한다. 둘을 별도
-- 문장으로 나눠서 실행하면 그 사이 시점에 같은 type의 활성 row가 2개 이상 남을 수 있고,
-- findByIsActiveTrue() / findByIsActiveTrueAndIsRequiredTrue()는 type으로 필터링하지 않으므로
-- 이번에 고친 버그(여러 버전이 동시에 조회·검증 대상이 되는 문제)가 그대로 재현된다.
--
--   START TRANSACTION;
--   INSERT INTO terms (title, content, type, version, is_required, is_active, created_at, updated_at)
--     VALUES (..., '...', '<TYPE>', '<신버전>', 1, 1, NOW(6), NOW(6));
--   UPDATE terms SET is_active = 0 WHERE type = '<TYPE>' AND is_active = 1 AND version <> '<신버전>';
--   COMMIT;
--
-- 개정 작업 전후로 아래 쿼리 결과가 항상 모든 type에 대해 1이어야 한다(정기 점검용으로도 활용):
--
--   SELECT type, COUNT(*) FROM terms WHERE is_active = b'1' GROUP BY type HAVING COUNT(*) <> 1;
ALTER TABLE terms
    ADD COLUMN is_active bit(1) NOT NULL DEFAULT b'1' AFTER is_required;
