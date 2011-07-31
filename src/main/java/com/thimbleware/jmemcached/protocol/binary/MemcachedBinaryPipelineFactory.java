package com.thimbleware.jmemcached.protocol.binary;

import com.thimbleware.jmemcached.Cache;
import com.thimbleware.jmemcached.protocol.MemcachedCommandHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.DefaultChannelGroup;


public class MemcachedBinaryPipelineFactory implements ChannelPipelineFactory {

    private Cache cache;
    private String version;
    private boolean verbose;
    private int idleTime;

    private DefaultChannelGroup channelGroup;

    private final MemcachedBinaryCommandDecoder decoder =  new MemcachedBinaryCommandDecoder();
    private final MemcachedCommandHandler memcachedCommandHandler;
    private final MemcachedBinaryResponseEncoder memcachedBinaryResponseEncoder = new MemcachedBinaryResponseEncoder();

    public MemcachedBinaryPipelineFactory(Cache cache, String version, boolean verbose, int idleTime, DefaultChannelGroup channelGroup) {
        this.cache = cache;
        this.version = version;
        this.verbose = verbose;
        this.idleTime = idleTime;
        this.channelGroup = channelGroup;
        memcachedCommandHandler = new MemcachedCommandHandler(this.cache, this.version, this.verbose, this.idleTime, this.channelGroup);
    }

    public ChannelPipeline getPipeline() throws Exception {
        return Channels.pipeline(
                decoder,
                memcachedCommandHandler,
                memcachedBinaryResponseEncoder
        );
    }
}
