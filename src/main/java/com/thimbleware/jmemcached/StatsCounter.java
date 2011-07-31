package com.thimbleware.jmemcached;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StatsCounter {
	public static final AtomicLong bytes_written = new AtomicLong();
	public static final AtomicLong bytes_read = new AtomicLong();
	public static final AtomicInteger curr_conns = new AtomicInteger();
	public static final AtomicInteger total_conns = new AtomicInteger();
}
