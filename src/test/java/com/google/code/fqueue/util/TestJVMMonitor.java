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
package com.google.code.fqueue.util;

import org.apache.commons.lang.StringUtils;

import junit.framework.TestCase;

/**
 * @author sunli
 */
public class TestJVMMonitor extends TestCase {

    /**
     * @param name
     */
    public TestJVMMonitor(String name) {
        super(name);
    }

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMonitor() {
        String items = "fileDescriptor,load,allThreadsCount,peakThreadCount,daemonThreadCount,totalStartedThreadCount,deadLockCount,heapMemory,noHeapMemory,memory,classCount,GCTime,memoryPoolCollectionUsage,memoryPoolUsage,memoryPoolPeakUsage";
        // String items = "memoryPoolPeakUsage";
        long start = System.currentTimeMillis();
        if (items != null) {
            String[] itemList = StringUtils.split(items, ",");
            for (int i = 0, len = itemList.length; i < len; i++) {
                String data = JVMMonitor.getMonitorStats(itemList[i]);
                System.out.println(data);
            }
        }
        System.out.println("获取JVM监控信息耗时：" + (System.currentTimeMillis() - start));
    }
}
