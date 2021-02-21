package space.wudi.learnio.nettyInOut;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;


public class Main {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup(3);
        NioServerSocketChannel server = new NioServerSocketChannel();
        server.config().setAutoRead(false);
        ChannelPipeline serverPipeline = server.pipeline();
        serverPipeline.addLast(new InHandler(group, (remoteAddress -> {
            final List<ChannelHandler> handlers = new ArrayList<>();
            handlers.add(new OutHandler("ClientOut0"));
            handlers.add(new InHandler("ClientIn1"));
            handlers.add(new OutHandler("ClientOut1"));
            handlers.add(new InHandler("ClientIn2"));
            handlers.add(new OutHandler("ClientOut2"));
            return handlers;
        }), "AcceptIn1"));
        serverPipeline.addLast(new OutHandler("AcceptOut1"));
        group.register(server);
        server.bind(new InetSocketAddress(12306));
        LockSupport.park();
    }
}
