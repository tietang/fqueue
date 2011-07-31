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
package com.thimbleware.jmemcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thimbleware.jmemcached.protocol.binary.MemcachedBinaryPipelineFactory;
import com.thimbleware.jmemcached.protocol.text.MemcachedPipelineFactory;

/**
 * The actual daemon - responsible for the binding and configuration of the
 * network configuration.
 */
public class MemCacheDaemon<CACHE_ELEMENT extends CacheElement> {

	final Logger log = LoggerFactory.getLogger(MemCacheDaemon.class);

	public static String memcachedVersion = "0.9";

	private int frameSize = 32768 * 1024;

	private boolean binary = false;
	private boolean verbose;
	private int idleTime;
	private InetSocketAddress addr;
	private Cache<CACHE_ELEMENT> cache;

	private boolean running = false;
	private ServerSocketChannelFactory channelFactory;
	private DefaultChannelGroup allChannels;

	public MemCacheDaemon() {
	}

	public MemCacheDaemon(Cache<CACHE_ELEMENT> cache) {
		this.cache = cache;
	}

	/**
	 * Bind the network connection and start the network processing threads.
	 */
	public void start() {
		// TODO provide tweakable options here for passing in custom executors.
		channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors
				.newCachedThreadPool());

		allChannels = new DefaultChannelGroup("jmemcachedChannelGroup");

		ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

		ChannelPipelineFactory pipelineFactory;
		if (binary)
			pipelineFactory = createMemcachedBinaryPipelineFactory(cache, memcachedVersion, verbose, idleTime,
					allChannels);
		else
			pipelineFactory = createMemcachedPipelineFactory(cache, memcachedVersion, verbose, idleTime, frameSize,
					allChannels);

		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.receiveBufferSize", 1024 * 64);
		bootstrap.setPipelineFactory(pipelineFactory);

		Channel serverChannel = bootstrap.bind(addr);
		allChannels.add(serverChannel);

		log.info("Listening on " + String.valueOf(addr.getHostName()) + ":" + addr.getPort());

		running = true;
	}

	protected ChannelPipelineFactory createMemcachedBinaryPipelineFactory(Cache cache, String memcachedVersion,
			boolean verbose, int idleTime, DefaultChannelGroup allChannels) {
		return new MemcachedBinaryPipelineFactory(cache, memcachedVersion, verbose, idleTime, allChannels);
	}

	protected ChannelPipelineFactory createMemcachedPipelineFactory(Cache cache, String memcachedVersion,
			boolean verbose, int idleTime, int receiveBufferSize, DefaultChannelGroup allChannels) {
		return new MemcachedPipelineFactory(cache, memcachedVersion, verbose, idleTime, receiveBufferSize, allChannels);
	}

	public void stop() {
		log.info("terminating daemon; closing all channels");

		ChannelGroupFuture future = allChannels.close();
		future.awaitUninterruptibly();
		if (!future.isCompleteSuccess()) {
			throw new RuntimeException("failure to complete closing all network channels");
		}
		log.info("channels closed, freeing cache storage");
		try {
			cache.close();
		} catch (IOException e) {
			throw new RuntimeException("exception while closing storage", e);
		}
		channelFactory.releaseExternalResources();

		running = false;
		log.info("successfully shut down");
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setIdleTime(int idleTime) {
		this.idleTime = idleTime;
	}

	public void setAddr(InetSocketAddress addr) {
		this.addr = addr;
	}

	public Cache<CACHE_ELEMENT> getCache() {
		return cache;
	}

	public void setCache(Cache<CACHE_ELEMENT> cache) {
		this.cache = cache;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isBinary() {
		return binary;
	}

	public void setBinary(boolean binary) {
		this.binary = binary;
	}
}
