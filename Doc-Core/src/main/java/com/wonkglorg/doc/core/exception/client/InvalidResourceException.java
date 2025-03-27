package com.wonkglorg.doc.core.exception.client;


/**
 * Specialisation of a {@link ClientException} that is thrown when a resource is invalid
 */
public class InvalidResourceException extends ClientException {
    public InvalidResourceException() {
    }

    public InvalidResourceException(String message) {
        super(message);
    }

    public InvalidResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidResourceException(Throwable cause) {
        super(cause);
    }

    public InvalidResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
