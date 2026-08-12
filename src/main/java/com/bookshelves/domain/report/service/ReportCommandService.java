package com.bookshelves.domain.report.service;

import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.report.code.ReportErrorCode;
import com.bookshelves.domain.report.converter.ReportConverter;
import com.bookshelves.domain.report.dto.ReportCreateResponse;
import com.bookshelves.domain.report.entity.Report;
import com.bookshelves.domain.report.exception.ReportException;
import com.bookshelves.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportCommandService {

  private final ReportRepository reportRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final MemberRepository memberRepository;

  // 신고자는 인증된 회원 ID로 식별한다.
  public ReportCreateResponse createReport(Long chatroomId, Long reporterId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ReportException(ReportErrorCode.CHATROOM_NOT_FOUND));
    Long meetingId = chatRoom.getMeeting().getId();

    // 모임 삭제와 신고 저장을 직렬화해 FK 경합을 막는다.
    Meeting meeting =
        meetingRepository
            .findByIdForUpdate(meetingId)
            .orElseThrow(() -> new ReportException(ReportErrorCode.CHATROOM_NOT_FOUND));

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(meetingId, reporterId)) {
      throw new ReportException(ReportErrorCode.NOT_PARTICIPANT);
    }
    // 비참여자에게 모임 진행 상태를 노출하지 않도록 참여 여부를 먼저 확인한다.
    if (!meeting.hasStarted()) {
      throw new ReportException(ReportErrorCode.MEETING_NOT_STARTED);
    }
    // 선조회로 중복 응답을 처리하고 최종 무결성은 unique 제약에 맡긴다.
    if (reportRepository.existsByReporterMemberIdAndChatRoomId(reporterId, chatroomId)) {
      throw new ReportException(ReportErrorCode.ALREADY_REPORTED);
    }

    Member reporter = memberRepository.getReferenceById(reporterId);

    // 즉시 flush해 동시 중복 신고의 제약 위반도 같은 409 응답으로 변환한다.
    Report report;
    try {
      report = reportRepository.saveAndFlush(ReportConverter.toReport(chatRoom, reporter));
    } catch (DataIntegrityViolationException e) {
      throw new ReportException(ReportErrorCode.ALREADY_REPORTED);
    }

    return ReportConverter.toReportCreateResponse(report);
  }
}
