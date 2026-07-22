package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.ai.repository.MeetingSummaryRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.converter.MeetingConverter;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingQueryService {

  private final MeetingRepository meetingRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final MeetingSummaryRepository meetingSummaryRepository;

  public MeetingDetailResDTO getMeetingDetail(Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findWithBookById(meetingId)
            .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));
    Long chatroomId =
        chatRoomRepository
            .findByMeetingId(meetingId)
            .map(chatRoom -> chatRoom.getId())
            .orElse(null);
    List<MeetingSummary> summaries =
        meetingSummaryRepository.findAllByMeetingIdOrderByQuestionOrder(meetingId);

    return MeetingConverter.toMeetingDetailResDTO(meeting, chatroomId, summaries);
  }
}
