package com.bookshelves.domain.home.converter;

import com.bookshelves.domain.ai.entity.AIRecommendation;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.entity.MemberBook;
import com.bookshelves.domain.home.dto.response.HomeResDTO;
import com.bookshelves.domain.home.dto.response.HomeResDTO.MeetingInfo;
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
    Book book = recommendation.getBook();
    return new RecommendedBookInfo(
        recommendation.getRecommendationMessage(),
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getPublisher(),
        book.getKdcName(),
        book.getCoverImageUrl());
  }

  private static RecentBookInfo toRecentBookInfo(MemberBook memberBook) {
    if (memberBook == null) {
      return null;
    }
    Book book = memberBook.getBook();
    return new RecentBookInfo(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getPublisher(),
        book.getCoverImageUrl(),
        memberBook.getRating(),
        memberBook.getProgress());
  }

  // 모임 카드의 제목은 책 제목이다. 모임 자체에는 이름 필드가 없다.
  private static MeetingInfo toMeetingInfo(Meeting meeting) {
    Book book = meeting.getBook();
    return new MeetingInfo(
        meeting.getId(),
        book.getTitle(),
        book.getCoverImageUrl(),
        meeting.getStartDate(),
        meeting.getCurParticipants(),
        meeting.getMaxParticipants());
  }
}
