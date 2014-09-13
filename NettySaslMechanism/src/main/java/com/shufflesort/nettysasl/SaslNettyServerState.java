package com.shufflesort.nettysasl;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelLocal;

final class SaslNettyServerState {

    public static final ChannelLocal<SaslNettyServer> getSaslNettyServer = new ChannelLocal<SaslNettyServer>() {
        @Override
        protected SaslNettyServer initialValue(final Channel channel) {
            return null;
        }
    };

}