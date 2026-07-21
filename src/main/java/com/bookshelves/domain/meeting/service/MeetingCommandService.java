package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.BookRepository;
import com.bookshelves.domain.meeting.converter.MeetingConverter;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MeetingCommandService {

  private final BookRepository bookRepository;
  private final MeetingRepository meetingRepository;

  public MeetingCreateResDTO createMeeting(String isbn, MeetingCreateReqDTO request) {
    Book book =
        bookRepository
            .findByIsbn(isbn)
            .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));

    Meeting meeting = MeetingConverter.toEntity(book, request);
    Meeting savedMeeting = meetingRepository.save(meeting);

    return MeetingCreateResDTO.from(savedMeeting);
  }
}
