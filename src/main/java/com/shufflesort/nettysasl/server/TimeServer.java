package com.shufflesort.nettysasl.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufflesort.nettysasl.decoders.ServerUnwrapMessageDecoder;
import com.shufflesort.nettysasl.encoders.MessageEncoder;
import com.shufflesort.nettysasl.encoders.ServerWrapMessageEncoder;
import com.shufflesort.nettysasl.handlers.SaslStormServerAuthorizeHandler;
import com.shufflesort.nettysasl.handlers.SaslStormServerHandler;
import com.shufflesort.nettysasl.handlers.TimeServerHandler;

public class TimeServer {

	private static final Logger logger = LoggerFactory
			.getLogger(TimeServer.class);

	public static final ChannelGroup allChannels = new DefaultChannelGroup(
			"time-server");

	public static void main(final String args[]) throws Exception {
		logger.debug("Hello Sai.");

		final ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());

		final ServerBootstrap bootstrap = new ServerBootstrap(factory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws IOException {
				return Channels.pipeline(
						// new TimeEncoder(),
						new ServerWrapMessageEncoder(),
						new MessageEncoder(),
						// new TimeDecoder(),
						new ServerUnwrapMessageDecoder(),
						// new MessageDecoder(),
						new SaslStormServerHandler(),
						new SaslStormServerAuthorizeHandler(),
						new TimeServerHandler());
			}
		});

		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);

		final Channel channel = bootstrap.bind(new InetSocketAddress(8081));
		allChannels.add(channel);

		/*
		 * waitForShutdownCommand(); final ChannelGroupFuture future =
		 * allChannels.close(); future.awaitUninterruptibly();
		 * factory.releaseExternalResources();
		 */
	}
}
