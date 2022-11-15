/*
 *  Copyright 2011 sunli [sunli1223@gmail.com][weibo.com@sunli1223]
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
package com.google.code.fqueue.exception;

/**
 * @author sunli
 * @version $Id$
 * @date 2011-5-18
 */
public class FileEOFException extends Exception {

    private static final long serialVersionUID = 4701796168682302255L;

    public FileEOFException() {
        super();
    }

    public FileEOFException(String message) {
        super(message);
    }

    public FileEOFException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileEOFException(Throwable cause) {
        super(cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
