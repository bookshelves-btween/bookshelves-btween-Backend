-- anonymizeMember()는 WITHDRAWN 상태 회원만 처리해 device_token을 정리한다. 이미 ANONYMIZED
-- 상태로 넘어간 회원은 이 메서드의 대상이 다시 되지 않아, 이번 수정 이전에 이미 익명화됐던
-- 회원의 device_token은 앞으로도 자동으로 정리되지 않는다. 기존 ANONYMIZED 회원의
-- device_token을 여기서 일회성으로 정리한다. 대상이 이미 지워진 뒤 다시 실행돼도 삭제할
-- row가 없을 뿐이라 재실행에 안전하다.
DELETE dt FROM device_token dt
    JOIN member m ON m.id = dt.member_id
WHERE m.status = 'ANONYMIZED';
