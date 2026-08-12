package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {

  // 조회 결과는 서비스에서 SummaryAxis 표시 순서로 정렬한다.
  List<MeetingSummary> findAllByMeetingId(Long meetingId);
}
