package com.shufflesort.nettysasl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.security.sasl.Sasl;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;

class SaslUtils {
    public static final String AUTH_DIGEST_MD5 = "DIGEST-MD5";

    public static final String DEFAULT_REALM = "default";

    /**
     * Checks the negotiated quality of protection is included in the given SASL
     * properties and therefore acceptable.
     * 
     * @param sasl
     *            participant to check
     * @param conf
     *            configuration parameters
     * @throws IOException
     *             for any error
     */
    public static void checkSaslNegotiatedProtection(final Object sasl)
            throws IOException {

        final List<String> requestedQop = new ArrayList<String>();
        // requestedQop.add("auth-int");
        requestedQop.add("auth-conf");
        // requestedQop.add("auth");

        String negotiatedQop = null;
        if (sasl instanceof SaslNettyClient) {
            negotiatedQop = ((SaslNettyClient) sasl).getNegotiatedQop();
        } else {
            negotiatedQop = ((SaslNettyServer) sasl).getNegotiatedQop();
        }

        System.out.println("Verifying QOP, requested QOP = {" + requestedQop
                + "}, negotiated QOP = {" + negotiatedQop + "}");
        if (!requestedQop.contains(negotiatedQop)) {
            throw new IOException(
                    String.format(
                            "SASL handshake completed, but "
                                    + "channel does not have acceptable quality of protection, "
                                    + "requested = %s, negotiated = %s",
                            requestedQop, negotiatedQop));
        }
    }

    /**
     * Encode a identifier as a base64-encoded char[] array.
     * 
     * @param identifier
     *            as a byte array.
     * @return identifier as a char array.
     */
    static String encodeIdentifier(final byte[] identifier) {
        return new String(Base64.encodeBase64(identifier), Charsets.UTF_8);
    }

    /**
     * Encode a password as a base64-encoded char[] array.
     * 
     * @param password
     *            as a byte array.
     * @return password as a char array.
     */
    static char[] encodePassword(final byte[] password) {
        return new String(Base64.encodeBase64(password), Charsets.UTF_8)
                .toCharArray();
    }

    private static Map<String, String> getSaslPropertiesConf() {
        final Map<String, String> properties = new TreeMap<String, String>();

        final List<String> requestedQop = new ArrayList<String>();
        // requestedQop.add("auth-int");
        requestedQop.add("auth-conf");
        // requestedQop.add("auth");

        final String[] qop = new String[requestedQop.size()];
        for (int i = 0; i < requestedQop.size(); i++) {
            qop[i] = requestedQop.get(i).toUpperCase();
        }
        properties.put(Sasl.QOP, SaslUtils.join(",", qop));
        // properties.put(Sasl.QOP, "auth");
        properties.put(Sasl.SERVER_AUTH, "true");
        return properties;
    }

    static Map<String, String> getSaslProps() {
        return getSaslPropertiesConf();
    }

    static String getSecretKey() {
        return "SaiRam";
    }

    static String getUserName() {
        return "SaiRam";
    }

    /**
     * Concatenates strings, using a separator.
     * 
     * @param separator
     *            to join with
     * @param strings
     *            to join
     * @return the joined string
     */
    public static String join(final CharSequence separator,
            final String[] strings) {
        // Ideally we don't have to duplicate the code here if array is
        // iterable.
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String s : strings) {
            if (first) {
                first = false;
            } else {
                sb.append(separator);
            }
            sb.append(s);
        }
        return sb.toString();
    }

};
