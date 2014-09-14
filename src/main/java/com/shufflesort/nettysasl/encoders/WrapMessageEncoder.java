package com.shufflesort.nettysasl.encoders;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import javax.security.sasl.SaslException;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.shufflesort.nettysasl.SaslNettyClient;
import com.shufflesort.nettysasl.SaslNettyServer;

public class WrapMessageEncoder extends OneToOneEncoder {

    SaslNettyClient saslNettyClient = null;
    SaslNettyServer saslNettyServer = null;

    public WrapMessageEncoder(final Object obj) {
        if (obj instanceof SaslNettyClient) {
            saslNettyClient = (SaslNettyClient) obj;
        }
        if (obj instanceof SaslNettyServer) {
            saslNettyServer = (SaslNettyServer) obj;
        }
    }

    private void disposeSasl() throws SaslException {
        if (saslNettyClient != null) {
            saslNettyClient.dispose();
        }
        if (saslNettyServer != null) {
            saslNettyServer.dispose();
        }
    }

    @Override
    protected Object encode(final ChannelHandlerContext ctx,
            final Channel channel, final Object obj) throws Exception {
        byte[] saslToken = null;

        System.out.println("sasl Wrapping Message encoder called");

        // If authentication is not completed yet and channel connection is not
        // established, then
        // return the object as it is.
        if (saslNettyClient == null && saslNettyServer == null) {
            System.out
                    .println("Wrapping Message encoder called, saslNettyClient and saslNettyServer are null hence returning the same object");
            return obj;
        }

        // If sasl authentication is not completed yet.
        if (saslNettyClient != null && !saslNettyClient.isComplete()) {
            System.out
                    .println("Wrapping Message encoder called, saslNettyClient is not authenticated at first place, hence returning the same object");
            return obj;
        }

        if (saslNettyServer != null && !saslNettyServer.isComplete()) {
            System.out
                    .println("Wrapping Message encoder called, saslNettyServer is not authenticated at first place, hence returning the same object");
            return obj;
        }

        // If wrap functionality is not required, then send
        // the object as it is.
        if (saslNettyClient != null && !saslNettyClient.isUseWrap()) {
            System.out
                    .println("Wrapping Message encoder called, saslNettyClient is not null but useWrap is false hence returning the same object");
            return obj;
        }

        if (saslNettyServer != null && !saslNettyServer.isUseWrap()) {
            System.out
                    .println("Wrapping Message encoder called, saslNettyServer is not null but useWrap is false hence returning the same object");
            return obj;
        }

        System.out.println("sasl Wrapping ");

        final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        final ObjectOutputStream objOutStream = new ObjectOutputStream(bytesOut);
        objOutStream.writeObject(obj);
        objOutStream.flush();
        final byte[] bytes = bytesOut.toByteArray();
        bytesOut.close();
        objOutStream.close();

        try {
            System.out.println("Wrapping the sasl pay load");

            if (saslNettyClient != null) {
                saslToken = saslNettyClient.wrap(bytes, 0, bytes.length);
            } else {
                saslToken = saslNettyServer.wrap(bytes, 0, bytes.length);
            }
        } catch (final SaslException se) {
            try {
                disposeSasl();
            } catch (final SaslException igonored) {

            }
            throw se;
        }

        ChannelBufferOutputStream bout = null;

        if (saslToken != null) {
            bout = new ChannelBufferOutputStream(
                    ChannelBuffers.directBuffer(saslToken.length + 4));
            bout.writeInt(saslToken.length);
            if (saslToken.length > 0) {
                bout.write(saslToken);
            }
            bout.close();
            saslToken = null;
        }
        return bout.buffer();
    }
}