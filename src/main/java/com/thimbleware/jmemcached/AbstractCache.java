package com.thimbleware.jmemcached;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.channel.ChannelHandlerContext;

import com.thimbleware.jmemcached.protocol.exceptions.DatabaseException;
/**
 * 在jmemcached的基础之上完善了stats信息
 * 
 * 
 */


/**
 * Abstract implementation of a cache handler for the memcache daemon; provides
 * some convenience methods and a general framework for implementation
 */
public abstract class AbstractCache<CACHE_ELEMENT extends CacheElement> implements Cache<CACHE_ELEMENT> {

	protected final AtomicLong started = new AtomicLong();

	protected final AtomicInteger getCmds = new AtomicInteger();
	protected final AtomicInteger setCmds = new AtomicInteger();
	protected final AtomicInteger getHits = new AtomicInteger();
	protected final AtomicInteger getMisses = new AtomicInteger();
	protected final AtomicLong casCounter = new AtomicLong(1);
	protected final OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();

	public AbstractCache() {
		initStats();
	}

	/** Get JVM CPU time in milliseconds */
	public long getJVMCpuTime() {
		if (!(bean instanceof com.sun.management.OperatingSystemMXBean))
			return 0L;
		return ((com.sun.management.OperatingSystemMXBean) bean).getProcessCpuTime();
	}

	public double getJVMLoad() {
		if (!(bean instanceof com.sun.management.OperatingSystemMXBean))
			return 0L;
		return ((com.sun.management.OperatingSystemMXBean) bean).getSystemLoadAverage();
	}

	public int getAllthreadsCount() {
		ThreadGroup group = Thread.currentThread().getThreadGroup();
		ThreadGroup topGroup = group;
		while (group != null) {
			topGroup = group;
			group = group.getParent();
		}
		return topGroup.activeCount();
	}

	/**
	 * @return the current time in seconds (from epoch), used for expiries, etc.
	 */
	public static int Now() {
		return (int) (System.currentTimeMillis() / 1000);
	}

	public abstract Set<String> keys();

	public abstract long getCurrentItems();

	public abstract long getLimitMaxBytes();

	public abstract long getCurrentBytes();

	public final int getGetCmds() {
		return getCmds.get();
	}

	public final int getSetCmds() {
		return setCmds.get();
	}

	public final int getGetHits() {
		return getHits.get();
	}

	public final int getGetMisses() {
		return getMisses.get();
	}

	/**
	 * Return runtime statistics
	 * 
	 * @param arg
	 *            additional arguments to the stats command
	 * @return the full command response
	 */
	public final Map<String, Set<String>> stat(String arg, ChannelHandlerContext channelHandlerContext) {
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		// stats we know
		multiSet(result, "version", MemCacheDaemon.memcachedVersion);
		multiSet(result, "cmd_get", java.lang.String.valueOf(getGetCmds()));
		multiSet(result, "cmd_set", java.lang.String.valueOf(getSetCmds()));
		multiSet(result, "get_hits", java.lang.String.valueOf(getGetHits()));
		multiSet(result, "get_misses", java.lang.String.valueOf(getGetMisses()));
		multiSet(result, "time", java.lang.String.valueOf(java.lang.String.valueOf(Now())));
		multiSet(result, "uptime", java.lang.String.valueOf(Now() - this.started.longValue()));
		multiSet(result, "curr_items", java.lang.String.valueOf(this.getCurrentItems()));
		multiSet(result, "total_items", java.lang.String.valueOf(this.getCurrentItems()));
		multiSet(result, "limit_maxbytes", java.lang.String.valueOf(this.getLimitMaxBytes()));
		multiSet(result, "current_bytes", java.lang.String.valueOf(this.getCurrentBytes()));
		multiSet(result, "bytes", java.lang.String.valueOf(this.getCurrentBytes()));
		multiSet(result, "free_bytes", java.lang.String.valueOf(Runtime.getRuntime().freeMemory()));

		// Not really the same thing precisely, but meaningful nonetheless.
		// potentially this should be renamed
		multiSet(result, "pid", java.lang.String.valueOf(Thread.currentThread().getId()));

		// stuff we know nothing about; gets faked only because some clients
		// expect this
		multiSet(result, "rusage_user", String.valueOf(TimeUnit.NANOSECONDS.toSeconds(getJVMCpuTime())));
		multiSet(result, "rusage_system", "0.0");
		multiSet(result, "connection_structures", "0");
		multiSet(result, "curr_connections", String.valueOf(StatsCounter.curr_conns.longValue()));
		multiSet(result, "total_connections", String.valueOf(StatsCounter.total_conns.longValue()));

		// TODO we could collect these stats
		multiSet(result, "bytes_read", String.valueOf(StatsCounter.bytes_read.longValue()));
		multiSet(result, "bytes_written", String.valueOf(StatsCounter.bytes_written.longValue()));
		multiSet(result, "system_load", String.valueOf(getJVMLoad()));
		multiSet(result, "threads", String.valueOf(getAllthreadsCount()));
		return result;
	}

	private void multiSet(Map<String, Set<String>> map, String key, String val) {
		Set<String> cur = map.get(key);
		if (cur == null) {
			cur = new HashSet<String>();
		}
		cur.add(val);
		map.put(key, cur);
	}

	/**
	 * Initialize all statistic counters
	 */
	protected void initStats() {
		started.set(System.currentTimeMillis() / 1000);
		getCmds.set(0);
		setCmds.set(0);
		getHits.set(0);
		getMisses.set(0);

	}

	public abstract void asyncEventPing() throws DatabaseException, Exception;
}
