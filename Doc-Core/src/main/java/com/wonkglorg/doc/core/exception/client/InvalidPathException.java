package com.wonkglorg.doc.core.exception.client;

/**
 * Specialisation of a {@link ClientException} that is thrown when a path is invalid
 */
public class InvalidPathException extends ClientException{
	public InvalidPathException() {
	}
	
	public InvalidPathException(String message) {
		super(message);
	}
	
	public InvalidPathException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public InvalidPathException(Throwable cause) {
		super(cause);
	}
	
	public InvalidPathException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
