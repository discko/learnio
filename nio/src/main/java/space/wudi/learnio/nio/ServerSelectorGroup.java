package space.wudi.learnio.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Function;


public class ServerSelectorGroup {
    /** array of accepting channels */
    private final ServerSocketChannel[] acceptChannels;
    /** the container of accepting channels */
    private final SelectorRunnable acceptRunnable;
    /** the containers of worker channels */
    private final SelectorRunnable[] workerRunnables;

    /**
     * init a server with specific port (on localhost) and workers num of workers
     * @param port server port to bind
     * @param workers   num of workers
     * @param clientHandlerBuilder a function returns a {@link ClientHandler} implementation according to {@link SocketChannel} info
     * @throws IOException if IO errors occur
     */
    public ServerSelectorGroup(int port, int workers, Function<SocketChannel, Class<? extends ClientHandler>> clientHandlerBuilder) throws IOException {
        this(new int[]{port}, workers, clientHandlerBuilder);
    }

    /**
     * init several servers with specific ports (all on localhost) and workers num of workers
     * @param ports an array of ports of servers to bind
     * @param workers   total num of workers
     * @param clientHandlerBuilder a function returns a {@link ClientHandler} implementation according to {@link SocketChannel} info
     * @throws IOException if IO errors occur
     */
    public ServerSelectorGroup(int[] ports, int workers, Function<SocketChannel, Class<? extends ClientHandler>> clientHandlerBuilder) throws IOException {
        System.out.println("preparing workers");
        workerRunnables = new SelectorRunnable[workers];
        for (int i = 0; i < workers; i++) {
            workerRunnables[i] = new SelectorRunnable(clientHandlerBuilder);
            new Thread(workerRunnables[i], "WorkerThread-"+i).start();
        }

        System.out.println("preparing server");
        acceptChannels = new ServerSocketChannel[ports.length];
        acceptRunnable = new SelectorRunnable(null);

        acceptRunnable.setWorkers(workerRunnables);

        // start the thead with accept channel
        new Thread(acceptRunnable, "ServerThread").start();
        for (int i = 0; i < ports.length; i++) {
            acceptChannels[i] = ServerSocketChannel.open();
            acceptChannels[i].bind(new InetSocketAddress(ports[i]));
            acceptChannels[i].configureBlocking(false); // set accept channel non-blocking
            // register accept channel into selector
            acceptRunnable.addRegisterQueueAndWakeUpSelector(new RegisterHolder.ServerRegisterHolder(acceptChannels[i]));
            System.out.println("server add to queue");
        }
        System.out.println("init done");
    }
    @SuppressWarnings("unused")
    public ServerSocketChannel[] getAcceptChannels() {
        return acceptChannels;
    }
    @SuppressWarnings("unused")
    public SelectorRunnable getAcceptRunnable() {
        return acceptRunnable;
    }
    @SuppressWarnings("unused")
    public SelectorRunnable[] getWorkerRunnables() {
        return workerRunnables;
    }
}
