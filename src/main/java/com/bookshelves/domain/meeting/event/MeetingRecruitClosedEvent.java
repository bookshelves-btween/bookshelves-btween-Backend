package com.bookshelves.domain.meeting.event;

// 정원 충족 또는 모집 기한 도달로 모임 성립이 확정된 이벤트.
// AI 질문 준비는 이 이벤트가 커밋된 후 시작한다.
public record MeetingRecruitClosedEvent(Long meetingId) {}
