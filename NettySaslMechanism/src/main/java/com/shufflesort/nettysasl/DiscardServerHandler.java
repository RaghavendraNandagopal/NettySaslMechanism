package com.shufflesort.nettysasl;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class DiscardServerHandler extends SimpleChannelHandler {

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final ExceptionEvent e) {
        e.getCause().printStackTrace();

        final Channel ch = e.getChannel();
        ch.close();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx,
            final MessageEvent e) {
        /*
         * final ChannelBuffer buf = (ChannelBuffer) e.getMessage(); while
         * (buf.readable()) { System.out.println((char) buf.readByte());
         * System.out.flush(); }
         */
        ctx.getChannel().write(e.getMessage());

    }

}
