package com.thimbleware.jmemcached.protocol.exceptions;

/**
 */
public class ClientException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7348226175632572914L;

	public ClientException() {
	}

	public ClientException(String s) {
		super(s);
	}

	public ClientException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public ClientException(Throwable throwable) {
		super(throwable);
	}

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}
