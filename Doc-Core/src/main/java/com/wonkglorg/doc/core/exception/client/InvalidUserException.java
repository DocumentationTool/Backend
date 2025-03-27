package com.wonkglorg.doc.core.exception.client;

/**
 * Specialisation of a {@link ClientException} that is thrown when a user is invalid
 */
public class InvalidUserException extends ClientException{
	public InvalidUserException() {
	}
	
	public InvalidUserException(String message) {
		super(message);
	}
	
	public InvalidUserException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public InvalidUserException(Throwable cause) {
		super(cause);
	}
	
	public InvalidUserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
