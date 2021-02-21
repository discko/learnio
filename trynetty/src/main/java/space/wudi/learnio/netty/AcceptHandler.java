package space.wudi.learnio.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Function;

public class AcceptHandler extends ChannelInboundHandlerAdapter {
    NioEventLoopGroup group;
    Function<SocketAddress, Collection<ChannelInboundHandlerAdapter>> handlersBuilder;

    public AcceptHandler(NioEventLoopGroup group, Function<SocketAddress, Collection<ChannelInboundHandlerAdapter>> handlersBuilder) {
        this.group = group;
        this.handlersBuilder = handlersBuilder;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        System.out.println(Thread.currentThread().getName()+"-"+"accept channel registered");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(Thread.currentThread().getName()+"-"+"accept channel activated");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        NioSocketChannel client = (NioSocketChannel) msg;
        System.out.println(Thread.currentThread().getName()+"-"+"new client connected: "+client.remoteAddress());
        ChannelPipeline pipeline = client.pipeline();
        for (ChannelInboundHandler handler: handlersBuilder.apply(ctx.channel().remoteAddress())) {
            pipeline.addLast(handler);
        }
        group.register(client);
        client.writeAndFlush(Unpooled.copiedBuffer("欢迎来到智能对话机~\n", StandardCharsets.UTF_8));
    }
}
