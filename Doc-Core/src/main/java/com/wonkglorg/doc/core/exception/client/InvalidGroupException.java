package com.wonkglorg.doc.core.exception.client;

/**
 * Specialisation of a {@link ClientException} that is thrown when a group is invalid
 */
public class InvalidGroupException extends ClientException{
    public InvalidGroupException() {
    }

    public InvalidGroupException(String message) {
        super(message);
    }

    public InvalidGroupException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidGroupException(Throwable cause) {
        super(cause);
    }

    public InvalidGroupException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
