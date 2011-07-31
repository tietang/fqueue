package com.google.code.fqueue;

import com.thimbleware.jmemcached.protocol.exceptions.DatabaseException;

import junit.framework.TestCase;

public class DatabaseExceptionTest extends TestCase {
	public void testPerformance() {
		DatabaseException exception = new DatabaseException("password wrong");
		exception.printStackTrace();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000000; i++) {
			exception = new DatabaseException("password wrong");

		}
		System.out.println("spend:" + (System.currentTimeMillis() - start)
				+ "ms");
	}
}
