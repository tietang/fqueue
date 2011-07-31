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
package com.google.code.fqueue.memcached;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.fqueue.memcached.storage.FSStorage;
import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.CacheStorage;

/**
 * @author sunli
 * @date 2011-5-18
 * @version $Id$
 */
public class StartNewQueue {
    private static final Log log = LogFactory.getLog(StartNewQueue.class);

    public static void newQueueInstance(int port) {
        InetSocketAddress addr = new InetSocketAddress("0.0.0.0", port);
        int idle = -1;
        boolean verbose = false;
        MemCacheDaemon.memcachedVersion = "0.1";
        final MemCacheDaemon<LocalCacheElement> daemon = new MemCacheDaemon<LocalCacheElement>();
        CacheStorage<String, LocalCacheElement> storage = new FSStorage();
        CacheImpl cacheImpl = new CacheImpl(storage);
        daemon.setCache(cacheImpl);
        daemon.setAddr(addr);
        daemon.setBinary(false);
        daemon.setIdleTime(idle);
        daemon.setVerbose(verbose);
        daemon.start();
        log.info("\r\n\t         FQueue instance started,port:" + port
                + " [version 0.1] \r\n\t\t\t Copyright (C) 2011 sunli");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (daemon != null && daemon.isRunning())
                    daemon.stop();
                log.info("shutdown server");
            }
        }));
    }
}
