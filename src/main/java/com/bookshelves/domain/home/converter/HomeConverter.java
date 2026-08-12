package com.bookshelves.domain.home.converter;

import com.bookshelves.domain.ai.entity.AIRecommendation;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.entity.MemberBook;
import com.bookshelves.domain.book.enums.MemberBookStatus;
import com.bookshelves.domain.home.dto.response.HomeResDTO;
import com.bookshelves.domain.home.dto.response.HomeResDTO.BookInfo;
import com.bookshelves.domain.home.dto.response.HomeResDTO.MeetingBookInfo;
import com.bookshelves.domain.home.dto.response.HomeResDTO.MeetingInfo;
import com.bookshelves.domain.home.dto.response.HomeResDTO.MeetingSummary;
import com.bookshelves.domain.home.dto.response.HomeResDTO.MemberBookRecord;
import com.bookshelves.domain.home.dto.response.HomeResDTO.MemberInfo;
import com.bookshelves.domain.home.dto.response.HomeResDTO.RecentBookInfo;
import com.bookshelves.domain.home.dto.response.HomeResDTO.RecommendedBookInfo;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.member.entity.Member;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeConverter {

  public static HomeResDTO toHomeResDTO(
      Member member,
      AIRecommendation recommendation,
      MemberBook recentBook,
      List<Meeting> meetings) {
    return new HomeResDTO(
        new MemberInfo(member.getNickname()),
        recommendation == null ? null : recommendation.getRecommendedDate(),
        toRecommendedBookInfo(recommendation),
        toRecentBookInfo(recentBook),
        meetings.stream().map(HomeConverter::toMeetingInfo).toList());
  }

  private static RecommendedBookInfo toRecommendedBookInfo(AIRecommendation recommendation) {
    if (recommendation == null) {
      return null;
    }
    return new RecommendedBookInfo(
        recommendation.getRecommendationMessage(), toBookInfo(recommendation.getBook()));
  }

  private static RecentBookInfo toRecentBookInfo(MemberBook memberBook) {
    if (memberBook == null) {
      return null;
    }
    return new RecentBookInfo(
        new MemberBookRecord(
            memberBook.getId(),
            memberBook.getProgress(),
            MemberBookStatus.from(memberBook.getProgress()).name(),
            memberBook.getRating(),
            memberBook.getUpdatedAt()),
        toBookInfo(memberBook.getBook()));
  }

  private static BookInfo toBookInfo(Book book) {
    return new BookInfo(
        book.getId(),
        book.getIsbn(),
        book.getTitle(),
        book.getAuthor(),
        book.getPublisher(),
        book.getCoverImageUrl(),
        book.getKdcCode(),
        book.getKdcName());
  }

  // 별도 모임명 대신 책 제목을 카드 제목으로 사용한다.
  private static MeetingInfo toMeetingInfo(Meeting meeting) {
    Book book = meeting.getBook();
    return new MeetingInfo(
        new MeetingSummary(
            meeting.getId(),
            meeting.getStatus().name(),
            meeting.getStartDate(),
            meeting.getCurParticipants(),
            meeting.getMaxParticipants(),
            meeting.getDuration()),
        new MeetingBookInfo(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getPublisher(),
            book.getCoverImageUrl()));
  }
}
