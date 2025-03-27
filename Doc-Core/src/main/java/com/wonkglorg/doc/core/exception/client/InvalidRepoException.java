package com.wonkglorg.doc.core.exception.client;

/**
 * Specialisation of a {@link ClientException} that is thrown when a repository is invalid
 */
public class InvalidRepoException extends ClientException{
	public InvalidRepoException() {
	}
	
	public InvalidRepoException(String message) {
		super(message);
	}
	
	public InvalidRepoException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public InvalidRepoException(Throwable cause) {
		super(cause);
	}
	
	public InvalidRepoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
