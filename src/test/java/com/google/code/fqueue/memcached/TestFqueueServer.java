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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.google.code.fqueue.util.Config;
import com.google.code.yanf4j.core.impl.StandardSocketOption;

/**
 * @author sunli
 */
public class TestFqueueServer extends TestCase {
    private final static Log log = LogFactory.getLog(TestFqueueServer.class);
    public static final AtomicInteger counter = new AtomicInteger(0);
    private static MemcachedClientBuilder builder = null;
    private static MemcachedClient client;
    private static String keyName = "key_abc";
    static {
        PropertyConfigurator.configure("config/log4j.properties");

    }

    public void deleteData() {
        File file = new File("dbtest");
        file.delete();
    }

    /**
     * @param name
     */
    public TestFqueueServer(String name) {
        super(name);
    }

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        Config.setSetting("port", "12001");
        Config.setSetting("path", "dbtest");
        Config.setSetting("logsize", "40");
        Config.setSetting("authorization", "key|abc@@bbs|pass");
        StartNewQueue.newQueueInstance(Integer.parseInt(Config.getSetting("port")));
        log.info("running at port " + Config.getSetting("port"));
        builder = new XMemcachedClientBuilder(AddrUtil.getAddresses("127.0.0.1:12001"));
        builder.setConnectionPoolSize(50); // set connection pool size to five
        builder.setSocketOption(StandardSocketOption.SO_KEEPALIVE, true);
        builder.setSocketOption(StandardSocketOption.SO_RCVBUF, 64 * 1024);
        builder.setSocketOption(StandardSocketOption.SO_SNDBUF, 64 * 1024);
        builder.setSocketOption(StandardSocketOption.SO_REUSEADDR, true);
        builder.setSocketOption(StandardSocketOption.TCP_NODELAY, false);
        try {
            client = builder.build();
            client.setOptimizeGet(false);
           
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        client.get("clear|key|abc");
    }

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private int getSize() throws TimeoutException, InterruptedException, MemcachedException {
        String value = client.get("size|key");
        if (value != null) {
            return Integer.parseInt(value);
        }
        return -1;
    }

    public void authorization() throws TimeoutException, InterruptedException, IOException {
        try {
            client.get("xxxxxxxxxx");
        } catch (MemcachedException e) {
            assertEquals("xxxxxxxxxx command Unsupported now", e.getMessage());
        }
        try {
            client.get("xxxxxxxxxx_xxx");
        } catch (MemcachedException e) {
            assertEquals("Authorization error", e.getMessage());
        }
    }

    public void testOperation() throws InterruptedException, TimeoutException, MemcachedException, IOException {
        assertEquals(0, getSize());
        client.set(keyName, 0, "12345");
        assertEquals(1, getSize());
        assertEquals("12345", client.get(keyName));
        assertEquals(0, getSize());
        client.set(keyName + "_" + System.currentTimeMillis(), 0, "12345");
        assertEquals(1, getSize());
        assertEquals("12345", client.get(keyName + "_" + System.currentTimeMillis()));
        log.info("push 10000 items");
        long start = System.currentTimeMillis();
        // 测试顺序写入10000个数据，再按顺序取出来，是否正确
        for (int i = 0; i < 10000; i++) {
            client.set(keyName, 0, "value_" + i);
        }
        log.info("push 10000 items :" + (System.currentTimeMillis() - start) + " ms");
        assertEquals(10000, getSize());
        log.info("poll 10000 items");
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            assertEquals("value_" + i, client.get(keyName));
        }
        log.info("poll 10000 items :" + (System.currentTimeMillis() - start) + " ms");
        assertEquals(0, getSize());
        log.info("开始测试权限状态");
        authorization();
        log.info("开始测试多线程操作");
        mutiThreadWrite();
        mutiThreadGet();
        assertEquals(0, getSize());
    }

    public void mutiThreadWrite() throws InterruptedException, TimeoutException, MemcachedException {
        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        // MemcachedBenchJob.test = tester;
        MemcachedTest[] muti = new MemcachedTest[threadCount];
        for (int i = 0; i < threadCount; i++) {
            muti[i] = new MemcachedTest(latch);
        }
        log.info("start");
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            pool.execute(muti[i]);
        }
        latch.await();
        long spend = System.currentTimeMillis() - start;

        log.info(threadCount + "threads写入次数:" + threadCount * 10000 + " spend:" + spend + " ms");
        assertEquals(threadCount * 10000, getSize());

    }

    public void mutiThreadGet() throws InterruptedException, TimeoutException, MemcachedException {
        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        // MemcachedBenchJob.test = tester;
        MemcachedTestGet[] muti = new MemcachedTestGet[threadCount];
        for (int i = 0; i < threadCount; i++) {
            muti[i] = new MemcachedTestGet(latch);
        }
        log.info("start");
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            pool.execute(muti[i]);
        }
        latch.await();
        long spend = System.currentTimeMillis() - start;
        log.info(threadCount + "threads 获取次数:" + threadCount * 10000);
        assertEquals(0, getSize());

    }

    public class MemcachedTest implements Runnable {

        public CountDownLatch latch;

        public MemcachedTest(CountDownLatch latch) {
            this.latch = latch;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
                for (int i = 0; i < 10000; i++) {
                    client.set(keyName, 0, String.valueOf(counter.incrementAndGet()));
                }
                latch.countDown();
            } catch (TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MemcachedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public class MemcachedTestGet implements Runnable {

        public CountDownLatch latch;

        public MemcachedTestGet(CountDownLatch latch) {
            this.latch = latch;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
                for (int i = 0; i < 10000; i++) {
                    client.get(keyName);
                }
                latch.countDown();
            } catch (TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MemcachedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}
