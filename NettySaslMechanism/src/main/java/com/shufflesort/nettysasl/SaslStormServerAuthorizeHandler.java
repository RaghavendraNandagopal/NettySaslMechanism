package com.shufflesort.nettysasl;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Authorize or deny client requests based on existence and completeness of
 * client's SASL authentication.
 */
public class SaslStormServerAuthorizeHandler extends
        SimpleChannelUpstreamHandler {

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
        System.out
                .println("messageReceived: Checking whether the client is authorized to send messages to the server ");

        // Authorize: client is allowed to doRequest() if and only if the client
        // has successfully authenticated with this server.
        final SaslNettyServer saslNettyServer = SaslNettyServerState.getSaslNettyServer
                .get(channel);

        if (saslNettyServer == null) {
            System.err
                    .println("messageReceived: This client is *NOT* authorized to perform "
                            + "this action since there's no saslNettyServer to "
                            + "authenticate the client: "
                            + "refusing to perform requested action: " + msg);
            return;
        }

        if (!saslNettyServer.isComplete()) {
            System.err
                    .println("messageReceived: This client is *NOT* authorized to perform "
                            + "this action because SASL authentication did not complete: "
                            + "refusing to perform requested action: " + msg);
            // Return now *WITHOUT* sending upstream here, since client
            // not authorized.
            return;
        }

        System.out.println("messageReceived: authenticated client: "
                + saslNettyServer.getUserName()
                + " is authorized to do request " + "on server.");

        // We call fireMessageReceived since the client is allowed to perform
        // this request. The client's request will now proceed to the next
        // pipeline component.
        Channels.fireMessageReceived(ctx, msg);
    }
}