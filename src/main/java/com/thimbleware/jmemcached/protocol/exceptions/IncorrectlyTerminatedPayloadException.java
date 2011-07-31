package com.thimbleware.jmemcached.protocol.exceptions;

/**
 */
public class IncorrectlyTerminatedPayloadException extends ClientException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3832455564964217827L;

	public IncorrectlyTerminatedPayloadException() {
    }

    public IncorrectlyTerminatedPayloadException(String s) {
        super(s);
    }

    public IncorrectlyTerminatedPayloadException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public IncorrectlyTerminatedPayloadException(Throwable throwable) {
        super(throwable);
    }
}
