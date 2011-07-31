package com.thimbleware.jmemcached.protocol.exceptions;

/**
 */
public class UnknownCommandException extends ClientException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1826699280038666904L;

	public UnknownCommandException() {
    }

    public UnknownCommandException(String s) {
        super(s);
    }

    public UnknownCommandException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public UnknownCommandException(Throwable throwable) {
        super(throwable);
    }
}
