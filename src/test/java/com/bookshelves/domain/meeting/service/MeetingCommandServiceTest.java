package com.bookshelves.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.ai.service.AIQuestionPreparationService;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.service.BookCommandService;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingParticipationResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.event.MeetingRecruitClosedEvent;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.domain.report.repository.ReportRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class MeetingCommandServiceTest {

  @Mock private BookCommandService bookCommandService;
  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingParticipantRepository meetingParticipantRepository;
  @Mock private ChatRoomRepository chatRoomRepository;
  @Mock private NotificationCommandService notificationCommandService;
  @Mock private MemberRepository memberRepository;
  @Mock private AuthenticationFacade authenticationFacade;
  @Mock private AIQuestionRepository aiQuestionRepository;
  @Mock private ReportRepository reportRepository;
  @Mock private AIQuestionPreparationService aiQuestionPreparationService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private MeetingCommandService meetingCommandService;

  @Test
  void createsMeetingWithBookResolvedByIsbn() {
    String isbn = "9788936434595";
    Book book = Book.builder().isbn(isbn).title("아몬드").author("손원평").publisher("창비").build();
    Meeting savedMeeting = mock(Meeting.class);
    Member leader = mock(Member.class);
    MeetingCreateReqDTO request =
        new MeetingCreateReqDTO(isbn, LocalDate.of(2026, 8, 1), "20:00", 4, 60);
    given(bookCommandService.getOrCreateByIsbn(isbn)).willReturn(book);
    given(meetingRepository.save(any(Meeting.class))).willReturn(savedMeeting);
    given(savedMeeting.getId()).willReturn(1L);
    given(authenticationFacade.getCurrentMemberId()).willReturn(10L);
    given(memberRepository.getReferenceById(10L)).willReturn(leader);

    MeetingCreateResDTO response = meetingCommandService.createMeeting(request);

    ArgumentCaptor<MeetingParticipant> participantCaptor =
        ArgumentCaptor.forClass(MeetingParticipant.class);
    ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
    assertThat(response.id()).isEqualTo(1L);
    verify(bookCommandService).getOrCreateByIsbn(isbn);
    verify(meetingRepository).save(any(Meeting.class));
    verify(meetingParticipantRepository).save(participantCaptor.capture());
    verify(chatRoomRepository).save(chatRoomCaptor.capture());
    assertThat(chatRoomCaptor.getValue().getMeeting()).isSameAs(savedMeeting);
    assertThat(participantCaptor.getValue().getMeeting()).isSameAs(savedMeeting);
    assertThat(participantCaptor.getValue().getMember()).isSameAs(leader);
    assertThat(participantCaptor.getValue().getIsLeader()).isTrue();
    verify(savedMeeting).addParticipant();
  }

  @Test
  void startsClosedMeetingAfterStartDate() {
    Meeting meeting = mock(Meeting.class);
    Book book = mock(Book.class);
    Member member = mock(Member.class);
    MeetingParticipant participant = mock(MeetingParticipant.class);
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 20, 0);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUIT_CLOSED);
    given(meeting.canStart()).willReturn(true);
    given(meeting.getStartDate()).willReturn(now);
    given(meeting.getId()).willReturn(1L);
    given(meeting.getBook()).willReturn(book);
    given(book.getTitle()).willReturn("아몬드");
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(1L))
        .willReturn(List.of(participant));
    given(participant.getMember()).willReturn(member);

    boolean started = meetingCommandService.startMeeting(1L, now);

    assertThat(started).isTrue();
    verify(meeting).start();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
    verify(notificationCommandService).saveAll(notificationCaptor.capture());
    assertThat(notificationCaptor.getValue()).hasSize(1);
    Notification notification = notificationCaptor.getValue().get(0);
    assertThat(notification.getMember()).isSameAs(member);
    assertThat(notification.getTitle()).isEqualTo("아몬드 독서 모임이 시작되었어요");
    assertThat(notification.getContent()).isEqualTo("지금 모임에 참여해보세요");
    assertThat(notification.getType()).isEqualTo(NotificationType.MEETING_STARTED);
    assertThat(notification.getRelatedId()).isEqualTo(1L);
  }

  @Test
  void skipsStartingMeetingBeforeStartDate() {
    Meeting meeting = mock(Meeting.class);
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 20, 0);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUIT_CLOSED);
    given(meeting.canStart()).willReturn(true);
    given(meeting.getStartDate()).willReturn(now.plusMinutes(1));

    boolean started = meetingCommandService.startMeeting(1L, now);

    assertThat(started).isFalse();
    verify(meeting, never()).start();
  }

  @Test
  void createsMeetingParticipant() {
    Meeting meeting = mock(Meeting.class);
    Member member = mock(Member.class);
    MeetingParticipant savedParticipant = mock(MeetingParticipant.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(authenticationFacade.getCurrentMemberId()).willReturn(10L);
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getCurParticipants()).willReturn(1);
    given(meeting.getMaxParticipants()).willReturn(4);
    given(memberRepository.getReferenceById(10L)).willReturn(member);
    given(meetingParticipantRepository.save(org.mockito.ArgumentMatchers.any()))
        .willReturn(savedParticipant);
    given(savedParticipant.getId()).willReturn(100L);

    MeetingParticipationResDTO response = meetingCommandService.participateMeeting(1L);

    assertThat(response.meetingParticipantId()).isEqualTo(100L);
    verify(meetingParticipantRepository).save(org.mockito.ArgumentMatchers.any());
    verify(meeting).addParticipant();
  }

  @Test
  void rejectsUnknownMeeting() {
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.MEETING_NOT_FOUND);
  }

  @Test
  void rejectsDuplicateMeeting() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getCurParticipants()).willReturn(1);
    given(meeting.getMaxParticipants()).willReturn(4);
    given(authenticationFacade.getCurrentMemberId()).willReturn(10L);
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(1L, 10L)).willReturn(true);

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.DUPLICATE_MEETING);
    verify(meetingParticipantRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMeetingThatIsNotRecruiting() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.IN_PROGRESS);

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);
    verify(meetingParticipantRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsRecruitingMeetingThatIsAlreadyFull() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getCurParticipants()).willReturn(4);
    given(meeting.getMaxParticipants()).willReturn(4);

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);
    verify(meetingParticipantRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void closesRecruitmentAndRejectsParticipationAfterDeadlineWhenMinimumWasReached() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getCurParticipants()).willReturn(3);
    given(meeting.getMaxParticipants()).willReturn(6);
    given(meeting.isRecruitmentClosedAt(any(LocalDateTime.class))).willReturn(true);
    given(meeting.canStart()).willReturn(true);

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);

    verify(meeting).closeRecruitment();
    verify(meetingParticipantRepository, never()).save(any());
    verify(meetingRepository, never()).delete(any());
  }

  @Test
  void cancelsMeetingAndRejectsParticipationAfterDeadlineWhenUnderstaffed() {
    Meeting meeting = mock(Meeting.class);
    Book book = mock(Book.class);
    Member member = mock(Member.class);
    MeetingParticipant participant = mock(MeetingParticipant.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getCurParticipants()).willReturn(2);
    given(meeting.getMaxParticipants()).willReturn(6);
    given(meeting.isRecruitmentClosedAt(any(LocalDateTime.class))).willReturn(true);
    given(meeting.canStart()).willReturn(false);
    given(meeting.getBook()).willReturn(book);
    given(meeting.getStartDate()).willReturn(LocalDateTime.of(2026, 8, 2, 2, 0));
    given(book.getTitle()).willReturn("혼모노");
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(1L))
        .willReturn(List.of(participant));
    given(participant.getMember()).willReturn(member);

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);

    verify(notificationCommandService).saveAll(any());
    verify(reportRepository).deleteAllByMeetingId(1L);
    verify(chatRoomRepository).deleteAllByMeetingId(1L);
    verify(meetingParticipantRepository).deleteAllByMeetingId(1L);
    verify(meetingRepository).delete(meeting);
    verify(meetingParticipantRepository, never()).save(any());
  }

  @Test
  void participationCommitsDeadlineProcessingWhenRecruitmentClosedExceptionIsThrown()
      throws Exception {
    Method method = MeetingCommandService.class.getMethod("participateMeeting", Long.class);
    Transactional transactional = method.getAnnotation(Transactional.class);

    assertThat(transactional).isNotNull();
    assertThat(transactional.noRollbackFor()).containsExactly(MeetingException.class);
  }

  @Test
  void deletesUnderstaffedMeetingAfterRecruitmentDeadline() {
    Meeting meeting = mock(Meeting.class);
    Book book = mock(Book.class);
    Member member = mock(Member.class);
    MeetingParticipant participant = mock(MeetingParticipant.class);
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 20, 0);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getRecruitmentCloseDate()).willReturn(now.minusMinutes(1));
    given(meeting.getStartDate()).willReturn(now.plusHours(6).minusMinutes(1));
    given(meeting.canStart()).willReturn(false);
    given(meeting.getBook()).willReturn(book);
    given(book.getTitle()).willReturn("혼모노");
    given(meeting.getCurParticipants()).willReturn(2);
    given(meeting.getMaxParticipants()).willReturn(6);
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(1L))
        .willReturn(List.of(participant));
    given(participant.getMember()).willReturn(member);

    boolean deleted = meetingCommandService.processRecruitmentDeadline(1L, now);

    assertThat(deleted).isTrue();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
    verify(notificationCommandService).saveAll(notificationCaptor.capture());
    assertThat(notificationCaptor.getValue()).hasSize(1);
    Notification notification = notificationCaptor.getValue().get(0);
    assertThat(notification.getMember()).isSameAs(member);
    assertThat(notification.getType()).isEqualTo(NotificationType.MEETING_CANCELED);
    assertThat(notification.getRelatedId()).isNull();
    assertThat(notification.getContent()).isEqualTo("혼모노 | 8/2 (일) · 01:59 | 2/6");
    InOrder deletionOrder =
        inOrder(
            notificationCommandService,
            reportRepository,
            chatRoomRepository,
            meetingParticipantRepository,
            meetingRepository);
    deletionOrder.verify(notificationCommandService).saveAll(any());
    deletionOrder.verify(reportRepository).deleteAllByMeetingId(1L);
    deletionOrder.verify(chatRoomRepository).deleteAllByMeetingId(1L);
    deletionOrder.verify(meetingParticipantRepository).deleteAllByMeetingId(1L);
    deletionOrder.verify(meetingRepository).delete(meeting);
  }

  @Test
  void skipsDeletingUnknownMeeting() {
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    boolean deleted =
        meetingCommandService.processRecruitmentDeadline(1L, LocalDateTime.of(2026, 8, 1, 20, 0));

    assertThat(deleted).isFalse();
    verify(meetingParticipantRepository, never()).deleteAllByMeetingId(any());
    verify(reportRepository, never()).deleteAllByMeetingId(any());
    verify(chatRoomRepository, never()).deleteAllByMeetingId(any());
    verify(meetingRepository, never()).delete(any());
  }

  @Test
  void closesRecruitmentWhenMeetingReachedMinimumParticipants() {
    Meeting meeting = mock(Meeting.class);
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 20, 0);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getRecruitmentCloseDate()).willReturn(now);
    given(meeting.canStart()).willReturn(true);

    boolean processed = meetingCommandService.processRecruitmentDeadline(1L, now);

    assertThat(processed).isTrue();
    verify(meeting).closeRecruitment();
    // 마감 시각 도달 경로도 정원 충족 경로와 같은 이벤트로 AI 질문 준비를 시작해야 한다
    verify(eventPublisher).publishEvent(new MeetingRecruitClosedEvent(1L));
    verify(meetingParticipantRepository, never()).deleteAllByMeetingId(any());
    verify(chatRoomRepository, never()).deleteAllByMeetingId(any());
    verify(meetingRepository, never()).delete(any());
  }

  @Test
  void doesNotPublishRecruitClosedEventWhenUnderstaffedMeetingIsDeleted() {
    Meeting meeting = mock(Meeting.class);
    Book book = mock(Book.class);
    Member member = mock(Member.class);
    MeetingParticipant participant = mock(MeetingParticipant.class);
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 20, 0);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getRecruitmentCloseDate()).willReturn(now);
    given(meeting.canStart()).willReturn(false);
    given(meeting.getBook()).willReturn(book);
    given(book.getTitle()).willReturn("아몬드");
    given(meeting.getStartDate()).willReturn(now.plusHours(6));
    given(meeting.getCurParticipants()).willReturn(2);
    given(meeting.getMaxParticipants()).willReturn(6);
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(1L))
        .willReturn(List.of(participant));
    given(participant.getMember()).willReturn(member);

    meetingCommandService.processRecruitmentDeadline(1L, now);

    // 삭제되는 모임에 질문을 준비하면 낭비이자 고아 데이터가 된다
    verify(eventPublisher, never()).publishEvent(any(MeetingRecruitClosedEvent.class));
    verify(aiQuestionRepository).deleteAllByMeetingId(1L);
  }

  @Test
  void skipsDeletingMeetingBeforeRecruitmentDeadline() {
    Meeting meeting = mock(Meeting.class);
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 20, 0);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getRecruitmentCloseDate()).willReturn(now.plusMinutes(1));

    boolean deleted = meetingCommandService.processRecruitmentDeadline(1L, now);

    assertThat(deleted).isFalse();
    verify(meetingParticipantRepository, never()).deleteAllByMeetingId(any());
    verify(chatRoomRepository, never()).deleteAllByMeetingId(any());
    verify(meetingRepository, never()).delete(any());
  }

  @Test
  void waitsForMeetingLockBeforeDeletingRelatedData() throws Exception {
    Meeting meeting = mock(Meeting.class);
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 20, 0);
    CountDownLatch lockRequested = new CountDownLatch(1);
    CountDownLatch lockAcquired = new CountDownLatch(1);
    given(meetingRepository.findByIdForUpdate(1L))
        .willAnswer(
            invocation -> {
              lockRequested.countDown();
              if (!lockAcquired.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("모임 잠금 획득 대기 시간이 초과되었습니다.");
              }
              return Optional.of(meeting);
            });
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getRecruitmentCloseDate()).willReturn(now.minusMinutes(1));
    given(meeting.canStart()).willReturn(false);
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(1L)).willReturn(List.of());

    CompletableFuture<Boolean> deletion =
        CompletableFuture.supplyAsync(
            () -> meetingCommandService.processRecruitmentDeadline(1L, now));

    assertThat(lockRequested.await(1, TimeUnit.SECONDS)).isTrue();
    verify(chatRoomRepository, never()).deleteAllByMeetingId(any());
    verify(meetingParticipantRepository, never()).deleteAllByMeetingId(any());
    verify(meetingRepository, never()).delete(any());

    lockAcquired.countDown();
    assertThat(deletion.get(1, TimeUnit.SECONDS)).isTrue();

    InOrder deletionOrder =
        inOrder(
            notificationCommandService,
            chatRoomRepository,
            meetingParticipantRepository,
            meetingRepository);
    deletionOrder.verify(notificationCommandService).saveAll(List.of());
    deletionOrder.verify(chatRoomRepository).deleteAllByMeetingId(1L);
    deletionOrder.verify(meetingParticipantRepository).deleteAllByMeetingId(1L);
    deletionOrder.verify(meetingRepository).delete(meeting);
  }
}
