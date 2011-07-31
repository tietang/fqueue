package com.thimbleware.jmemcached.protocol;

import com.thimbleware.jmemcached.Cache;
import com.thimbleware.jmemcached.CacheElement;

import java.io.Serializable;
import java.util.Set;
import java.util.Map;

/**
 * Represents the response to a command.
 */
public final class ResponseMessage<CACHE_ELEMENT extends CacheElement> implements Serializable {

    public ResponseMessage(CommandMessage cmd) {
        this.cmd = cmd;
    }

    public CommandMessage<CACHE_ELEMENT> cmd;
    public CACHE_ELEMENT[] elements;
    public Cache.StoreResponse response;
    public Map<String, Set<String>> stats;
    public String version;
    public Cache.DeleteResponse deleteResponse;
    public Integer incrDecrResponse;
    public boolean flushSuccess;

    public ResponseMessage<CACHE_ELEMENT> withElements(CACHE_ELEMENT[] elements) {
        this.elements = elements;
        return this;
    }

    public ResponseMessage<CACHE_ELEMENT> withResponse(Cache.StoreResponse response) {
        this.response = response;
        return this;
    }

    public ResponseMessage<CACHE_ELEMENT> withDeleteResponse(Cache.DeleteResponse deleteResponse) {
        this.deleteResponse = deleteResponse;
        return this;
    }

    public ResponseMessage<CACHE_ELEMENT> withIncrDecrResponse(Integer incrDecrResp) {
        this.incrDecrResponse = incrDecrResp;

        return this;
    }

    public ResponseMessage<CACHE_ELEMENT> withStatResponse(Map<String, Set<String>> stats) {
        this.stats = stats;

        return this;
    }

    public ResponseMessage<CACHE_ELEMENT> withFlushResponse(boolean success) {
        this.flushSuccess = success;

        return this;
    }
}
