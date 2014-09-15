package com.shufflesort.nettysasl.encoders;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufflesort.nettysasl.model.ControlMessage;
import com.shufflesort.nettysasl.model.SaslMessageToken;
import com.shufflesort.nettysasl.model.UnixTime;

public class MessageEncoder extends OneToOneEncoder {

	private static final Logger logger = LoggerFactory
			.getLogger(MessageEncoder.class);

	@Override
	protected Object encode(final ChannelHandlerContext ctx,
			final Channel channel, final Object obj) throws Exception {

		if (obj instanceof UnixTime) {
			logger.debug("sasl called inside UnixTime encoding");
			return ((UnixTime) obj).buffer();
		}

		if (obj instanceof ControlMessage) {
			logger.debug("sasl called inside ControlMessage encoding");
			return ((ControlMessage) obj).buffer();
		}

		if (obj instanceof SaslMessageToken) {
			logger.debug("sasl called inside SaslMessageToken encoding");
			return ((SaslMessageToken) obj).buffer();
		}

		throw new RuntimeException("Unsupported encoding of object of class "
				+ obj.getClass().getName());
	}

}
