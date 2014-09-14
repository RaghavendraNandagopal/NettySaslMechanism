package com.shufflesort.nettysasl.decoders;

import javax.security.sasl.SaslException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.shufflesort.nettysasl.SaslNettyClient;
import com.shufflesort.nettysasl.SaslNettyClientState;
import com.shufflesort.nettysasl.model.ControlMessage;
import com.shufflesort.nettysasl.model.SaslMessageToken;
import com.shufflesort.nettysasl.model.UnixTime;

public class ClientUnwrapMessageDecoder extends FrameDecoder {

    @Override
    protected Object decode(final ChannelHandlerContext ctx, final Channel ch,
            ChannelBuffer buf) throws Exception {

        final SaslNettyClient saslNettyClient = SaslNettyClientState.getSaslNettyClient
                .get(ch);
        final boolean isUnwrap = isUseUnwrap(saslNettyClient);
        if (isUnwrap) {
            // Make sure that we have received at least a int
            long available = buf.readableBytes();
            if (available < 4) {
                // need more data
                return null;
            }

            if (available >= 4) {
                buf.markReaderIndex();

                final int wrappedPayLoadLength = buf.readInt();
                available -= 4;

                if (wrappedPayLoadLength <= 0) {
                    return buf;
                }

                if (available < wrappedPayLoadLength) {
                    // The whole bytes were not received yet - return null
                    buf.resetReaderIndex();
                    return null;
                }

                byte[] unWrappedPayLoad = null;
                try {
                    unWrappedPayLoad = saslNettyClient.unwrap(
                            buf.readBytes(wrappedPayLoadLength).array(), 0,
                            wrappedPayLoadLength);
                } catch (final SaslException se) {
                    try {
                        saslNettyClient.dispose();
                    } catch (final SaslException ignored) {
                    }
                    throw se;
                }

                buf = ChannelBuffers.dynamicBuffer();
                buf.writeBytes(unWrappedPayLoad);
                System.out.println("Client Unwrapped the message successfully");
            }
        }

        System.out.println("Client MessageDecoder called");

        // Make sure that we have received at least a short
        long available = buf.readableBytes();
        if (available < 2) {
            // need more data
            return null;
        }

        // Use while loop, try to decode as more messages as possible in single
        // call
        while (available >= 2) {

            // Mark the current buffer position before reading task/len field
            // because the whole frame might not be in the buffer yet.
            // We will reset the buffer position to the marked position if
            // there's not enough bytes in the buffer.
            buf.markReaderIndex();

            // read the short field
            final short code = buf.readShort();
            available -= 2;

            // Prepare Control message
            final ControlMessage ctrl_msg = ControlMessage.mkMessage(code);
            if (ctrl_msg == null) {
                return null;
            }

            // case 1: Unix Time
            if (ctrl_msg == ControlMessage.UNIX_TIME_MESSAGE) {
                // Make sure that we have received at least an integer (length)
                if (available < 4) {
                    // need more data
                    buf.resetReaderIndex();
                    return null;
                }

                return new UnixTime(buf.readInt());
            }

            // case 2: Sasl Token Message Request or Sasl Complete Message
            // Request
            if (ctrl_msg == ControlMessage.SASL_TOKEN_MESSAGE_REQUEST
                    || ctrl_msg == ControlMessage.SASL_COMPLETE_REQUEST) {
                return ctrl_msg;
            }

            // case 3: Sasl Message Token
            if (ctrl_msg == ControlMessage.SASL_TOKEN_MESSAGE) {
                // Make sure that we have received at least an integer (length)
                if (available < 4) {
                    // need more data
                    buf.resetReaderIndex();
                    return null;
                }
                final int payLoadLength = buf.readInt();
                available -= 4;
                if (payLoadLength <= 0) {
                    return new SaslMessageToken(null);
                }
                if (available < payLoadLength) {
                    // The whole bytes were not received yet - return null
                    buf.resetReaderIndex();
                    return null;
                }
                return new SaslMessageToken(buf.readBytes(payLoadLength)
                        .array());
            }

        }

        return null;
    }

    private boolean isUseUnwrap(final SaslNettyClient saslNettyClient) {
        // If authentication is not completed yet and channel connection is not
        // established, then return the object as it is.
        if (saslNettyClient == null) {
            System.out
                    .println("Unwrapping Client Message decoder called, saslNettyClient is null hence returning the same object");
            return false;
        }

        // If sasl authentication is not completed yet.
        if (saslNettyClient != null && !saslNettyClient.isComplete()) {
            System.out
                    .println("Unwrapping Client Message decoder called, saslNettyClient is not authenticated at first place, hence returning the same object");
            return false;
        }

        // If wrap functionality is not required, then send
        // the object as it is.
        if (saslNettyClient != null && !saslNettyClient.isUseWrap()) {
            System.out
                    .println("Unwrapping Client Message decoder called, saslNettyClient is not null but useWrap is false hence returning the same object");
            return false;
        }

        System.out.println("Unwrapping Client pay load message ");
        return true;
    }

}
