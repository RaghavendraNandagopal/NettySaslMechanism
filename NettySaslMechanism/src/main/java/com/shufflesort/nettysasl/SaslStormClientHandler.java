package com.shufflesort.nettysasl;

import java.io.IOException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class SaslStormClientHandler extends SimpleChannelHandler {

    long start_time;
    /** Used for client or server's token to send or receive from each other. */
    private byte[] token;
    private String userName;

    public SaslStormClientHandler() throws IOException {
        start_time = System.currentTimeMillis();
        getSASLCredentials();
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent event) {
        // register the newly established channel
        final Channel channel = ctx.getChannel();

        try {
            SaslNettyClient saslNettyClient = SaslNettyClientState.getSaslNettyClient
                    .get(channel);

            if (saslNettyClient == null) {
                System.out.println("Creating saslNettyClient now "
                        + "for channel: " + channel);
                saslNettyClient = new SaslNettyClient(userName, token);
                // Store the saslNettyClient state for this specific channel.
                SaslNettyClientState.getSaslNettyClient.set(channel,
                        saslNettyClient);
            }

            channel.write(ControlMessage.SASL_TOKEN_MESSAGE_REQUEST);
        } catch (final Exception e) {
            System.err.println("Failed to authenticate with server "
                    + "due to error: " + e);
        }
        return;

    }

    private void getSASLCredentials() throws IOException {
        userName = SaslUtils.getUserName();
        final String secretKey = SaslUtils.getSecretKey();
        if (secretKey != null) {
            token = secretKey.getBytes();
        }
        System.out.println("SASL credentials for storm topology " + userName
                + " is " + secretKey);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx,
            final MessageEvent event) throws Exception {
        System.out.println("send/recv time (ms): {}"
                + (System.currentTimeMillis() - start_time));

        final Channel channel = ctx.getChannel();

        // Generate SASL response to server using Channel-local SASL client.
        final SaslNettyClient saslNettyClient = SaslNettyClientState.getSaslNettyClient
                .get(channel);
        if (saslNettyClient == null) {
            throw new Exception("saslNettyClient was unexpectedly "
                    + "null for channel: " + channel);
        }

        // examine the response message from server
        if (event.getMessage() instanceof ControlMessage) {
            final ControlMessage msg = (ControlMessage) event.getMessage();
            if (msg == ControlMessage.SASL_COMPLETE_REQUEST) {
                System.out.println("Server has sent us the SaslComplete "
                        + "message. Allowing normal work to proceed.");

                if (!saslNettyClient.isComplete()) {
                    System.err
                            .println("Server returned a Sasl-complete message, "
                                    + "but as far as we can tell, we are not authenticated yet.");
                    throw new Exception("Server returned a "
                            + "Sasl-complete message, but as far as "
                            + "we can tell, we are not authenticated yet.");
                }

                SaslUtils.checkSaslNegotiatedProtection(saslNettyClient);
                saslNettyClient.setUseWrap();

                ctx.getPipeline().remove(this);
                // We call fireMessageReceived since the client is allowed to
                // perform this request. The client's request will now proceed
                // to the next pipeline component namely StormClientHandler.
                // Channels.fireMessageReceived(ctx, msg);
                Channels.fireChannelConnected(ctx, ctx.getChannel()
                        .getRemoteAddress());
                return;
            }
        }
        final SaslMessageToken saslTokenMessage = (SaslMessageToken) event
                .getMessage();
        System.out.println("Responding to server's token of length: "
                + saslTokenMessage.getSaslToken().length);

        // Generate SASL response (but we only actually send the response if
        // it's non-null.
        final byte[] responseToServer = saslNettyClient
                .saslResponse(saslTokenMessage);
        if (responseToServer == null) {
            // If we generate a null response, then authentication has completed
            // (if not, warn), and return without sending a response back to the
            // server.
            System.out.println("Response to server is null: "
                    + "authentication should now be complete.");
            if (!saslNettyClient.isComplete()) {
                System.err.println("Generated a null response, "
                        + "but authentication is not complete.");
                throw new Exception("Server reponse is null, but as far as "
                        + "we can tell, we are not authenticated yet.");
            }

            // SaslUtils.checkSaslNegotiatedProtection(saslNettyClient);
            // saslNettyClient.setUseWrap();
            return;
        } else {
            System.out.println("Response to server token has length:"
                    + responseToServer.length);
        }
        // Construct a message containing the SASL response and send it to the
        // server.
        final SaslMessageToken saslResponse = new SaslMessageToken(
                responseToServer);
        channel.write(saslResponse);
    }
}