package com.shufflesort.nettysasl;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.shufflesort.nettysasl.model.UnixTime;

public class TimeServerHandler extends SimpleChannelHandler {

    /*
     * public void channelConnected(final ChannelHandlerContext ctx, final
     * ChannelStateEvent e) { final Channel ch = e.getChannel();
     * 
     * System.out.println("Connected to Server from" +
     * ctx.getChannel().getRemoteAddress());
     * 
     * final UnixTime unixTime = new UnixTime( (int) (System.currentTimeMillis()
     * / 1000L + 2208988800L));
     * 
     * 
     * final ChannelBuffer time = ChannelBuffers.buffer(4); time.writeInt((int)
     * (System.currentTimeMillis() / 1000L + 2208988800L));
     * 
     * final ChannelFuture f = ch.write(time);
     * 
     * final ChannelFuture f = ch.write(unixTime);
     * 
     * f.addListener(new ChannelFutureListener() {
     * 
     * @Override public void operationComplete(final ChannelFuture future) {
     * final Channel ch = future.getChannel(); ch.close(); } }); }
     */

    @Override
    public void channelConnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        System.out.println("Connected from "
                + ctx.getChannel().getRemoteAddress() + " to server "
                + ctx.getChannel().getLocalAddress());
    }

    @Override
    public void channelOpen(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        TimeServer.allChannels.add(e.getChannel());
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

        System.out.println("Client Unix Time: " + unixTime);

        final UnixTime serverUnixTime = new UnixTime(
                (int) (System.currentTimeMillis() / 1000L + 2208988800L));

        e.getChannel().write(serverUnixTime);

    }

}
