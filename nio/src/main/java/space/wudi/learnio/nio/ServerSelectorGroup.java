package space.wudi.learnio.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.locks.LockSupport;


public class ServerSelectorGroup {
    private ServerSocketChannel[] servers;
    private SelectorRunnable serverRunnable;
    private SelectorRunnable[] workerRunnables;


    public ServerSelectorGroup(int port, int workers) throws IOException {
        this(new int[]{port}, workers);
    }

    public ServerSelectorGroup(int[] ports, int workers) throws IOException {
        System.out.println("preparing workers");
        workerRunnables = new SelectorRunnable[workers];
        for (int i = 0; i < workers; i++) {
            workerRunnables[i] = new SelectorRunnable();
            new Thread(workerRunnables[i], "WorkerThread-"+i).start();
        }

        System.out.println("preparing server");
        servers = new ServerSocketChannel[ports.length];
        serverRunnable = new SelectorRunnable();

        serverRunnable.setWorkers(workerRunnables);


        new Thread(serverRunnable, "ServerThread").start();
        for (int i = 0; i < ports.length; i++) {
            servers[i] = ServerSocketChannel.open();
            servers[i].bind(new InetSocketAddress(ports[i]));
            servers[i].configureBlocking(false);
//            System.out.println("before get key");
//            SelectionKey key = serverRunnable.register(servers[i], SelectionKey.OP_ACCEPT, null);
//            System.out.println("outside "+ key);
            serverRunnable.addRegisterQueueAndWakeUpSelector(new RegisterHolder.ServerRegisterHolder(servers[i]));
            System.out.println("server add to queue");
        }
        System.out.println("init done");
    }

    public ServerSocketChannel[] getServers() {
        return servers;
    }

    public SelectorRunnable getServerRunnable() {
        return serverRunnable;
    }

    public SelectorRunnable[] getWorkerRunnables() {
        return workerRunnables;
    }

    public void park(){
        LockSupport.park();
    }
}
