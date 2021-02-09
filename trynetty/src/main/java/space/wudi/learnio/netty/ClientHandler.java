package space.wudi.learnio.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        System.out.println(Thread.currentThread().getName()+"-"+"client "+ ctx.channel().remoteAddress() + " registered");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(Thread.currentThread().getName()+"-"+"client "+ ctx.channel().remoteAddress() + " activated");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuffer = (ByteBuf) msg;
        String sentence = ((String) byteBuffer.readCharSequence(byteBuffer.readableBytes(), StandardCharsets.UTF_8)).trim();
        System.out.println(Thread.currentThread().getName()+"-"+ctx.name()+" client "+ ctx.channel().remoteAddress() + ": "+sentence);
        if("quit".equals(sentence)){
            ctx.writeAndFlush(toByteBuf("Bye~")).sync();
            System.out.println(Thread.currentThread().getName()+"-"+ctx.name()+" disconnecting the client "+ctx.channel().remoteAddress());
            ctx.disconnect().sync();
            return;
        }
        String outSentence = getReply(sentence);
        ctx.writeAndFlush(toByteBuf(outSentence)).sync();
        System.out.println(Thread.currentThread().getName()+"-"+ctx.name()+ "to client "+ctx.channel().remoteAddress()+": "+outSentence);
    }

    private ByteBuf toByteBuf(String msg){
        return Unpooled.copiedBuffer(msg, StandardCharsets.UTF_8);
    }

    private String getReply(String inSentence) {
        String outSentence;
        if(inSentence.endsWith("么？") || inSentence.endsWith("吗？") ||
                inSentence.endsWith("么?") || inSentence.endsWith("吗?")){
            outSentence = inSentence.substring(0, inSentence.length()-2)+"！";
        }else if(inSentence.endsWith("么") || inSentence.endsWith("吗") ||
                inSentence.endsWith("？") || inSentence.endsWith("?") ||
                inSentence.endsWith("。") || inSentence.endsWith(".")){
            outSentence = inSentence.substring(0, inSentence.length() - 1) + "！";
        }else{
            outSentence = inSentence + "！";
        }
        return outSentence+ "\n";
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println(Thread.currentThread().getName()+"-"+ctx.name()+" client "+ ctx.channel().remoteAddress() + " inactivated");
//        ctx.deregister();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        System.out.println(Thread.currentThread().getName()+"-"+ctx.name()+" client "+ ctx.channel().remoteAddress() + " unregistered");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
