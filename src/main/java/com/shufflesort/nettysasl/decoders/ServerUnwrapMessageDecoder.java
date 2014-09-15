package com.shufflesort.nettysasl.decoders;

import javax.security.sasl.SaslException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufflesort.nettysasl.SaslNettyServer;
import com.shufflesort.nettysasl.SaslNettyServerState;
import com.shufflesort.nettysasl.model.ControlMessage;
import com.shufflesort.nettysasl.model.SaslMessageToken;
import com.shufflesort.nettysasl.model.UnixTime;

public class ServerUnwrapMessageDecoder extends FrameDecoder {

	private static final Logger logger = LoggerFactory
			.getLogger(ServerUnwrapMessageDecoder.class);

	@Override
	protected Object decode(final ChannelHandlerContext ctx, final Channel ch,
			ChannelBuffer buf) throws Exception {

		final SaslNettyServer saslNettyServer = SaslNettyServerState.getSaslNettyServer
				.get(ch);
		final boolean isUnwrap = isUseUnwrap(saslNettyServer);
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
					unWrappedPayLoad = saslNettyServer.unwrap(
							buf.readBytes(wrappedPayLoadLength).array(), 0,
							wrappedPayLoadLength);
				} catch (final SaslException se) {
					try {
						saslNettyServer.dispose();
					} catch (final SaslException ignored) {
					}
					throw se;
				}

				buf = ChannelBuffers.dynamicBuffer();
				buf.writeBytes(unWrappedPayLoad);
				logger.debug("Server Unwrapped the message successfully");
			}
		}

		logger.debug("Server MessageDecoder called");

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

	private boolean isUseUnwrap(final SaslNettyServer saslNettyServer) {
		// If authentication is not completed yet and channel connection is not
		// established, then return the object as it is.
		if (saslNettyServer == null) {
			logger.debug("Unwrapping Server Message decoder called, saslNettyServer is null hence returning the same object");
			return false;
		}

		// If sasl authentication is not completed yet.
		if (saslNettyServer != null && !saslNettyServer.isComplete()) {
			logger.debug("Unwrapping Server Message decoder called, saslNettyServer is not authenticated at first place, hence returning the same object");
			return false;
		}

		// If wrap functionality is not required, then send
		// the object as it is.
		if (saslNettyServer != null && !saslNettyServer.isUseWrap()) {
			logger.debug("Unwrapping Server Message decoder called, saslNettyServer is not null but useWrap is false hence returning the same object");
			return false;
		}

		logger.debug("Unwrapping Server pay load message ");
		return true;
	}

}
