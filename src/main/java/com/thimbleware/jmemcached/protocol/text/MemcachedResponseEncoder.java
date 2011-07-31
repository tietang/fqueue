package com.thimbleware.jmemcached.protocol.text;

import static com.thimbleware.jmemcached.protocol.text.MemcachedPipelineFactory.USASCII;
import static java.lang.String.valueOf;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thimbleware.jmemcached.Cache;
import com.thimbleware.jmemcached.CacheElement;
import com.thimbleware.jmemcached.StatsCounter;
import com.thimbleware.jmemcached.protocol.Command;
import com.thimbleware.jmemcached.protocol.ResponseMessage;
import com.thimbleware.jmemcached.protocol.exceptions.ClientException;

/**
 * Response encoder for the memcached text protocol. Produces strings destined
 * for the StringEncoder
 */
public final class MemcachedResponseEncoder<CACHE_ELEMENT extends CacheElement> extends SimpleChannelUpstreamHandler {

	final Logger logger = LoggerFactory.getLogger(MemcachedResponseEncoder.class);

	public static final ChannelBuffer CRLF = ChannelBuffers.copiedBuffer("\r\n", USASCII);
	private static final ChannelBuffer VALUE = ChannelBuffers.copiedBuffer("VALUE ", USASCII);
	private static final ChannelBuffer EXISTS = ChannelBuffers.copiedBuffer("EXISTS\r\n", USASCII);
	private static final ChannelBuffer NOT_FOUND = ChannelBuffers.copiedBuffer("NOT_FOUND\r\n", USASCII);
	private static final ChannelBuffer NOT_STORED = ChannelBuffers.copiedBuffer("NOT_STORED\r\n", USASCII);
	private static final ChannelBuffer STORED = ChannelBuffers.copiedBuffer("STORED\r\n", USASCII);
	private static final ChannelBuffer DELETED = ChannelBuffers.copiedBuffer("DELETED\r\n", USASCII);
	private static final ChannelBuffer END = ChannelBuffers.copiedBuffer("END\r\n", USASCII);
	private static final ChannelBuffer OK = ChannelBuffers.copiedBuffer("OK\r\n", USASCII);
	private static final ChannelBuffer ERROR = ChannelBuffers.copiedBuffer("ERROR\r\n", USASCII);
	private static final ChannelBuffer CLIENT_ERROR = ChannelBuffers.copiedBuffer("CLIENT_ERROR", USASCII);
	
