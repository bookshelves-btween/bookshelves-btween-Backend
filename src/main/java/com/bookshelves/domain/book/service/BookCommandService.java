package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.converter.BookConverter;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.BookRepository;
import com.bookshelves.domain.book.util.IsbnNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BookCommandService {

  private final BookRepository bookRepository;
  private final KakaoBookSearchClient kakaoBookSearchClient;

  public Book getOrCreateByIsbn(String rawIsbn) {
    String requestedIsbn =
        IsbnNormalizer.normalize(rawIsbn)
            .orElseThrow(() -> new BookException(BookErrorCode.INVALID_BOOK_ISBN));
    String canonicalIsbn = IsbnNormalizer.toIsbn13(requestedIsbn);

    return bookRepository
        .findByIsbn(canonicalIsbn)
        .orElseGet(() -> saveExternalBook(requestedIsbn, canonicalIsbn));
  }

  private Book saveExternalBook(String requestedIsbn, String canonicalIsbn) {
    KakaoBookItem item =
        kakaoBookSearchClient.searchByIsbn(requestedIsbn).books().stream()
            .findFirst()
            .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));

    Book book = BookConverter.toEntity(item, canonicalIsbn);
    return bookRepository.save(book);
  }
}
