package com.wonkglorg.doc.core.exception.client;

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
