package space.wudi.learnio.nio;

import java.nio.channels.SelectionKey;

public abstract class ClientHandler {

    protected final SelectionKey selectionKey;

    ClientHandler(SelectionKey selectionKey){
        this.selectionKey = selectionKey;
    }
    /**
     * handle the client messages
     * @return false if no need to communicate to the client any more
     */
    abstract boolean handle();

}
