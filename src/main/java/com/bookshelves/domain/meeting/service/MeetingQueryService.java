package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.ai.repository.MeetingSummaryRepository;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.converter.MeetingConverter;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingSearchResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

  public MeetingSearchResDTO searchMeetings(String name, int page, int size) {
    PageRequest pageRequest =
      PageRequest.of(page - 1, size, Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id")));
    Page<Meeting> meetingPage =
      meetingRepository.findByBookTitleContainingIgnoreCase(name.trim(), pageRequest); // 이름 공백 제거

    List<Long> meetingIds = meetingPage.getContent().stream().map(Meeting::getId).toList();
    Map<Long, Long> chatroomIds =
      meetingIds.isEmpty()
        ? Map.of()
        : chatRoomRepository.findAllByMeetingIdIn(meetingIds).stream()
        .collect(
          Collectors.toMap(
            chatRoom -> chatRoom.getMeeting().getId(), ChatRoom::getId));

    return MeetingConverter.toMeetingSearchResDTO(meetingPage, chatroomIds);
  }
}
