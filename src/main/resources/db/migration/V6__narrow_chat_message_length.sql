-- 채팅 메시지 길이 제한을 DB에도 건다.
--
-- 지금까지 500자 제한은 ChatMessageRequest의 @Size(max = 500) 한 곳에만 있었고 컬럼은 TEXT라
-- 65,535바이트를 받았다. 검증을 우회하는 경로가 하나라도 생기면 DB가 그대로 받아준다.
-- 채팅 원문은 모임 종료 시 요약 LLM 입력으로 통째로 들어가므로, 길이 계약이 애플리케이션 한
-- 층에만 있으면 비용이 사용자 입력에 그대로 연동된다.
--
-- utf8mb4 기준 VARCHAR(500)은 2,000바이트라 InnoDB 행 크기 제한에 여유가 있다.
--
-- 기존 데이터 주의: @Size는 bd6c036에서 들어왔다. 그 이전에 실제 환경에서 저장된 행이 있다면
-- 500자를 넘을 수 있다. 적용 전에 아래로 확인한다.
--   SELECT MAX(CHAR_LENGTH(message)) FROM chat_message;

ALTER TABLE chat_message
    MODIFY COLUMN message VARCHAR(500) NOT NULL;
