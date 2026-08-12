package com.bookshelves.domain.home.service;

import com.bookshelves.domain.ai.entity.AIRecommendation;
import com.bookshelves.domain.ai.repository.AIRecommendationRepository;
import com.bookshelves.domain.book.entity.MemberBook;
import com.bookshelves.domain.book.repository.MemberBookRepository;
import com.bookshelves.domain.home.converter.HomeConverter;
import com.bookshelves.domain.home.dto.response.HomeResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.exception.MemberException;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import com.bookshelves.global.util.ServiceTime;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HomeQueryService {

  private static final int JOINABLE_MEETING_LIMIT = 3;

  private final MemberRepository memberRepository;
  private final AIRecommendationRepository aiRecommendationRepository;
  private final MemberBookRepository memberBookRepository;
  private final MeetingRepository meetingRepository;
  private final AuthenticationFacade authenticationFacade;

  public HomeResDTO getHome() {
    Long memberId = authenticationFacade.getCurrentMemberId();
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

    MemberBook recentBook =
        memberBookRepository.findFirstByMemberIdOrderByUpdatedAtDescIdDesc(memberId).orElse(null);
    // 배치 전 상태가 갱신되지 않았어도 모집 기한이 지난 모임은 제외한다.
    List<Meeting> meetings =
        meetingRepository.findJoinableMeetings(
            MeetingStatus.RECRUITING,
            ServiceTime.now().plusHours(Meeting.RECRUITMENT_CLOSE_HOURS_BEFORE_START),
            memberId,
            PageRequest.of(0, JOINABLE_MEETING_LIMIT));

    return HomeConverter.toHomeResDTO(
        member, findRecommendation(ServiceTime.today()), recentBook, meetings);
  }

  // 오늘 추천이 없으면 가장 최근 추천으로 대체한다.
  private AIRecommendation findRecommendation(LocalDate today) {
    return aiRecommendationRepository
        .findByRecommendedDate(today)
        .or(
            () ->
                aiRecommendationRepository
                    .findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(today))
        .orElse(null);
  }
}
