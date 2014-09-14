package com.shufflesort.nettysasl.model;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;

public enum ControlMessage {
    CLOSE_MESSAGE((short) -100), EOB_MESSAGE((short) -201), FAILURE_RESPONSE(
            (short) -400), OK_RESPONSE((short) -200), SASL_COMPLETE_REQUEST(
            (short) -203), SASL_TOKEN_MESSAGE((short) -500), SASL_TOKEN_MESSAGE_REQUEST(
            (short) -202), UNIX_TIME_MESSAGE((short) -204);

    /**
     * Return a control message per an encoded status code
     * 
     * @param encoded
     * @return
     */
    public static ControlMessage mkMessage(final short encoded) {
        for (final ControlMessage cm : ControlMessage.values()) {
            if (encoded == cm.code) {
                return cm;
            }
        }
        return null;
    }

    private short code;

    // private constructor
    private ControlMessage(final short code) {
        this.code = code;
    }

    /**
     * encode the current Control Message into a channel buffer
     * 
     * @throws Exception
     */
    public ChannelBuffer buffer() throws IOException {
        final ChannelBufferOutputStream bout = new ChannelBufferOutputStream(
                ChannelBuffers.directBuffer(encodeLength()));
        write(bout);
        bout.close();
        return bout.buffer();
    }

    int encodeLength() {
        return 2; // short
    }

    void write(final ChannelBufferOutputStream bout) throws IOException {
        bout.writeShort(code);
    }

}
