package com.shufflesort.nettysasl;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import com.shufflesort.nettysasl.util.SaslUtils;

public class SaslNettyServer {

    /** CallbackHandler for SASL DIGEST-MD5 mechanism */
    public static class SaslDigestCallbackHandler implements CallbackHandler {

        private final String userName;
        /** Used to authenticate the clients */
        private final byte[] userPassword;

        public SaslDigestCallbackHandler(final String userName,
                final byte[] token) {
            System.out
                    .println("SaslDigestCallback: Creating SaslDigestCallback handler "
                            + "with user name: " + userName);
            this.userName = userName;
            userPassword = token;
        }

        @Override
        public void handle(final Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            NameCallback nc = null;
            PasswordCallback pc = null;
            AuthorizeCallback ac = null;

            for (final Callback callback : callbacks) {
                if (callback instanceof AuthorizeCallback) {
                    ac = (AuthorizeCallback) callback;
                } else if (callback instanceof NameCallback) {
                    nc = (NameCallback) callback;
                } else if (callback instanceof PasswordCallback) {
                    pc = (PasswordCallback) callback;
                } else if (callback instanceof RealmCallback) {
                    continue; // realm is ignored
                } else {
                    throw new UnsupportedCallbackException(callback,
                            "handle: Unrecognized SASL DIGEST-MD5 Callback");
                }
            }

            if (nc != null) {
                System.out
                        .println("handle: SASL server DIGEST-MD5 callback: setting "
                                + "username for client: " + userName);

                nc.setName(userName);
            }

            if (pc != null) {
                final char[] password = SaslUtils.encodePassword(userPassword);

                System.out
                        .println("handle: SASL server DIGEST-MD5 callback: setting "
                                + "password for client: " + userPassword);

                pc.setPassword(password);
            }
            if (ac != null) {

                final String authid = ac.getAuthenticationID();
                final String authzid = ac.getAuthorizationID();

                if (authid.equals(authzid)) {
                    ac.setAuthorized(true);
                } else {
                    ac.setAuthorized(false);
                }

                if (ac.isAuthorized()) {
                    System.out
                            .println("handle: SASL server DIGEST-MD5 callback: setting "
                                    + "canonicalized client ID: " + userName);
                    ac.setAuthorizedID(authzid);
                }
            }
        }
    }

    private SaslServer saslServer;

    private boolean useWrap = false;

    SaslNettyServer(final String userName, final byte[] token)
            throws IOException {
        System.out.println("SaslNettyServer: Topology token is: " + userName
                + " with authmethod " + SaslUtils.AUTH_DIGEST_MD5);

        try {

            final SaslDigestCallbackHandler ch = new SaslNettyServer.SaslDigestCallbackHandler(
                    userName, token);

            saslServer = Sasl.createSaslServer(SaslUtils.AUTH_DIGEST_MD5, null,
                    SaslUtils.DEFAULT_REALM, SaslUtils.getSaslProps(), ch);

        } catch (final SaslException e) {
            System.err.println("SaslNettyServer: Could not create SaslServer: "
                    + e);
        }

    }

    public void dispose() throws SaslException {
        saslServer.dispose();
    }

    public String getNegotiatedQop() {
        return (String) saslServer.getNegotiatedProperty(Sasl.QOP);
    }

    public String getUserName() {
        return saslServer.getAuthorizationID();
    }

    public boolean isComplete() {
        return saslServer.isComplete();
    }

    public boolean isUseWrap() {
        return useWrap;
    }

    /**
     * Used by SaslTokenMessage::processToken() to respond to server SASL
     * tokens.
     * 
     * @param token
     *            Server's SASL token
     * @return token to send back to the server.
     */
    public byte[] response(final byte[] token) {
        try {
            System.out
                    .println("response: Responding to input token of length: "
                            + token.length);
            final byte[] retval = saslServer.evaluateResponse(token);
            System.out.println("response: Response token length: "
                    + retval.length);
            return retval;
        } catch (final SaslException e) {
            System.err
                    .println("response: Failed to evaluate client token of length: "
                            + token.length + " : " + e);
            return null;
        }
    }

    public void setUseWrap() {
        final String qop = (String) saslServer.getNegotiatedProperty(Sasl.QOP);
        System.out.println("saslServer Negotiated quality of service: " + qop);
        useWrap = qop != null && !"auth".equalsIgnoreCase(qop);
        System.out.println("Setting SaslNettyServer useWrap to " + useWrap);
    }

    public byte[] unwrap(final byte[] outgoing, final int off, final int len)
            throws SaslException {
        return saslServer.unwrap(outgoing, off, len);
    }

    public byte[] wrap(final byte[] outgoing, final int off, final int len)
            throws SaslException {
        return saslServer.wrap(outgoing, off, len);
    }
}