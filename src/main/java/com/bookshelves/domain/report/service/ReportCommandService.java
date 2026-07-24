package com.bookshelves.domain.report.service;

import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.report.code.ReportErrorCode;
import com.bookshelves.domain.report.converter.ReportConverter;
import com.bookshelves.domain.report.dto.ReportCreateResponse;
import com.bookshelves.domain.report.entity.Report;
import com.bookshelves.domain.report.exception.ReportException;
import com.bookshelves.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportCommandService {

  private final ReportRepository reportRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final MemberRepository memberRepository;

  // 신고자는 토큰으로 식별된 memberId. 존재하는 방인지 → 참여자인지 → 중복 신고가 아닌지 순으로 검증한다.
  public ReportCreateResponse createReport(Long chatroomId, Long reporterId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ReportException(ReportErrorCode.CHATROOM_NOT_FOUND));

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(
        chatRoom.getMeeting().getId(), reporterId)) {
      throw new ReportException(ReportErrorCode.NOT_PARTICIPANT);
    }
    // 흔한 중복은 여기서 걸러 친절한 409를 준다. 최종 무결성은 Report의
    // (reporter_member_id, chatroom_id) 유니크 제약이 보장하며, existsBy와 save 사이의
    // 극히 드문 동시 요청 레이스는 DB 제약이 막는다(그 경우 두 번째 요청은 500). 데이터는 항상 안전하다.
    if (reportRepository.existsByReporterMemberIdAndChatRoomId(reporterId, chatroomId)) {
      throw new ReportException(ReportErrorCode.ALREADY_REPORTED);
    }

    Member reporter = memberRepository.getReferenceById(reporterId);
    Report report = reportRepository.save(ReportConverter.toReport(chatRoom, reporter));

    return ReportConverter.toReportCreateResponse(report);
  }
}
