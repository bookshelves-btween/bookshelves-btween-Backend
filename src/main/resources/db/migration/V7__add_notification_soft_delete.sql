-- 삭제된 알림 행을 중복 전송 방지 표식으로 유지하면서 사용자 조회에서는 제외한다.
ALTER TABLE notification
    ADD COLUMN is_deleted bit(1) NOT NULL DEFAULT b'0' AFTER is_read;
