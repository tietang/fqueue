package com.thimbleware.jmemcached.protocol.binary;

import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.CacheElement;
import com.thimbleware.jmemcached.protocol.Command;
import com.thimbleware.jmemcached.protocol.CommandMessage;
import com.thimbleware.jmemcached.protocol.exceptions.MalformedCommandException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.nio.ByteOrder;

/**
 */
@ChannelHandler.Sharable
public class MemcachedBinaryCommandDecoder extends FrameDecoder {

    public static final Charset USASCII = Charset.forName("US-ASCII");

    public static enum BinaryCommand {
        Get(0x00, Command.GET, false),
        Set(0x01, Command.SET, false),
        Add(0x02, Command.ADD, false),
        Replace(0x03, Command.REPLACE, false),
        Delete(0x04, Command.DELETE, false),
        Increment(0x05, Command.INCR, false),
        Decrement(0x06, Command.DECR, false),
        Quit(0x07, Command.QUIT, false),
        Flush(0x08, Command.FLUSH_ALL, false),
        GetQ(0x09, Command.GET, false),
        Noop(0x0A, null, false),
        Version(0x0B, Command.VERSION, false),
        GetK(0x0C, Command.GET, false, true),
        GetKQ(0x0D,Command.GET, true, true),
        Append(0x0E, Command.APPEND, false),
        Prepend(0x0F, Command.PREPEND, false),
        Stat(0x10, Command.STATS, false),
        SetQ(0x11, Command.SET, true),
        AddQ(0x12, Command.ADD, true),
        ReplaceQ(0x13, Command.REPLACE, true),
        DeleteQ(0x14, Command.DELETE, true),
        IncrementQ(0x15, Command.INCR, true),
        DecrementQ(0x16, Command.DECR, true),
        QuitQ(0x17, Command.QUIT, true),
        FlushQ(0x18, Command.FLUSH_ALL, true),
        AppendQ(0x19, Command.APPEND, true),
        PrependQ(0x1A, Command.PREPEND, true);

        public byte code;
        public Command correspondingCommand;
        public boolean noreply;
        public boolean addKeyToResponse = false;

        BinaryCommand(int code, Command correspondingCommand, boolean noreply) {
            this.code = (byte)code;
            this.correspondingCommand = correspondingCommand;
            this.noreply = noreply;
        }

        BinaryCommand(int code, Command correspondingCommand, boolean noreply, boolean addKeyToResponse) {
            this.code = (byte)code;
            this.correspondingCommand = correspondingCommand;
            this.noreply = noreply;
            this.addKeyToResponse = addKeyToResponse;
        }

        public static BinaryCommand forCommandMessage(CommandMessage msg) {
            for (BinaryCommand binaryCommand : values()) {
                if (binaryCommand.correspondingCommand == msg.cmd && binaryCommand.noreply == msg.noreply && binaryCommand.addKeyToResponse == msg.addKeyToResponse) {
                    return binaryCommand;
                }
            }

            return null;
        }

    }

    protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, ChannelBuffer channelBuffer) throws Exception {

        // need at least 24 bytes, to get header
        if (channelBuffer.readableBytes() < 24) return null;

        // get the header
        channelBuffer.markReaderIndex();
        ChannelBuffer headerBuffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, 24);
        channelBuffer.readBytes(headerBuffer);

        short magic = headerBuffer.readUnsignedByte();

        // magic should be 0x80
        if (magic != 0x80) {
            headerBuffer.resetReaderIndex();

            throw new MalformedCommandException("binary request payload is invalid, magic byte incorrect");
        }

        short opcode = headerBuffer.readUnsignedByte();
        short keyLength = headerBuffer.readShort();
        short extraLength = headerBuffer.readUnsignedByte();
        short dataType = headerBuffer.readUnsignedByte();   // unused
        short reserved = headerBuffer.readShort(); // unused
        int totalBodyLength = headerBuffer.readInt();
        int opaque = headerBuffer.readInt();
        long cas = headerBuffer.readLong();

        // we want the whole of totalBodyLength; otherwise, keep waiting.
        if (channelBuffer.readableBytes() < totalBodyLength) {
            channelBuffer.resetReaderIndex();
            return null;
        }

        // This assumes correct order in the enum. If that ever changes, we will have to scan for 'code' field.
        BinaryCommand bcmd = BinaryCommand.values()[opcode];

        Command cmdType = bcmd.correspondingCommand;
        CommandMessage cmdMessage = CommandMessage.command(cmdType);
        cmdMessage.noreply = bcmd.noreply;
        cmdMessage.cas_key = cas;
        cmdMessage.opaque = opaque;
        cmdMessage.addKeyToResponse = bcmd.addKeyToResponse;

        // get extras. could be empty.
        ChannelBuffer extrasBuffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, extraLength);
        channelBuffer.readBytes(extrasBuffer);

        // get the key if any
        if (keyLength != 0) {
            ChannelBuffer keyBuffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, keyLength);
            channelBuffer.readBytes(keyBuffer);

            ArrayList<String> keys = new ArrayList<String>();
            String key = keyBuffer.toString(USASCII);
            keys.add(key); // TODO this or UTF-8? ISO-8859-1?

            cmdMessage.keys = keys;


            if (cmdType == Command.ADD ||
                    cmdType == Command.SET ||
                    cmdType == Command.REPLACE ||
                    cmdType == Command.APPEND ||
                    cmdType == Command.PREPEND)
            {
                // TODO these are backwards from the spec, but seem to be what spymemcached demands -- which has the mistake?!
                short expire = (short) (extrasBuffer.capacity() != 0 ? extrasBuffer.readUnsignedShort() : 0);
                short flags = (short) (extrasBuffer.capacity() != 0 ? extrasBuffer.readUnsignedShort() : 0);

                // the remainder of the message -- that is, totalLength - (keyLength + extraLength) should be the payload
                int size = totalBodyLength - keyLength - extraLength;

                cmdMessage.element = new LocalCacheElement(key, flags, expire != 0 && expire < CacheElement.THIRTY_DAYS ? LocalCacheElement.Now() + expire : expire, 0L);
                cmdMessage.element.setData(new byte[size]);
                channelBuffer.readBytes(cmdMessage.element.getData(), 0, size);
            } else if (cmdType == Command.INCR || cmdType == Command.DECR) {
                long initialValue = extrasBuffer.readUnsignedInt();
                long amount = extrasBuffer.readUnsignedInt();
                long expiration = extrasBuffer.readUnsignedInt();

                cmdMessage.incrAmount = (int) amount;
                cmdMessage.incrDefault = (int) initialValue;
                cmdMessage.incrExpiry = (int) expiration;
            }
        }

        return cmdMessage;
    }
}
