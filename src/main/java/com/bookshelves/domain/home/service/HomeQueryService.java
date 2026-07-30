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

  // 홈에 모임 카드를 세 장까지 그린다.
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
    List<Meeting> meetings =
        meetingRepository.findJoinableMeetings(
            MeetingStatus.RECRUITING,
            ServiceTime.now(),
            memberId,
            PageRequest.of(0, JOINABLE_MEETING_LIMIT));

    return HomeConverter.toHomeResDTO(
        member, findRecommendation(ServiceTime.today()), recentBook, meetings);
  }

  // 오늘치가 없으면 가장 최근 것으로 내려간다.
  //
  // 23시 스케줄러가 서버 정지나 LLM 장애로 건너뛰면 오늘 행이 빈다. 그때 추천 영역을 통째로 지우는
  // 것보다 어제 책을 하루 더 두는 편이 낫다. 노출 날짜를 함께 내려보내므로 클라이언트가 언제 것인지
  // 구분할 수 있다. 책을 아직 한 권도 안 넣었을 때만 null이 된다.
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
