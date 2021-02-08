package space.wudi.learnio.nio;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.channels.*;
import java.util.function.Function;

@SuppressWarnings("all")
public abstract class RegisterHolder {
    final SelectableChannel channel;
    final int ops;

    public RegisterHolder(SelectableChannel channel, int ops) {
        this.channel = channel;
        this.ops = ops;
    }

    public abstract void register(Selector selector, Function<SocketChannel, Class<? extends ClientHandler>> handlerBuilder) throws IOException;

    public static class ServerRegisterHolder extends RegisterHolder {

        public ServerRegisterHolder(SelectableChannel channel) {
            super(channel, SelectionKey.OP_ACCEPT);
        }

        /**
         * register an accept channel to the selector
         * @param handlerBuilder not used
         */
        @Override
        public void register(Selector selector, Function<SocketChannel, Class<? extends ClientHandler>> notused) throws ClosedChannelException {
            this.channel.register(selector, this.ops);
            System.out.println("server registered");
        }
    }

    public static class ClientRegisterHolder extends RegisterHolder {

        public ClientRegisterHolder(SelectableChannel channel) {
            super(channel, SelectionKey.OP_WRITE);
        }

        /**
         * register a client IO channel to the selector
         * @param handlerBuilder to build a client handler
         * @throws IOException
         */
        @Override
        public void register(Selector selector, Function<SocketChannel, Class<? extends ClientHandler>> handlerBuilder) throws IOException {
            SelectionKey clientKey = this.channel.register(selector, this.ops);
            try {
                // get a new client handler
                Class<? extends ClientHandler> clientHandlerClazz = handlerBuilder.apply((SocketChannel) clientKey.channel());
                Constructor<? extends ClientHandler> declaredConstructor = clientHandlerClazz.getDeclaredConstructor(SelectionKey.class);
                ClientHandler clientHandler = declaredConstructor.newInstance(clientKey);
                // add the client handler to clientkey's attachment
                clientKey.attach(clientHandler);
                System.out.println("client "+((SocketChannel)channel).getRemoteAddress()+" registered");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
