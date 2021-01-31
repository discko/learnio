package space.wudi.learnio.nio;

import java.io.IOException;
import java.nio.channels.*;

@SuppressWarnings("all")
public abstract class RegisterHolder {
    final SelectableChannel channel;
    final int ops;

    public RegisterHolder(SelectableChannel channel, int ops) {
        this.channel = channel;
        this.ops = ops;
    }

    public abstract void register(Selector selector) throws IOException;

    public static class ServerRegisterHolder extends RegisterHolder {

        public ServerRegisterHolder(SelectableChannel channel) {
            super(channel, SelectionKey.OP_ACCEPT);
        }

        @Override
        public void register(Selector selector) throws ClosedChannelException {
            this.channel.register(selector, this.ops);
            System.out.println("server registered");
        }
    }

    public static class ClientRegisterHolder extends RegisterHolder {

        public ClientRegisterHolder(SelectableChannel channel) {
            super(channel, SelectionKey.OP_WRITE);
        }

        @Override
        public void register(Selector selector) throws IOException {
            SelectionKey clientKey = this.channel.register(selector, this.ops);
            clientKey.attach(new ClientHandler(clientKey));
            System.out.println("client "+((SocketChannel)channel).getRemoteAddress()+" registered");
        }
    }
}
