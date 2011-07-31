package com.thimbleware.jmemcached.protocol.exceptions;

/**
 */
public class MalformedCommandException extends ClientException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -514909313697495546L;

	public MalformedCommandException() {
    }

    public MalformedCommandException(String s) {
        super(s);
    }

    public MalformedCommandException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public MalformedCommandException(Throwable throwable) {
        super(throwable);
    }
}