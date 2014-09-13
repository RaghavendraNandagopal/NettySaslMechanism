package com.shufflesort.nettysasl;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

public class MessageEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(final ChannelHandlerContext ctx,
            final Channel channel, final Object obj) throws Exception {

        if (obj instanceof UnixTime) {
            System.out.println("sasl called inside UnixTime encoding");
            return ((UnixTime) obj).buffer();
        }

        if (obj instanceof ControlMessage) {
            System.out.println("sasl called inside ControlMessage encoding");
            return ((ControlMessage) obj).buffer();
        }

        if (obj instanceof SaslMessageToken) {
            System.out.println("sasl called inside SaslMessageToken encoding");
            return ((SaslMessageToken) obj).buffer();
        }

        throw new RuntimeException("Unsupported encoding of object of class "
                + obj.getClass().getName());
    }

}