	/**
	 * Handle exceptions in protocol processing. Exceptions are either client or
	 * internal errors. Report accordingly.
	 * 
	 * @param ctx
	 * @param e
	 * @throws Exception
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		try {
			throw e.getCause();
		} catch (ClientException ce) {
			if (ctx.getChannel().isOpen())
				ctx.getChannel().write(ChannelBuffers.wrappedBuffer(CLIENT_ERROR.array(), ce.getMessage().getBytes(), CRLF.array()));
		} catch (ClosedChannelException e2) {
			logger.info("ClosedChannelException" + e.getChannel().getRemoteAddress());
			if (ctx.getChannel().isOpen()) {
				ctx.getChannel().write(ERROR);
			}
		} catch (IOException e2) {
			StackTraceElement[] stackTraceElements = e2.getStackTrace();
			for (int i = 0; i < stackTraceElements.length; i++) {
				if (stackTraceElements[i].getClassName().equals("sun.nio.ch.SocketDispatcher")) {
					logger.info("IOException:" + e.getChannel().getRemoteAddress());
					if (ctx.getChannel().isOpen()) {
						ctx.getChannel().write(ERROR);
					}
					return;
				}
			}
			logger.error("error", e2);

		} catch (Throwable tr) {
			logger.error("error", tr);
			if (ctx.getChannel().isOpen()) {
				ctx.getChannel().write(ERROR);
			}
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext channelHandlerContext, MessageEvent messageEvent)
			throws Exception {
		ResponseMessage<CACHE_ELEMENT> command = (ResponseMessage<CACHE_ELEMENT>) messageEvent.getMessage();

		Command cmd = command.cmd.cmd;

		Channel channel = messageEvent.getChannel();

		if (cmd == Command.GET || cmd == Command.GETS) {
			CacheElement[] results = command.elements;
			int totalBytes = 0;
			for (CacheElement result : results) {
				if (result != null) {
					totalBytes += result.size() + 256;
				}
			}
			ChannelBuffer writeBuffer = ChannelBuffers.dynamicBuffer(totalBytes);

			for (CacheElement result : results) {
				if (result != null) {
					writeBuffer.writeBytes(VALUE.duplicate());
					writeBuffer.writeBytes(ChannelBuffers.copiedBuffer(result.getKeystring(), USASCII));
					writeBuffer.writeByte((byte) ' ');
					writeBuffer.writeBytes(ChannelBuffers.copiedBuffer(String.valueOf(result.getFlags()), USASCII));
					writeBuffer.writeByte((byte) ' ');
					writeBuffer.writeBytes(ChannelBuffers
							.copiedBuffer(String.valueOf(result.getData().length), USASCII));
					if (cmd == Command.GETS) {
						writeBuffer.writeByte((byte) ' ');
						writeBuffer.writeBytes(ChannelBuffers.copiedBuffer(String.valueOf(result.getCasUnique()),
								USASCII));
					}
					writeBuffer.writeByte((byte) '\r');
					writeBuffer.writeByte((byte) '\n');
					writeBuffer.writeBytes(result.getData());
					writeBuffer.writeByte((byte) '\r');
					writeBuffer.writeByte((byte) '\n');
				}
			}
			writeBuffer.writeBytes(END.duplicate());
			StatsCounter.bytes_read.addAndGet(writeBuffer.writerIndex());
			Channels.write(channel, writeBuffer);
		} else if (cmd == Command.SET || cmd == Command.CAS || cmd == Command.ADD || cmd == Command.REPLACE
				|| cmd == Command.APPEND || cmd == Command.PREPEND) {

			if (!command.cmd.noreply)
				Channels.write(channel, storeResponse(command.response));
		} else if (cmd == Command.INCR || cmd == Command.DECR) {
			if (!command.cmd.noreply)
				Channels.write(channel, incrDecrResponseString(command.incrDecrResponse));

		} else if (cmd == Command.DELETE) {
			if (!command.cmd.noreply)
				Channels.write(channel, deleteResponseString(command.deleteResponse));

		} else if (cmd == Command.STATS) {
			for (Map.Entry<String, Set<String>> stat : command.stats.entrySet()) {
				for (String statVal : stat.getValue()) {
					StringBuilder builder = new StringBuilder();
					builder.append("STAT ");
					builder.append(stat.getKey());
					builder.append(" ");
					builder.append(String.valueOf(statVal));
					builder.append("\r\n");
					Channels.write(channel, ChannelBuffers.copiedBuffer(builder.toString(), USASCII));
				}
			}
			Channels.write(channel, END.duplicate());

		} else if (cmd == Command.VERSION) {
			Channels.write(channel, ChannelBuffers.copiedBuffer("VERSION " + command.version + "\r\n", USASCII));
		} else if (cmd == Command.QUIT) {
			Channels.disconnect(channel);
		} else if (cmd == Command.FLUSH_ALL) {
			if (!command.cmd.noreply) {
				ChannelBuffer ret = command.flushSuccess ? OK.duplicate() : ERROR.duplicate();

				Channels.write(channel, ret);
			}
		} else {
			Channels.write(channel, ERROR.duplicate());
			logger.error("error; unrecognized command: " + cmd);
		}

	}

	private ChannelBuffer deleteResponseString(Cache.DeleteResponse deleteResponse) {
		if (deleteResponse == Cache.DeleteResponse.DELETED) {
			StatsCounter.bytes_read.addAndGet(9);
			return DELETED.duplicate();
		} else {
			StatsCounter.bytes_read.addAndGet(11);
			return NOT_FOUND.duplicate();
		}
	}

	private ChannelBuffer incrDecrResponseString(Integer ret) {
		if (ret == null) {
			StatsCounter.bytes_read.addAndGet(11);
			return NOT_FOUND.duplicate();
		} else {
			StatsCounter.bytes_read.addAndGet(11);
			ChannelBuffer buffer = ChannelBuffers.copiedBuffer(valueOf(ret) + "\r\n", USASCII);
			StatsCounter.bytes_read.addAndGet(buffer.writerIndex());
			return buffer;
		}
	}

	/**
	 * Find the string response message which is equivalent to a response to a
	 * set/add/replace message in the cache
	 * 
	 * @param storeResponse
	 *            the response code
	 * @return the string to output on the network
	 */
	private ChannelBuffer storeResponse(Cache.StoreResponse storeResponse) {
		switch (storeResponse) {
		case EXISTS:
			StatsCounter.bytes_read.addAndGet(8);
			return EXISTS.duplicate();
		case NOT_FOUND:
			StatsCounter.bytes_read.addAndGet(11);
			return NOT_FOUND.duplicate();
		case NOT_STORED:
			StatsCounter.bytes_read.addAndGet(12);
			return NOT_STORED.duplicate();
		case STORED:
			StatsCounter.bytes_read.addAndGet(8);
			return STORED.duplicate();
		}
		throw new RuntimeException("unknown store response from cache: " + storeResponse);
	}
}
