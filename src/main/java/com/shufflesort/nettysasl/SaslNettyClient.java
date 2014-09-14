package com.shufflesort.nettysasl;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import com.shufflesort.nettysasl.model.SaslMessageToken;
import com.shufflesort.nettysasl.util.SaslUtils;

/**
 * Implements SASL logic for storm worker client processes.
 */
public class SaslNettyClient {

    /**
     * Implementation of javax.security.auth.callback.CallbackHandler that works
     * with Storm topology tokens.
     */
    private static class SaslClientCallbackHandler implements CallbackHandler {
        /** Generated username contained in TopologyToken */
        private final String userName;
        /** Generated password contained in TopologyToken */
        private final char[] userPassword;

        /**
         * Set private members using topology token.
         * 
         * @param topologyToken
         */
        public SaslClientCallbackHandler(final String userName,
                final byte[] token) {
            this.userName = SaslUtils.encodeIdentifier(userName.getBytes());
            userPassword = SaslUtils.encodePassword(token);
        }

        /**
         * Implementation used to respond to SASL tokens from server.
         * 
         * @param callbacks
         *            objects that indicate what credential information the
         *            server's SaslServer requires from the client.
         * @throws UnsupportedCallbackException
         */
        @Override
        public void handle(final Callback[] callbacks)
                throws UnsupportedCallbackException {
            NameCallback nc = null;
            PasswordCallback pc = null;
            RealmCallback rc = null;
            for (final Callback callback : callbacks) {
                if (callback instanceof RealmChoiceCallback) {
                    continue;
                } else if (callback instanceof NameCallback) {
                    nc = (NameCallback) callback;
                } else if (callback instanceof PasswordCallback) {
                    pc = (PasswordCallback) callback;
                } else if (callback instanceof RealmCallback) {
                    rc = (RealmCallback) callback;
                } else {
                    throw new UnsupportedCallbackException(callback,
                            "handle: Unrecognized SASL client callback");
                }
            }
            if (nc != null) {
                System.out
                        .println("handle: SASL client callback: setting username: "
                                + userName);
                nc.setName(userName);
            }
            if (pc != null) {
                System.out
                        .println("handle: SASL client callback: setting userPassword");
                pc.setPassword(userPassword);
            }
            if (rc != null) {
                System.out
                        .println("handle: SASL client callback: setting realm: "
                                + rc.getDefaultText());
                rc.setText(rc.getDefaultText());
            }
        }
    }

    /**
     * Used to respond to server's counterpart, SaslServer with SASL tokens
     * represented as byte arrays.
     */
    private SaslClient saslClient;

    private boolean useWrap = false;

    /**
     * Create a SaslNettyClient for authentication with servers.
     */
    public SaslNettyClient(final String userName, final byte[] token) {
        try {
            System.out.println("SaslNettyClient: Creating SASL "
                    + SaslUtils.AUTH_DIGEST_MD5
                    + " client to authenticate to server ");

            saslClient = Sasl.createSaslClient(
                    new String[] { SaslUtils.AUTH_DIGEST_MD5 }, null, null,
                    SaslUtils.DEFAULT_REALM, SaslUtils.getSaslProps(),
                    new SaslClientCallbackHandler(userName, token));

        } catch (final IOException e) {
            System.err
                    .println("SaslNettyClient: Could not obtain topology token for Netty "
                            + "Client to use to authenticate with a Netty Server.");
            saslClient = null;
        }
    }

    public void dispose() throws SaslException {
        saslClient.dispose();
    }

    public String getNegotiatedQop() {
        return (String) saslClient.getNegotiatedProperty(Sasl.QOP);
    }

    public boolean isComplete() {
        return saslClient.isComplete();
    }

    public boolean isUseWrap() {
        return useWrap;
    }

    /**
     * Respond to server's SASL token.
     * 
     * @param saslTokenMessage
     *            contains server's SASL token
     * @return client's response SASL token
     */
    public byte[] saslResponse(final SaslMessageToken saslTokenMessage) {
        try {
            final byte[] retval = saslClient.evaluateChallenge(saslTokenMessage
                    .getSaslToken());
            return retval;
        } catch (final SaslException e) {
            System.err
                    .println("saslResponse: Failed to respond to SASL server's token:"
                            + e.getMessage());
            return null;
        }
    }

    public void setUseWrap() {
        final String qop = (String) saslClient.getNegotiatedProperty(Sasl.QOP);
        System.out.println("saslClient Negotiated quality of service: " + qop);
        useWrap = qop != null && !"auth".equalsIgnoreCase(qop);
        System.out.println("Setting SaslNettyClient useWrap to " + useWrap);
    }

    public byte[] unwrap(final byte[] outgoing, final int off, final int len)
            throws SaslException {
        return saslClient.unwrap(outgoing, off, len);
    }

    public byte[] wrap(final byte[] outgoing, final int off, final int len)
            throws SaslException {
        return saslClient.wrap(outgoing, off, len);
    }

}
