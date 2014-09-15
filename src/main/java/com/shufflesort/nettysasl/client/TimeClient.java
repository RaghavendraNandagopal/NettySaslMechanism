package com.shufflesort.nettysasl.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.shufflesort.nettysasl.decoders.ClientUnwrapMessageDecoder;
import com.shufflesort.nettysasl.encoders.ClientWrapMessageEncoder;
import com.shufflesort.nettysasl.encoders.MessageEncoder;
import com.shufflesort.nettysasl.handlers.SaslStormClientHandler;
import com.shufflesort.nettysasl.handlers.TimeClientHandler;

public class TimeClient {

    public static void main(final String[] args) {
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);

        final ChannelFactory factory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        final ClientBootstrap bootstrap = new ClientBootstrap(factory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws IOException {
                return Channels.pipeline(
                // new TimeEncoder(),
                        new ClientWrapMessageEncoder(), new MessageEncoder(),
                        // new TimeDecoder(),
                        new ClientUnwrapMessageDecoder(),
                        // new MessageDecoder(),
                        new SaslStormClientHandler(), new TimeClientHandler());
            }
        });

        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);

        final ChannelFuture future = bootstrap.connect(new InetSocketAddress(
                host, port));

        future.awaitUninterruptibly();
        if (!future.isSuccess()) {
            future.getCause().printStackTrace();
        }

        future.getChannel().getCloseFuture().awaitUninterruptibly();
        factory.releaseExternalResources();

    }
}
