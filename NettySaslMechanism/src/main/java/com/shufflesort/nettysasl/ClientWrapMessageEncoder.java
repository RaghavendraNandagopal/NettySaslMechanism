package com.shufflesort.nettysasl;

import javax.security.sasl.SaslException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

public class ClientWrapMessageEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(final ChannelHandlerContext ctx, final Channel ch,
            final Object obj) throws Exception {
        System.out.println("Sasl Client Wrap Message Encoder");

        final Channel channel = ctx.getChannel();

        final SaslNettyClient saslNettyClient = SaslNettyClientState.getSaslNettyClient
                .get(channel);

        // If authentication is not completed yet and channel connection is not
        // established, then
        // return the object as it is.
        if (saslNettyClient == null) {
            System.out
                    .println("Wrapping Client Message encoder called, saslNettyClient is null hence returning the same object");
            return obj;
        }

        // If sasl authentication is not completed yet.
        if (saslNettyClient != null && !saslNettyClient.isComplete()) {
            System.out
                    .println("Wrapping Client Message encoder called, saslNettyClient is not authenticated at first place, hence returning the same object");
            return obj;
        }

        // If wrap functionality is not required, then send
        // the object as it is.
        if (saslNettyClient != null && !saslNettyClient.isUseWrap()) {
            System.out
                    .println("Wrapping Client Message encoder called, saslNettyClient is not null but useWrap is false hence returning the same object");
            return obj;
        }

        System.out.println("Wrapping Client pay load message ");

        byte[] messagePayLoad = null;
        if (obj instanceof ChannelBuffer) {
            final ChannelBuffer buf = (ChannelBuffer) obj;
            final int length = buf.readableBytes();
            messagePayLoad = new byte[length];
            buf.markReaderIndex();
            System.out.println("Client Message PayLoad Length: " + length);
            buf.readBytes(messagePayLoad);
        }

        byte[] wrappedPayLoad = null;

        try {
            wrappedPayLoad = saslNettyClient.wrap(messagePayLoad, 0,
                    messagePayLoad.length);
        } catch (final SaslException se) {
            try {
                saslNettyClient.dispose();
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
        System.out.println("Client Wrapped the message successfully");
        return bout.buffer();
    }
}
