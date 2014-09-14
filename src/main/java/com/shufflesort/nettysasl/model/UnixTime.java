package com.shufflesort.nettysasl.model;

import java.io.IOException;
import java.util.Date;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;

public class UnixTime {

    private final int value;

    public UnixTime(final int value) {
        this.value = value;
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
        return 2 + 4; // short + int
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return new Date(value * 1000L).toString();
    }

    void write(final ChannelBufferOutputStream bout) throws IOException {
        // bout.writeShort(ControlMessage.UNIX_TIME_MESSAGE.buffer().readShort());
        bout.writeShort((short) -204);
        bout.writeInt(value);
    }
}
