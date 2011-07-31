package com.thimbleware.jmemcached.protocol;

import java.io.Serializable;

/**
 * Class for holding the current session status.
 */
public final class SessionStatus implements Serializable {

    /**
     * Possible states that the current session is in.
     */
    public static enum State {
        WAITING_FOR_DATA,
        READY,
        PROCESSING,
        PROCESSING_MULTILINE,
    }

    // the state the session is in
    public State state;

    // if we are waiting for more data, how much?
    public int bytesNeeded;

    // the current working command
    public CommandMessage cmd;


    public SessionStatus() {
        ready();
    }

    public SessionStatus ready() {
        this.cmd = null;
        this.bytesNeeded = -1;
        this.state = State.READY;

        return this;
    }

    public SessionStatus processing() {
        this.state = State.PROCESSING;

        return this;
    }

    public SessionStatus processingMultiline() {
        this.state = State.PROCESSING_MULTILINE;

        return this;
    }

    public SessionStatus needMore(int size, CommandMessage cmd) {
        this.cmd = cmd;
        this.bytesNeeded = size;
        this.state = State.WAITING_FOR_DATA;

        return this;
    }

}
