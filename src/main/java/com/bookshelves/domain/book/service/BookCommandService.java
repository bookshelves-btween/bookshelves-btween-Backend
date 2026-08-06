package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient;
import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.converter.BookConverter;
import com.bookshelves.domain.book.dto.request.MemberBookUpsertReqDTO;
import com.bookshelves.domain.book.dto.response.MemberBookUpsertResDTO;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.entity.MemberBook;
import com.bookshelves.domain.book.entity.MemberBookHistory;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.BookRepository;
import com.bookshelves.domain.book.repository.MemberBookHistoryRepository;
import com.bookshelves.domain.book.repository.MemberBookRepository;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository;
import com.bookshelves.domain.book.util.IsbnNormalizer;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class BookCommandService {

  private final BookRepository bookRepository;
  private final MemberBookRepository memberBookRepository;
  private final MemberBookHistoryRepository memberBookHistoryRepository;
  private final MemberRepository memberRepository;
  private final RecentBookSearchRepository recentBookSearchRepository;
  private final AuthenticationFacade authenticationFacade;
  private final KakaoBookSearchClient kakaoBookSearchClient;
  private final Data4LibraryBookDetailClient data4LibraryBookDetailClient;
  private final TransactionTemplate transactionTemplate;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Book getOrCreateByIsbn(String rawIsbn) {
    PreparedBook preparedBook = prepareBook(rawIsbn);
    if (preparedBook.persisted()) {
      return preparedBook.book();
    }
    return transactionTemplate.execute(status -> persistPreparedBook(preparedBook));
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public PreparedBook prepareBook(String rawIsbn) {
    String requestedIsbn =
        IsbnNormalizer.normalize(rawIsbn)
            .orElseThrow(() -> new BookException(BookErrorCode.INVALID_BOOK_ISBN));
    String canonicalIsbn = IsbnNormalizer.toIsbn13(requestedIsbn);

    Book savedBook = bookRepository.findByIsbn(canonicalIsbn).orElse(null);
    if (savedBook != null) {
      return new PreparedBook(savedBook, canonicalIsbn, true);
    }

    Book externalBook = fetchExternalBook(requestedIsbn, canonicalIsbn);
    return new PreparedBook(externalBook, canonicalIsbn, false);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Book persistPreparedBook(PreparedBook preparedBook) {
    return preparedBook.persisted()
        ? preparedBook.book()
        : saveExternalBook(preparedBook.book(), preparedBook.canonicalIsbn());
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public MemberBookUpsertResult upsertMemberBook(String rawIsbn, MemberBookUpsertReqDTO request) {
    PreparedBook preparedBook = prepareBook(rawIsbn);
    Long memberId = authenticationFacade.getCurrentMemberId();

    return transactionTemplate.execute(
        status -> {
          Book book = persistPreparedBook(preparedBook);
          return memberRepository
              .findByIdForUpdate(memberId)
              .map(lockedMember -> upsertLockedMemberBook(memberId, lockedMember, book, request))
              .orElseThrow(() -> new IllegalStateException("인증된 회원을 찾을 수 없습니다."));
        });
  }

  public void deleteRecentBookSearch(String keyword) {
    if (keyword == null) {
      throw new BookException(BookErrorCode.INVALID_RECENT_BOOK_SEARCH_DELETE_REQUEST);
    }

    String normalizedKeyword = keyword.strip();
    if (normalizedKeyword.isBlank()) {
      throw new BookException(BookErrorCode.INVALID_RECENT_BOOK_SEARCH_DELETE_REQUEST);
    }

    Long memberId = authenticationFacade.getCurrentMemberId();
    try {
      recentBookSearchRepository.delete(memberId, normalizedKeyword);
    } catch (DataAccessException exception) {
      throw new BookException(BookErrorCode.RECENT_BOOK_SEARCH_DELETE_FAILED);
    }
  }

  @Transactional
  public void deleteMemberBook(String rawIsbn) {
    String requestedIsbn =
        IsbnNormalizer.normalize(rawIsbn)
            .orElseThrow(() -> new BookException(BookErrorCode.INVALID_BOOK_ISBN));
    String canonicalIsbn = IsbnNormalizer.toIsbn13(requestedIsbn);
    Long memberId = authenticationFacade.getCurrentMemberId();

    memberRepository
        .findByIdForUpdate(memberId)
        .orElseThrow(() -> new IllegalStateException("인증된 회원을 찾을 수 없습니다."));

    MemberBook memberBook =
        memberBookRepository
            .findByMemberIdAndBookIsbn(memberId, canonicalIsbn)
            .orElseThrow(() -> new BookException(BookErrorCode.MEMBER_BOOK_NOT_FOUND));

    memberBookHistoryRepository.deleteAllByMemberBookId(memberBook.getId());
    memberBookRepository.delete(memberBook);
  }

  private MemberBookUpsertResult upsertLockedMemberBook(
      Long memberId, Member member, Book book, MemberBookUpsertReqDTO request) {
    return memberBookRepository
        .findByMemberIdAndBookId(memberId, book.getId())
        .map(memberBook -> new MemberBookUpsertResult(false, updateMemberBook(memberBook, request)))
        .orElseGet(() -> new MemberBookUpsertResult(true, createMemberBook(member, book, request)));
  }

  private MemberBookUpsertResDTO createMemberBook(
      Member member, Book book, MemberBookUpsertReqDTO request) {
    MemberBook memberBook =
        memberBookRepository.save(
            MemberBook.create(book, member, request.progress(), request.rating(), request.memo()));

    return createHistoryIfProgressIncreased(memberBook, 0, request.progress());
  }

  private MemberBookUpsertResDTO updateMemberBook(
      MemberBook memberBook, MemberBookUpsertReqDTO request) {
    validateRatingCanBeCleared(memberBook.getRating(), request.rating());

    int previousProgress = memberBook.getProgress();
    memberBook.update(request.progress(), request.rating(), request.memo());

    return createHistoryIfProgressIncreased(memberBook, previousProgress, request.progress());
  }

  private void validateRatingCanBeCleared(BigDecimal previousRating, BigDecimal newRating) {
    if (previousRating != null && newRating == null) {
      throw new BookException(BookErrorCode.MEMBER_BOOK_RATING_CANNOT_BE_CLEARED);
    }
  }

  private MemberBookUpsertResDTO createHistoryIfProgressIncreased(
      MemberBook memberBook, int previousProgress, int newProgress) {
    if (newProgress <= previousProgress) {
      return MemberBookUpsertResDTO.withoutHistory();
    }

    MemberBookHistory history =
        memberBookHistoryRepository.save(MemberBookHistory.create(memberBook, newProgress));
    return MemberBookUpsertResDTO.withHistory(history.getId());
  }

  private Book fetchExternalBook(String requestedIsbn, String canonicalIsbn) {
    KakaoBookItem item =
        kakaoBookSearchClient.searchByIsbn(requestedIsbn).books().stream()
            .findFirst()
            .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));

    KdcInfo kdcInfo = data4LibraryBookDetailClient.findKdcByIsbn(canonicalIsbn);
    return BookConverter.toEntity(item, canonicalIsbn, kdcInfo);
  }

  private Book saveExternalBook(Book book, String canonicalIsbn) {
    bookRepository.upsert(
        book.getIsbn(),
        book.getTitle(),
        book.getAuthor(),
        book.getPublisher(),
        book.getPublishedDate(),
        book.getDescription(),
        book.getCoverImageUrl(),
        book.getKdcCode(),
        book.getKdcName());
    return bookRepository
        .findByIsbnForUpdate(canonicalIsbn)
        .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
  }

  public record MemberBookUpsertResult(boolean created, MemberBookUpsertResDTO response) {}

  public record PreparedBook(Book book, String canonicalIsbn, boolean persisted) {}
}
