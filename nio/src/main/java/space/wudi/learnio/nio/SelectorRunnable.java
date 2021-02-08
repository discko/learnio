package space.wudi.learnio.nio;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A selectorRunnable contains ONE selector
 */
@SuppressWarnings("all")
public class SelectorRunnable implements Runnable {

    /** for event handling */
    private Selector selector;
    /** worker threads for reading and writing */
    private SelectorRunnable[] workers;
    /** next index of {@link #workers} to use*/
    private AtomicInteger nextWorker;
    /** name of acceptance thread. not used */
    private String threadName = "";
    /** temporally stores the SocketChannels to be registered  */
    private LinkedBlockingQueue<RegisterHolder> registerQueue;

    private final Function<SocketChannel, Class<? extends ClientHandler>> handlerBuilder;

    public SelectorRunnable(Function<SocketChannel, Class<? extends ClientHandler>> handlerBuilder) {
        this.registerQueue = new LinkedBlockingQueue<>();
        this.workers = new SelectorRunnable[]{this};
        this.nextWorker = new AtomicInteger(0);
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.handlerBuilder = handlerBuilder;
    }

    @Override
    public void run() {
        this.threadName = Thread.currentThread().getName();
        System.out.printf("%s --- waiting selecting\n", threadName);
        while(true){
            try {
                int num = selector.select();    // block util event happens or be waken up
                if(num > 0 ){
                    // events happens rather than being waken up
                    System.out.println("select "+num);
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){  // travel all channel that happens event
                        SelectionKey key = iterator.next();
                        // selector will reuse the container and won't empty it
                        // so remember to remove the evented key after using
                        // kqueue see sun.nio.ch.KQueueSelectorImpl.updateSelectedKeys
                        iterator.remove();
                        if(key.isAcceptable()){
                            handleAccept(key);  // new come client
                        }else if(key.isReadable() || key.isWritable()){
                            handleReadWrite(key);   // IO ready
                        }
                    }
                }
                while(!registerQueue.isEmpty()){
                    // take out a ServerSocketChannel which put in at ServerSelectorGroup
                    // or take out a SocketChannel which put in at handleAccept()
                    RegisterHolder registerHolder = registerQueue.take();
                    // and register the channel in the holder to this selector
                    registerHolder.register(selector, handlerBuilder);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * reset group workers.
     * if next worker index to use is greater than number of new workers
     * next worker index will be reset to 0.
     * <pre>
     *     in this version, if workers are running, they cannot be stopped.
     *     so reset the worker would not stop the old ones neither transferring
     *     sessions to new workers.
     *     this problem may be fixed then.
     * </pre>
     */
    public void setWorkers(SelectorRunnable[] workers){
        this.workers = workers;
        while(true){
            int nextWorkerValue = this.nextWorker.intValue();
            if(nextWorkerValue >= workers.length) {
                // if next worker index is over new workers number
                // restore worker count to zero
                if(this.nextWorker.compareAndSet(nextWorkerValue, 0)){
                    break;
                }
            }else{
                break;
            }
        }
    }

    /**
     * add the channel holder into queue and wake up the selector.select()
     * to register the channel to the selector
     */
    public void addRegisterQueueAndWakeUpSelector(RegisterHolder channelHolder){
        registerQueue.add(channelHolder);
        // wake up the selector which is blocking at selector.select() above in run()
        selector.wakeup();
    }

    /**
     * use when the channel in key trigger the accepting event
     */
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept(); // get the new coming client
        System.out.printf("%s --- accepted %s\n", threadName, client.getRemoteAddress());
        client.configureBlocking(false);    // set it to non-blocking
        // add the client to workers[i]'s queue to register the client to workers[i]'s selector
        workers[nextWorkerIndex()].addRegisterQueueAndWakeUpSelector(new RegisterHolder.ClientRegisterHolder(client));
        System.out.println("client add to queue");
    }

    /**
     * get the index of next worker
     */
    private int nextWorkerIndex(){
        // use get and CAS rather than getAndIncrement
        // to prevent increment overflow
        int current, next;
        do{
            current = nextWorker.get();
            next = current + 1 < 0 ? 0 : current +1;
        }while(!nextWorker.compareAndSet(current, next));
        return current;
    }

    /**
     * call handler to handle the read and write events
     */
    private void handleReadWrite(SelectionKey key) throws IOException {
        System.out.printf("%s --- handle r/w from %s\n", threadName, ((SocketChannel)key.channel()).getRemoteAddress());
        ClientHandler handler = (ClientHandler) key.attachment();
        if(!handler.handle()){  // if return false, session can be closed
            key.channel().close();
            key.cancel();
        }
    }
}
