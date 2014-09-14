package com.shufflesort.nettysasl.encoders;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.shufflesort.nettysasl.model.UnixTime;

public class TimeEncoder extends SimpleChannelHandler {

    @Override
    public void writeRequested(final ChannelHandlerContext ctx,
            final MessageEvent e) {

        ChannelBuffer buf = null;

        final UnixTime time = (UnixTime) e.getMessage();

        buf = ChannelBuffers.buffer(4);
        buf.writeInt(time.getValue());

        Channels.write(ctx, e.getFuture(), buf);

    }

}
