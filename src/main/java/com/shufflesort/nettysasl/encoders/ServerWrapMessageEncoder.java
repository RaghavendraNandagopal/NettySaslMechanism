package com.shufflesort.nettysasl.encoders;

import javax.security.sasl.SaslException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.shufflesort.nettysasl.SaslNettyServer;
import com.shufflesort.nettysasl.SaslNettyServerState;

public class ServerWrapMessageEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(final ChannelHandlerContext ctx, final Channel ch,
            final Object obj) throws Exception {
        System.out.println("Sasl Client Wrap Message Encoder");

        final Channel channel = ctx.getChannel();

        final SaslNettyServer saslNettyServer = SaslNettyServerState.getSaslNettyServer
                .get(channel);

        // If authentication is not completed yet and channel connection is not
        // established, then
        // return the object as it is.
        if (saslNettyServer == null) {
            System.out
                    .println("Wrapping Server Message encoder called, saslNettyServer is null hence returning the same object");
            return obj;
        }

        // If sasl authentication is not completed yet.
        if (saslNettyServer != null && !saslNettyServer.isComplete()) {
            System.out
                    .println("Wrapping Server Message encoder called, saslNettyServer is not authenticated at first place, hence returning the same object");
            return obj;
        }

        // If wrap functionality is not required, then send
        // the object as it is.
        if (saslNettyServer != null && !saslNettyServer.isUseWrap()) {
            System.out
                    .println("Wrapping Server Message encoder called, saslNettyServer is not null but useWrap is false hence returning the same object");
            return obj;
        }

        System.out.println("Wrapping Server pay load message ");

        byte[] messagePayLoad = null;
        if (obj instanceof ChannelBuffer) {
            final ChannelBuffer buf = (ChannelBuffer) obj;
            final int length = buf.readableBytes();
            messagePayLoad = new byte[length];
            buf.markReaderIndex();
            System.out.println("Server Message PayLoad Length: " + length);
            buf.readBytes(messagePayLoad);
        }

        byte[] wrappedPayLoad = null;

        try {
            wrappedPayLoad = saslNettyServer.wrap(messagePayLoad, 0,
                    messagePayLoad.length);
        } catch (final SaslException se) {
            try {
                saslNettyServer.dispose();
            } catch (final SaslException igonored) {

            }
            throw se;
        }

        ChannelBufferOutputStream bout = null;

        if (wrappedPayLoad != null) {
            bout = new ChannelBufferOutputStream(
                    ChannelBuffers.directBuffer(wrappedPayLoad.length + 4));
            bout.writeInt(wrappedPayLoad.length);
            if (wrappedPayLoad.length > 0) {
                bout.write(wrappedPayLoad);
            }
            bout.close();
            wrappedPayLoad = null;
        }
        System.out.println("Server Wrapped the message successfully");
        return bout.buffer();
    }

}
