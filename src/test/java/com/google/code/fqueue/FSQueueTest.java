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
package com.google.code.fqueue;

import junit.framework.TestCase;

/**
 * @author sunli
 * @date 2010-8-13
 * @version $Id$
 */
public class FSQueueTest extends TestCase {
	private static FQueue queue;
	static {
		try {
			queue = new FQueue("db");
			queue.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void setUp() throws Exception {

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void tesssstCrash() {
		queue.offer("testqueueoffer".getBytes());
		System.exit(9);
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void testOffer() {
		queue.offer("testqueueoffer".getBytes());
		assertEquals(new String(queue.poll()), "testqueueoffer");
	}

	public void testPoll() {
		queue.offer("testqueuepoll".getBytes());
		assertEquals(new String(queue.poll()), "testqueuepoll");
	}

	public void testAdd() {
		queue.add("testqueueadd".getBytes());
		assertEquals(new String(queue.poll()), "testqueueadd");
	}

	public void testAll() {
		queue.add("test1".getBytes());
		queue.add("test2".getBytes());
		assertEquals(new String(queue.poll()), "test1");
		queue.add("test3".getBytes());
		queue.add("test4".getBytes());
		assertEquals(new String(queue.poll()), "test2");
		assertEquals(new String(queue.poll()), "test3");
		System.out.println(new String(queue.poll()));
		StringBuffer sBuffer = new StringBuffer(1024);
		for (int i = 0; i < 1024; i++) {
			sBuffer.append("a");
		}
		String string = sBuffer.toString();
		assertEquals(0, queue.size());
		for (int i = 0; i < 100000; i++) {
			byte[] b = (string + i).getBytes();
			queue.offer(b);
		}
		assertEquals(100000, queue.size());
		for (int i = 0; i < 100000; i++) {
			if (i == 85301) {
				System.out.println(i);
			}
			byte[] b = queue.poll();
			if (b == null) {
				i--;
				System.out.println("null" + i);
				continue;
			}
			assertEquals(new String(b), (string + i));
		}
		queue.add("123".getBytes());
		queue.add("123".getBytes());
		assertEquals(queue.size(), 2);
		queue.clear();
		assertNull(queue.poll());
	}

	public void testPerformance() {
		StringBuffer sBuffer = new StringBuffer(1024);
		for (int i = 0; i < 1024; i++) {
			sBuffer.append("a");
		}
		String string = sBuffer.toString();
		System.out.println("Test write 1000000 times 1K data to queue");
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			byte[] b = (string + i).getBytes();
			queue.offer(b);
		}
		System.out.println("spend time:" + (System.currentTimeMillis() - start)
				+ "ms");
		System.out.println("Test read 1000000 times 1K data from queue");
		start = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {

			byte[] b = queue.poll();
			if (b == null) {
				i--;
				System.out.println("null" + i);
				continue;
			}
		}
		assertEquals(0, queue.size());
		System.out.println("spend:" + (System.currentTimeMillis() - start)
				+ "ms");
	}

	public void testPerformance2() {
		StringBuffer sBuffer = new StringBuffer(1024);
		for (int i = 0; i < 10; i++) {
			sBuffer.append("a");
		}
		String string = sBuffer.toString();
		System.out.println("Test write 10000000 times 10 Bytes data to queue");
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000000;i++) {
			byte[] b = (string + i).getBytes();
			queue.offer(b);
		}
		System.out.println("spend time:" + (System.currentTimeMillis() - start)
				+ "ms");
		System.out.println("Test read 10000000 times 10 bytes data from queue");
		start = System.currentTimeMillis();
		for (int i = 0; i < 10000000; i++) {
			byte[] b = queue.poll();
			if (b == null) {
				i--;
				System.out.println("null" + i);
				continue;
			}
		}
		assertEquals(0, queue.size());
		System.out.println("spend:" + (System.currentTimeMillis() - start)
				+ "ms");
	}
}
