/**
 *  Copyright 2008 ThimbleWare Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.thimbleware.jmemcached.protocol;

import com.thimbleware.jmemcached.CacheElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The payload object holding the parsed message.
 */
public final class CommandMessage<CACHE_ELEMENT extends CacheElement> implements Serializable {

    public static enum ErrorType {
        OK, ERROR, CLIENT_ERROR
    }

    public Command cmd;
    public CACHE_ELEMENT element;
    public List<String> keys;
    public boolean noreply;
    public Long cas_key;
    public int time = 0;
    public ErrorType error = ErrorType.OK;
    public String errorString;
    public int opaque;
    public boolean addKeyToResponse = false;

    public Integer incrDefault;
    public int incrExpiry;
    public int incrAmount;
    
    private CommandMessage(Command cmd) {
        this.cmd = cmd;
        element = null;
        keys = new ArrayList<String>();
    }

    public static CommandMessage error(String errorString) {
        CommandMessage errcmd = new CommandMessage(null);
        errcmd.error = ErrorType.ERROR;
        errcmd.errorString = errorString;
        return errcmd;
    }

    public static CommandMessage clientError(String errorString) {
        CommandMessage errcmd = new CommandMessage(null);
        errcmd.error = ErrorType.CLIENT_ERROR;
        errcmd.errorString = errorString;
        return errcmd;
    }

    public static CommandMessage command(Command cmd) {
        return new CommandMessage(cmd);
    }
}