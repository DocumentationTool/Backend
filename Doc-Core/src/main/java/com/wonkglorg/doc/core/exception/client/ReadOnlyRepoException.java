package com.wonkglorg.doc.core.exception.client;

/**
 * Specialisation of a {@link ClientException} that is thrown when a repository database tries to execute a file modification request while the underlying repository is marked as read-only
 */
public class ReadOnlyRepoException extends ClientException {
	
	public ReadOnlyRepoException(String message) {
		super(message);
	}
	
	public ReadOnlyRepoException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ReadOnlyRepoException(Throwable cause) {
		super(cause);
	}
	
	public ReadOnlyRepoException() {
		super();
	}

}
