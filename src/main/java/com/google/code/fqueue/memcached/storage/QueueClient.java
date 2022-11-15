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
package com.google.code.fqueue.memcached.storage;

import java.util.LinkedList;

/**
 * @author sunli
 * @version $Id$
 * @date 2011-5-23
 */
public class QueueClient {
    public static String[] parse(String keyString) {
        return parse(keyString, '_');
    }

    /**
     * 按照"_"进行切分
     *
     * @param keyString
     * @return
     */
    public static String[] parseWithCache(String keyString) {
        return parse(keyString, '_');
    }

    public static String[] parse(String keyString, Character separator) {
        LinkedList<String> list = new LinkedList<String>();
        int start = 0;
        for (int i = 0, len = keyString.length(); i < len; i++) {
            if (keyString.charAt(i) == separator) {
                list.add(keyString.substring(start, i));
                start = i + 1;
            }
            if (i == len - 1) {
                list.add(keyString.substring(start));
            }
        }
        String[] result = new String[list.size()];
        return list.toArray(result);

    }

}
