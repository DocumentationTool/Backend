package com.wonkglorg.doc.core.exception.client;

/**
 * Specialisation of a {@link ClientException} that is thrown when a tag already exists but shouldn't for the operation
 */
public class TagExistsException extends ClientException {
  public TagExistsException() {
  }

  public TagExistsException(String message) {
    super(message);
  }

  public TagExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public TagExistsException(Throwable cause) {
    super(cause);
  }

  public TagExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
