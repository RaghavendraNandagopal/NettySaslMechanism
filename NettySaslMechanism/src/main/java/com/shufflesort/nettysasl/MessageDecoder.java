package com.shufflesort.nettysasl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class MessageDecoder extends FrameDecoder {

    /*
     * Each ControlMessage is encoded as: code (<0) ... short(2) Each
     * TaskMessage is encoded as: task (>=0) ... short(2) len ... int(4) payload
     * ... byte[] *
     */
    @Override
    protected Object decode(final ChannelHandlerContext ctx,
            final Channel channel, final ChannelBuffer buf) throws Exception {

        System.out.println("sasl MessageDecoder called");

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
}