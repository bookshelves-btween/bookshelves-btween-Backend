package com.bookshelves.domain.book.exception;

import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class BookException extends ProjectException {

  public BookException(BookErrorCode errorCode) {
    super(errorCode);
  }
}
