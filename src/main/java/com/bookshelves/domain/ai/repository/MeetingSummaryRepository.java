package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {

  // 정렬은 DB가 아니라 조회 후 SummaryAxis의 표시 순서로 한다.
  // 축 순서는 화면 표현이지 저장 대상이 아니다.
  List<MeetingSummary> findAllByMeetingId(Long meetingId);
}
