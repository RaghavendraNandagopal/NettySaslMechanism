package com.shufflesort.nettysasl;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelLocal;

final class SaslNettyClientState {

    public static final ChannelLocal<SaslNettyClient> getSaslNettyClient = new ChannelLocal<SaslNettyClient>() {
        @Override
        protected SaslNettyClient initialValue(final Channel channel) {
            return null;
        }
    };

}
