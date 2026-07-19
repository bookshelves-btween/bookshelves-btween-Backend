package com.bookshelves.domain.meeting.entity;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "start_date", nullable = false)
  private LocalDateTime startDate;

  @Column(name = "duration", nullable = false)
  private Integer duration;

  @Column(name = "max_participants", nullable = false)
  private Integer maxParticipants;

  @Column(name = "cur_participants", nullable = false)
  private Integer curParticipants;

  @Column(name = "real_participants", nullable = false)
  private Integer realParticipants = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private MeetingStatus status = MeetingStatus.RECRUITING;
}
