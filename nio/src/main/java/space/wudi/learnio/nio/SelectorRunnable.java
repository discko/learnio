package space.wudi.learnio.nio;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("all")
public class SelectorRunnable implements Runnable {

    private Selector selector;

    private SelectorRunnable[] workers;

    private AtomicInteger nextWorker;

    private String threadName = "";

    private LinkedBlockingQueue<RegisterHolder> registerQueue;

    public SelectorRunnable() {
        this.registerQueue = new LinkedBlockingQueue<>();
        this.workers = new SelectorRunnable[]{this};
        this.nextWorker = new AtomicInteger(0);
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.threadName = Thread.currentThread().getName();
        System.out.printf("%s --- waiting selecting\n", threadName);
        while(true){
            try {
                int num = selector.select();
                if(num > 0 ){
                    System.out.println("select "+num);
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if(key.isAcceptable()){
                            handleAccept(key);
                        }else if(key.isReadable() || key.isWritable()){
                            handleReadWrite(key);
                        }
                    }
                }
                while(!registerQueue.isEmpty()){
                    RegisterHolder registerHolder = registerQueue.take();
                    registerHolder.register(selector);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setWorkers(SelectorRunnable[] workers){
        this.workers = workers;
        while(true){
            int nextWorkerValue = this.nextWorker.intValue();
            if(nextWorkerValue >= workers.length) {
                if(this.nextWorker.compareAndSet(nextWorkerValue, 0)){
                    break;
                }
            }else{
                break;
            }
        }
    }

    public void addRegisterQueueAndWakeUpSelector(RegisterHolder channel){
        registerQueue.add(channel);
        selector.wakeup();
    }

    public SelectionKey register(SelectableChannel channel, int ops, Object o) throws ClosedChannelException {
        SelectionKey key = channel.register(selector, ops, null);
        System.out.println("registered"+ key);
        return key;
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        System.out.printf("%s --- accepted %s\n", threadName, client.getRemoteAddress());
        client.configureBlocking(false);
        workers[nextWorker.getAndIncrement() % workers.length].addRegisterQueueAndWakeUpSelector(new RegisterHolder.ClientRegisterHolder(client));
        System.out.println("client add to queue");
    }

    private void handleReadWrite(SelectionKey key) throws IOException {
        System.out.printf("%s --- handle r/w from %s\n", threadName, ((SocketChannel)key.channel()).getRemoteAddress());
        ClientHandler handler = (ClientHandler) key.attachment();
        if(!handler.handle()){
            key.channel().close();
            key.cancel();
        }
    }
}
