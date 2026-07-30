package com.bookshelves.domain.meeting.event;

// 모집이 마감되어 모임 성립이 확정된 순간(RECRUITING → RECRUIT_CLOSED).
//
// 명세상 전환 경로는 둘이다 — 정원 충족, 그리고 `starts_at - 6h` 모집 마감.
// 두 경로 모두 "이 모임은 폭파되지 않는다"가 확정되는 시점이라 같은 이벤트로 합류시킨다.
// (현재 코드에는 정원 충족 경로만 있고, 6h 마감 스케줄러가 붙으면 거기서도 이 이벤트를 발행하면 된다.)
//
// AI 질문 준비가 이 시점에 걸리는 이유: 각색 입력이 Book뿐이라 참여자·시작 시각과 무관하고,
// 모임 시작까지 시간 여유가 충분해 LLM 지연이 사용자에게 노출되지 않는다.
public record MeetingRecruitClosedEvent(Long meetingId) {}
