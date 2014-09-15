package com.shufflesort.nettysasl.handlers;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufflesort.nettysasl.SaslNettyServer;
import com.shufflesort.nettysasl.SaslNettyServerState;

/**
 * Authorize or deny client requests based on existence and completeness of
 * client's SASL authentication.
 */
public class SaslStormServerAuthorizeHandler extends
		SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(SaslStormServerAuthorizeHandler.class);

	/**
	 * Constructor.
	 */
	public SaslStormServerAuthorizeHandler() {
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx,
			final MessageEvent e) {
		final Object msg = e.getMessage();
		if (msg == null) {
			return;
		}

		final Channel channel = ctx.getChannel();
		logger.debug("messageReceived: Checking whether the client is authorized to send messages to the server ");

		// Authorize: client is allowed to doRequest() if and only if the client
		// has successfully authenticated with this server.
		final SaslNettyServer saslNettyServer = SaslNettyServerState.getSaslNettyServer
				.get(channel);

		if (saslNettyServer == null) {
			logger.error("messageReceived: This client is *NOT* authorized to perform "
					+ "this action since there's no saslNettyServer to "
					+ "authenticate the client: "
					+ "refusing to perform requested action: " + msg);
			return;
		}

		if (!saslNettyServer.isComplete()) {
			logger.error("messageReceived: This client is *NOT* authorized to perform "
					+ "this action because SASL authentication did not complete: "
					+ "refusing to perform requested action: " + msg);
			// Return now *WITHOUT* sending upstream here, since client
			// not authorized.
			return;
		}

		logger.debug("messageReceived: authenticated client: "
				+ saslNettyServer.getUserName()
				+ " is authorized to do request " + "on server.");

		// We call fireMessageReceived since the client is allowed to perform
		// this request. The client's request will now proceed to the next
		// pipeline component.
		Channels.fireMessageReceived(ctx, msg);
	}
}