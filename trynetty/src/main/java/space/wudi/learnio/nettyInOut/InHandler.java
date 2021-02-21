package space.wudi.learnio.nettyInOut;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Function;

public class InHandler extends ChannelInboundHandlerAdapter {
    private final String name;
    private final EventLoopGroup group;
    private final Function<SocketAddress, Collection<ChannelHandler>> childHandlerBuilder;
    private final static String LF = "\n";

    private final static boolean READ = true;

    public InHandler(String name){
        this(null, null, name);
    }

    public InHandler(EventLoopGroup group, Function<SocketAddress, Collection<ChannelHandler>> childHandlersBuilder, String name){
        this.group = group;
        this.childHandlerBuilder = childHandlersBuilder;
        this.name = name;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        final String event = "registered";
        printInfo(event, ctx);
        if(READ){
            ctx.read();
        }
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final String event = "activated";
        printInfo(event, ctx);
        if(READ){
            ctx.read();
        }
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        final String event = "read";
        printInfo(event, ctx, msg);

        if(childHandlerBuilder == null){
            String sentence = ((String) ((ByteBuf) msg).readCharSequence(((ByteBuf) msg).readableBytes(), StandardCharsets.UTF_8)).trim();
            sentence += this.name;
            System.out.println(this.name+" sentence: "+sentence);
            ByteBuf replyByteBuf = Unpooled.copiedBuffer(sentence+LF, StandardCharsets.UTF_8);
            ctx.writeAndFlush(replyByteBuf);
            ByteBuf passByteBuf = Unpooled.copiedBuffer(sentence+LF, StandardCharsets.UTF_8);
            ctx.fireChannelRead(passByteBuf);
        }else{
            NioSocketChannel client = (NioSocketChannel) msg;
            for(ChannelHandler handler : childHandlerBuilder.apply(client.remoteAddress())){
                client.pipeline().addLast(handler);
            }
            group.register(client);
            ctx.fireChannelRead(client);
        }
        System.out.println(name);
        if(READ){
            ctx.read();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        final String event = "readComplete";
        printInfo(event, ctx);
        if(READ){
            ctx.read();
        }
        ctx.fireChannelReadComplete();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        final String event = "writabilityChanged";
        printInfo(event, ctx);
        if(READ){
            ctx.read();
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        final String event = "inactivated";
        printInfo(event, ctx);
        if(READ){
            ctx.read();
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        final String event = "unregistered";
        printInfo(event, ctx);
        if(READ){
            ctx.read();
        }
        ctx.fireChannelUnregistered();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final String event = "exceptionCaught";
        printInfo(event, ctx, cause);
        if(READ){
            ctx.read();
        }
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        final String event = "userEventTriggered";
        printInfo(event, ctx, evt);
        if(READ){
            ctx.read();
        }
        ctx.fireUserEventTriggered(evt);
    }

    @SuppressWarnings("all")
    private void printInfo(String event, ChannelHandlerContext ctx, Object... extraInfos){
        StringBuilder sb = new StringBuilder();
        do{
            if (extraInfos == null){
//                sb.append("null");
                break;
            }
            int iMax = extraInfos.length - 1;
            if (iMax == -1){
//                sb.append("[]");
                break;
            }
            sb.append('[');
            for (int i = 0; ; i++) {
                if(extraInfos[i] == null){
                    sb.append(extraInfos[i]);
                }else{
                    String clazz = extraInfos[i].getClass().getSimpleName();
                    sb.append(clazz).append(": ")
                            .append(extraInfos[i]);
                    if(extraInfos[i] instanceof ByteBuf){
                        ByteBuf byteBuf = (ByteBuf) extraInfos[i];
                        String msg = (String)byteBuf.getCharSequence(0, byteBuf.readableBytes(), StandardCharsets.UTF_8);
                        sb.append(" msg: \"").append(msg).append("\"");
                    }else if(extraInfos[i] instanceof Throwable){
                        ((Throwable)extraInfos[i]).printStackTrace();
                    }
                }
                if (i == iMax){
                    sb.append(']');
                    break;
                }
                sb.append(", ");
            }
        }while(false);
        System.out.printf("%s\t%s\t%s\t%s\t%s\n",
                Thread.currentThread().getName(),
                this.name,
                (ctx == null || ctx.channel() == null || ctx.channel().remoteAddress() == null ? "null" : (((InetSocketAddress)ctx.channel().remoteAddress()).getPort())),
                event,
                sb.toString());
    }

}
