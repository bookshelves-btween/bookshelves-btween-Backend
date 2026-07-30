package com.bookshelves.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.entity.AIRecommendation;
import com.bookshelves.domain.ai.repository.AIRecommendationRepository;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.entity.MemberBook;
import com.bookshelves.domain.book.repository.MemberBookRepository;
import com.bookshelves.domain.home.dto.response.HomeResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.exception.MemberException;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import com.bookshelves.global.util.ServiceTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeQueryServiceTest {

  private static final Long MEMBER_ID = 1L;

  @Mock private MemberRepository memberRepository;
  @Mock private AIRecommendationRepository aiRecommendationRepository;
  @Mock private MemberBookRepository memberBookRepository;
  @Mock private MeetingRepository meetingRepository;
  @Mock private AuthenticationFacade authenticationFacade;

  @InjectMocks private HomeQueryService homeQueryService;

  @BeforeEach
  void setUp() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(MEMBER_ID);
  }

  private Member member() {
    Member member = Member.createSocialMember(Provider.KAKAO, "provider-1");
    ReflectionTestUtils.setField(member, "id", MEMBER_ID);
    ReflectionTestUtils.setField(member, "nickname", "책 먹는 여우");
    return member;
  }

  private Book book(Long id, String title) {
    Book book =
        Book.builder()
            .isbn("isbn-" + id)
            .title(title)
            .author("손원평")
            .publisher("창비")
            .kdcName("한국소설")
            .coverImageUrl("https://example.com/" + id + ".jpg")
            .build();
    ReflectionTestUtils.setField(book, "id", id);
    return book;
  }

  private AIRecommendation recommendation(Book book, LocalDate date) {
    return AIRecommendation.builder()
        .book(book)
        .recommendationMessage("감정을 배우는 소년의 조용한 성장 기록")
        .recommendedDate(date)
        .build();
  }

  private Meeting meeting(Long id, Book book, LocalDateTime startDate) {
    Meeting meeting =
        Meeting.builder().book(book).startDate(startDate).duration(60).maxParticipants(6).build();
    ReflectionTestUtils.setField(meeting, "id", id);
    ReflectionTestUtils.setField(meeting, "curParticipants", 4);
    return meeting;
  }

  private void givenMemberExists() {
    given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
  }

  private void givenNoRecentBookAndNoMeeting() {
    given(memberBookRepository.findFirstByMemberIdOrderByUpdatedAtDescIdDesc(MEMBER_ID))
        .willReturn(Optional.empty());
    given(meetingRepository.findJoinableMeetings(any(), any(), eq(MEMBER_ID), any()))
        .willReturn(List.of());
  }

  @Test
  void returnsTodaysRecommendationWithBookDetails() {
    givenMemberExists();
    givenNoRecentBookAndNoMeeting();
    LocalDate today = ServiceTime.today();
    given(aiRecommendationRepository.findByRecommendedDate(today))
        .willReturn(Optional.of(recommendation(book(10L, "아몬드"), today)));

    HomeResDTO result = homeQueryService.getHome();

    assertThat(result.member().nickname()).isEqualTo("책 먹는 여우");
    assertThat(result.recommendedAt()).isEqualTo(today);
    assertThat(result.recommendedBook().bookId()).isEqualTo(10L);
    assertThat(result.recommendedBook().title()).isEqualTo("아몬드");
    assertThat(result.recommendedBook().author()).isEqualTo("손원평");
    assertThat(result.recommendedBook().coverImageUrl()).isEqualTo("https://example.com/10.jpg");
    assertThat(result.recommendedBook().recommendationMessage()).isEqualTo("감정을 배우는 소년의 조용한 성장 기록");
  }

  @Test
  void fallsBackToLatestRecommendationWhenSchedulerSkippedToday() {
    // 23시에 서버가 내려가 있었으면 오늘 행이 없다. 추천 영역을 지우는 것보다 어제 책을 하루 더 두는 편이 낫다.
    givenMemberExists();
    givenNoRecentBookAndNoMeeting();
    LocalDate today = ServiceTime.today();
    LocalDate yesterday = today.minusDays(1);
    given(aiRecommendationRepository.findByRecommendedDate(today)).willReturn(Optional.empty());
    given(
            aiRecommendationRepository
                .findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(today))
        .willReturn(Optional.of(recommendation(book(10L, "아몬드"), yesterday)));

    HomeResDTO result = homeQueryService.getHome();

    // 언제 것인지 클라이언트가 구분할 수 있어야 한다
    assertThat(result.recommendedAt()).isEqualTo(yesterday);
    assertThat(result.recommendedBook().bookId()).isEqualTo(10L);
  }

  @Test
  void returnsNullRecommendationWhenNoBookHasBeenSeeded() {
    givenMemberExists();
    givenNoRecentBookAndNoMeeting();
    given(aiRecommendationRepository.findByRecommendedDate(any())).willReturn(Optional.empty());
    given(
            aiRecommendationRepository
                .findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(any()))
        .willReturn(Optional.empty());

    HomeResDTO result = homeQueryService.getHome();

    assertThat(result.recommendedBook()).isNull();
    assertThat(result.recommendedAt()).isNull();
  }

  @Test
  void returnsRecentBookWithMyRatingAndProgress() {
    givenMemberExists();
    given(aiRecommendationRepository.findByRecommendedDate(any())).willReturn(Optional.empty());
    given(
            aiRecommendationRepository
                .findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(any()))
        .willReturn(Optional.empty());
    given(meetingRepository.findJoinableMeetings(any(), any(), eq(MEMBER_ID), any()))
        .willReturn(List.of());

    MemberBook memberBook =
        MemberBook.create(book(20L, "혼모노"), member(), 70, new BigDecimal("4.5"), null);
    given(memberBookRepository.findFirstByMemberIdOrderByUpdatedAtDescIdDesc(MEMBER_ID))
        .willReturn(Optional.of(memberBook));

    HomeResDTO result = homeQueryService.getHome();

    assertThat(result.recentBook().bookId()).isEqualTo(20L);
    assertThat(result.recentBook().title()).isEqualTo("혼모노");
    assertThat(result.recentBook().rating()).isEqualByComparingTo("4.5");
    assertThat(result.recentBook().progress()).isEqualTo(70);
    assertThat(result.recentBook().coverImageUrl()).isEqualTo("https://example.com/20.jpg");
  }

  @Test
  void returnsJoinableMeetingsWithParticipantCounts() {
    givenMemberExists();
    given(aiRecommendationRepository.findByRecommendedDate(any())).willReturn(Optional.empty());
    given(
            aiRecommendationRepository
                .findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(any()))
        .willReturn(Optional.empty());
    given(memberBookRepository.findFirstByMemberIdOrderByUpdatedAtDescIdDesc(MEMBER_ID))
        .willReturn(Optional.empty());

    LocalDateTime startDate = LocalDateTime.of(2026, 8, 2, 19, 0);
    given(meetingRepository.findJoinableMeetings(any(), any(), eq(MEMBER_ID), any()))
        .willReturn(List.of(meeting(21L, book(20L, "혼모노"), startDate)));

    HomeResDTO result = homeQueryService.getHome();

    assertThat(result.meetings()).hasSize(1);
    HomeResDTO.MeetingInfo info = result.meetings().get(0);
    assertThat(info.meetingId()).isEqualTo(21L);
    // 모임에는 이름 필드가 없어 책 제목을 카드 제목으로 쓴다
    assertThat(info.title()).isEqualTo("혼모노");
    assertThat(info.startDate()).isEqualTo(startDate);
    assertThat(info.currentParticipants()).isEqualTo(4);
    assertThat(info.maxParticipants()).isEqualTo(6);
  }

  @Test
  void asksOnlyForRecruitingMeetingsStartingInTheFuture() {
    givenMemberExists();
    givenNoRecentBookAndNoMeeting();
    given(aiRecommendationRepository.findByRecommendedDate(any())).willReturn(Optional.empty());
    given(
            aiRecommendationRepository
                .findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(any()))
        .willReturn(Optional.empty());

    homeQueryService.getHome();

    ArgumentCaptor<MeetingStatus> status = ArgumentCaptor.captor();
    ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.captor();
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
    verify(meetingRepository)
        .findJoinableMeetings(status.capture(), now.capture(), eq(MEMBER_ID), pageable.capture());

    assertThat(status.getValue()).isEqualTo(MeetingStatus.RECRUITING);
    assertThat(now.getValue()).isNotNull();
    // 홈은 카드 세 장까지 그린다
    assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
  }

  @Test
  void throwsWhenMemberIsMissing() {
    given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> homeQueryService.getHome()).isInstanceOf(MemberException.class);
  }
}
