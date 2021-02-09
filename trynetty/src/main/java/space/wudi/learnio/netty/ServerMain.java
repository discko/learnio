package space.wudi.learnio.netty;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.locks.LockSupport;

public class ServerMain {

    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup(3);
        NioServerSocketChannel server = new NioServerSocketChannel();
        ChannelPipeline serverPipeline = server.pipeline();
        serverPipeline.addLast(new AcceptHandler(group, (remoteAddress -> Collections.singletonList(new ClientHandler()))));
        group.register(server);
        server.bind(new InetSocketAddress(12306));
        LockSupport.park();
    }
}
