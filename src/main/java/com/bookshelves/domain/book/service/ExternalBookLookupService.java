package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient;
import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.ExternalBookCacheRepository;
import com.bookshelves.domain.book.repository.ExternalBookCacheRepository.CachedBookDetail;
import com.bookshelves.domain.book.util.IsbnNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalBookLookupService {

  private final KakaoBookSearchClient kakaoBookSearchClient;
  private final Data4LibraryBookDetailClient data4LibraryBookDetailClient;
  private final ExternalBookCacheRepository externalBookCacheRepository;

  public KakaoBookSearchResult search(String normalizedQuery, int page, int size) {
    return externalBookCacheRepository
        .findSearch(normalizedQuery, page, size)
        .orElseGet(() -> searchAndCache(normalizedQuery, page, size));
  }

  public CachedBookDetail findByIsbn(String requestedIsbn, String canonicalIsbn) {
    return externalBookCacheRepository
        .findDetail(canonicalIsbn)
        .orElseGet(() -> findDetailAndCache(requestedIsbn, canonicalIsbn));
  }

  private KakaoBookSearchResult searchAndCache(String normalizedQuery, int page, int size) {
    KakaoBookSearchResult searchResult = kakaoBookSearchClient.search(normalizedQuery, page, size);
    externalBookCacheRepository.saveSearch(normalizedQuery, page, size, searchResult);
    return searchResult;
  }

  private CachedBookDetail findDetailAndCache(String requestedIsbn, String canonicalIsbn) {
    KakaoBookItem item =
        kakaoBookSearchClient.searchByIsbn(requestedIsbn).books().stream()
            .findFirst()
            .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
    String externalIsbn = IsbnNormalizer.normalize(item.isbn()).orElse(requestedIsbn);
    String canonicalExternalIsbn = IsbnNormalizer.toIsbn13(externalIsbn);
    KdcInfo kdcInfo = data4LibraryBookDetailClient.findKdcByIsbn(canonicalExternalIsbn);
    CachedBookDetail bookDetail = new CachedBookDetail(item, canonicalExternalIsbn, kdcInfo);
    externalBookCacheRepository.saveDetail(canonicalIsbn, bookDetail);
    return bookDetail;
  }
}
