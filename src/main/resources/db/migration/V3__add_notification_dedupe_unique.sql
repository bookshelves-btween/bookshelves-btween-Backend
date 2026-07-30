-- 같은 회원에게 같은 사건의 알림이 두 번 쌓이는 것을 DB에서 막는다.
--
-- 생성 전에 존재를 확인하는 방식만으로는 부족하다. 준비 작업 두 개가 동시에 돌면 둘 다 아직 없다고
-- 읽고 각각 INSERT한다. 확인과 삽입 사이를 직렬화할 수단이 없어 제약으로 막는다.
--
-- related_id가 NULL인 알림(모임 취소)은 MySQL이 NULL을 unique 검사에서 제외하므로 영향을 받지 않는다.
-- 취소 알림은 모임이 즉시 삭제되어 이동 대상 ID를 남기지 않는 설계다.

ALTER TABLE notification
    ADD CONSTRAINT uk_notification_member_type_related UNIQUE (member_id, type, related_id);
