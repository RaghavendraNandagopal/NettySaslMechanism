package com.shufflesort.nettysasl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class TimeDecoder extends FrameDecoder {

    @Override
    protected Object decode(final ChannelHandlerContext ctx,
            final Channel channel, final ChannelBuffer buf) throws Exception {
        if (buf.readableBytes() < 4) {
            return null;
        }

        return new UnixTime(buf.readInt());
    }
}
