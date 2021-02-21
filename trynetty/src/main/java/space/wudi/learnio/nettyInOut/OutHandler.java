package space.wudi.learnio.nettyInOut;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class OutHandler extends ChannelOutboundHandlerAdapter {
    final private String name;

    public OutHandler(String name){
        this.name = name;
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) {
        final String op = "bind";
        printInfo(op, ctx, localAddress, promise);
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        final String op = "connect";
        printInfo(op, ctx, remoteAddress, localAddress, promise);
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) {
        final String op = "deregister";
        printInfo(op, ctx, promise);
        ctx.deregister(promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        final String op = "disconnect";
        printInfo(op, ctx, promise);
        ctx.disconnect(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) {
        final String op = "read";
        printInfo(op, ctx);
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        final String op = "write";
        printInfo(op, ctx, msg, promise);
        ctx.write(msg, promise);
/*
        ctx.channel().write(msg, promise);
        ctx.pipeline().write(msg,promise);
        Throwable throwable = promise.cause();
        if(throwable != null){
            System.out.println(throwable.getClass()+": "+throwable.getMessage());

        }
*/
    }

    @Override
    public void flush(ChannelHandlerContext ctx) {
        final String op = "flush";
        printInfo(op, ctx);
        ctx.flush();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        final String op = "close";
        printInfo(op, ctx, promise);
        ctx.close();
    }

    @SuppressWarnings("all")
    private void printInfo(String op, ChannelHandlerContext ctx, Object... extraInfos){
        StringBuilder sb = new StringBuilder();
        do{
            if (extraInfos == null){
                break;
            }
            int iMax = extraInfos.length - 1;
            if (iMax == -1){
                break;
            }
            sb.append('[');
            for (int i = 0; ; i++) {
                if(extraInfos[i] == null){
                    sb.append(extraInfos[i]);
                }else{
                    String clazz = extraInfos[i].getClass().getSimpleName();
                    sb.append(clazz).append(": ");
                    if(extraInfos[i] instanceof ByteBuf){
                        ByteBuf byteBuf = (ByteBuf) extraInfos[i];
                        String msg = (String)byteBuf.getCharSequence(0, byteBuf.readableBytes(), StandardCharsets.UTF_8);
                        sb.append(" msg: \"").append(msg).append("\"");
                    }else if(extraInfos[i] instanceof Throwable){
                        ((Throwable)extraInfos[i]).printStackTrace();
                    }else{
                        sb.append(extraInfos[i]);
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
                op,
                sb.toString());
    }
}
