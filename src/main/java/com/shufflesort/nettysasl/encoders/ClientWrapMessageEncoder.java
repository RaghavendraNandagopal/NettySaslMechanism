package com.shufflesort.nettysasl.encoders;

import javax.security.sasl.SaslException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufflesort.nettysasl.SaslNettyClient;
import com.shufflesort.nettysasl.SaslNettyClientState;
import com.shufflesort.nettysasl.decoders.ClientUnwrapMessageDecoder;

public class ClientWrapMessageEncoder extends OneToOneEncoder {

	private static final Logger logger = LoggerFactory
			.getLogger(ClientWrapMessageEncoder.class);

	@Override
	protected Object encode(final ChannelHandlerContext ctx, final Channel ch,
			final Object obj) throws Exception {
		logger.debug("Sasl Client Wrap Message Encoder");

		final Channel channel = ctx.getChannel();

		final SaslNettyClient saslNettyClient = SaslNettyClientState.getSaslNettyClient
				.get(channel);

		// If authentication is not completed yet and channel connection is not
		// established, then
		// return the object as it is.
		if (saslNettyClient == null) {
			logger.debug("Wrapping Client Message encoder called, saslNettyClient is null hence returning the same object");
			return obj;
		}

		// If sasl authentication is not completed yet.
		if (saslNettyClient != null && !saslNettyClient.isComplete()) {
			logger.debug("Wrapping Client Message encoder called, saslNettyClient is not authenticated at first place, hence returning the same object");
			return obj;
		}

		// If wrap functionality is not required, then send
		// the object as it is.
		if (saslNettyClient != null && !saslNettyClient.isUseWrap()) {
			logger.debug("Wrapping Client Message encoder called, saslNettyClient is not null but useWrap is false hence returning the same object");
			return obj;
		}

		logger.debug("Wrapping Client pay load message ");

		byte[] messagePayLoad = null;
		if (obj instanceof ChannelBuffer) {
			final ChannelBuffer buf = (ChannelBuffer) obj;
			final int length = buf.readableBytes();
			messagePayLoad = new byte[length];
			buf.markReaderIndex();
			logger.debug("Client Message PayLoad Length: " + length);
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
		logger.debug("Client Wrapped the message successfully");
		return bout.buffer();
	}
}
