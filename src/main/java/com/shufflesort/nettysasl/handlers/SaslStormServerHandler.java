package com.shufflesort.nettysasl.handlers;

import java.io.IOException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufflesort.nettysasl.SaslNettyServer;
import com.shufflesort.nettysasl.SaslNettyServerState;
import com.shufflesort.nettysasl.model.ControlMessage;
import com.shufflesort.nettysasl.model.SaslMessageToken;
import com.shufflesort.nettysasl.util.SaslUtils;

public class SaslStormServerHandler extends SimpleChannelHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(SaslStormServerHandler.class);

	/** Used for client or server's token to send or receive from each other. */
	private byte[] token;

	private String userName;

	public SaslStormServerHandler() throws IOException {
		getSASLCredentials();
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx,
			final ExceptionEvent e) {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}

	private void getSASLCredentials() throws IOException {
		userName = SaslUtils.getUserName();
		final String secretKey = SaslUtils.getSecretKey();
		if (secretKey != null) {
			token = secretKey.getBytes();
		}
		logger.debug("(Server Side) SASL credentials for storm topology "
				+ userName + " is " + secretKey);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx,
			final MessageEvent e) throws Exception {
		final Object msg = e.getMessage();
		if (msg == null) {
			return;
		}

		final Channel channel = ctx.getChannel();

		if (msg instanceof ControlMessage
				&& (ControlMessage) e.getMessage() == ControlMessage.SASL_TOKEN_MESSAGE_REQUEST) {
			// initialize server-side SASL functionality, if we haven't yet
			// (in which case we are looking at the first SASL message from the
			// client).
			SaslNettyServer saslNettyServer = SaslNettyServerState.getSaslNettyServer
					.get(channel);
			if (saslNettyServer == null) {
				logger.debug("No saslNettyServer for " + channel
						+ " yet; creating now, with topology token: ");
				try {
					saslNettyServer = new SaslNettyServer(userName, token);
				} catch (final IOException ioe) {
					logger.error("Error occurred while creating saslNettyServer on server "
							+ channel.getLocalAddress()
							+ " for client "
							+ channel.getRemoteAddress());
					saslNettyServer = null;
				}

				SaslNettyServerState.getSaslNettyServer.set(channel,
						saslNettyServer);

			} else {
				logger.debug("Found existing saslNettyServer on server:"
						+ channel.getLocalAddress() + " for client "
						+ channel.getRemoteAddress());
			}

			logger.debug("processToken:  With nettyServer: " + saslNettyServer
					+ " and token length: " + token.length);

			SaslMessageToken saslTokenMessageRequest = null;
			saslTokenMessageRequest = new SaslMessageToken(
					saslNettyServer.response(new byte[0]));
			// Send response to client.
			channel.write(saslTokenMessageRequest);
			// do not send upstream to other handlers: no further action needs
			// to be done for SASL_TOKEN_MESSAGE_REQUEST requests.
			return;
		}

		if (msg instanceof SaslMessageToken) {
			// initialize server-side SASL functionality, if we haven't yet
			// (in which case we are looking at the first SASL message from the
			// client).
			final SaslNettyServer saslNettyServer = SaslNettyServerState.getSaslNettyServer
					.get(channel);
			if (saslNettyServer == null) {
				if (saslNettyServer == null) {
					throw new Exception("saslNettyServer was unexpectedly "
							+ "null for channel: " + channel);
				}
			}
			final SaslMessageToken saslTokenMessageRequest = new SaslMessageToken(
					saslNettyServer.response(((SaslMessageToken) msg)
							.getSaslToken()));

			// Send response to client.
			channel.write(saslTokenMessageRequest);

			if (saslNettyServer.isComplete()) {
				// If authentication of client is complete, we will also send a
				// SASL-Complete message to the client.
				logger.debug("SASL authentication is complete for client with "
						+ "username: " + saslNettyServer.getUserName());
				channel.write(ControlMessage.SASL_COMPLETE_REQUEST);
				logger.debug("Removing SaslServerHandler from pipeline since SASL "
						+ "authentication is complete.");

				SaslUtils.checkSaslNegotiatedProtection(saslNettyServer);
				saslNettyServer.setUseWrap();

				ctx.getPipeline().remove(this);
			}
			return;
		} else {
			// Client should not be sending other-than-SASL messages before
			// SaslServerHandler has removed itself from the pipeline. Such
			// non-SASL requests will be denied by the Authorize channel handler
			// (the next handler upstream in the server pipeline) if SASL
			// authentication has not completed.
			logger.debug("Sending upstream an unexpected non-SASL message :  "
					+ msg);
			Channels.fireMessageReceived(ctx, msg);
		}
	}
}
