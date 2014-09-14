package com.shufflesort.nettysasl;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.shufflesort.nettysasl.model.UnixTime;

public class TimeClientHandler extends SimpleChannelHandler {

    // private final ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

    @Override
    public void channelConnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        final Channel ch = e.getChannel();

        System.out.println("Connected to Server "
                + ctx.getChannel().getRemoteAddress() + " from "
                + ctx.getChannel().getLocalAddress());

        final UnixTime unixTime = new UnixTime(
                (int) (System.currentTimeMillis() / 1000L + 2208988800L));

        final ChannelFuture f = ch.write(unixTime);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx,
            final MessageEvent e) {

        final UnixTime unixTime = (UnixTime) e.getMessage();
        System.out.println("Server Unix Time: " + unixTime);
        e.getChannel().close();

        /*
         * final ChannelBuffer buf = (ChannelBuffer) e.getMessage(); final long
         * currentTimeMillis = buf.readInt() * 1000L; System.out.println(new
         * Date(currentTimeMillis)); e.getChannel().close();
         */

        /*
         * final ChannelBuffer m = (ChannelBuffer) e.getMessage();
         * buf.writeBytes(m);
         * 
         * if (buf.readableBytes() >= 4) { final long currentTimeMillis =
         * buf.readInt() * 1000L; System.out.println(new
         * Date(currentTimeMillis)); e.getChannel().close(); }
         */
    }

}
