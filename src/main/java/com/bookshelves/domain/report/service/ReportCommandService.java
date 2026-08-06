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

  // 신고자는 토큰으로 식별된 memberId.
  // 존재하는 방인지 → 참여자인지 → 모임이 시작했는지 → 중복 신고가 아닌지 순으로 검증한다.
  public ReportCreateResponse createReport(Long chatroomId, Long reporterId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ReportException(ReportErrorCode.CHATROOM_NOT_FOUND));
    Long meetingId = chatRoom.getMeeting().getId();

    // 인원 미달 모임 삭제도 같은 meeting 행을 잠근다. 신고 저장과 삭제를 직렬화해
    // report 벌크 삭제 직후 새 신고가 삽입되는 FK 경쟁 조건을 막는다.
    Meeting meeting =
        meetingRepository
            .findByIdForUpdate(meetingId)
            .orElseThrow(() -> new ReportException(ReportErrorCode.CHATROOM_NOT_FOUND));

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(meetingId, reporterId)) {
      throw new ReportException(ReportErrorCode.NOT_PARTICIPANT);
    }
    // 참여자 확인보다 뒤에 둔다 — 비참여자에게 그 모임의 진행 상태를 알려주지 않는다.
    // 시작 전에는 신고할 대화 자체가 없고, 모집 마감 전 모임은 조기 폭파로 사라질 수 있다.
    if (!meeting.hasStarted()) {
      throw new ReportException(ReportErrorCode.MEETING_NOT_STARTED);
    }
    // 흔한 중복은 여기서 걸러 친절한 409를 준다. 최종 무결성은 Report의
    // (reporter_member_id, chatroom_id) 유니크 제약이 보장한다.
    if (reportRepository.existsByReporterMemberIdAndChatRoomId(reporterId, chatroomId)) {
      throw new ReportException(ReportErrorCode.ALREADY_REPORTED);
    }

    Member reporter = memberRepository.getReferenceById(reporterId);

    // existsBy는 트랜잭션 시작 시점의 스냅샷을 읽으므로(MySQL 기본 REPEATABLE READ) 락을 얻기 전에
    // 커밋된 신고를 놓칠 수 있다. 커밋까지 미루면 예외가 이 메서드 밖에서 터지니 즉시 플러시해
    // 제약 위반을 여기서 같은 409로 바꾼다.
    Report report;
    try {
      report = reportRepository.saveAndFlush(ReportConverter.toReport(chatRoom, reporter));
    } catch (DataIntegrityViolationException e) {
      throw new ReportException(ReportErrorCode.ALREADY_REPORTED);
    }

    return ReportConverter.toReportCreateResponse(report);
  }
}
