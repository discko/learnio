package space.wudi.learnio.nettybootstrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import space.wudi.learnio.netty.ClientHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.LockSupport;

public class ServerMain {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup acceptLoopGroup = new NioEventLoopGroup(2);
        EventLoopGroup workerLoopGroup = new NioEventLoopGroup(3);


        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(acceptLoopGroup, workerLoopGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);

        serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        serverBootstrap.childHandler(new ChannelInit());

        serverBootstrap.bind(new InetSocketAddress(12306)).sync();
        System.out.println("server started");
        LockSupport.park();
    }

    static class ChannelInit extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            System.out.println("in init pipeline: "+pipeline.toMap());
            pipeline.addLast(new ClientHandler());
        }
    }
}
