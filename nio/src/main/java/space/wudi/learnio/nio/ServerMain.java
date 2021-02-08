package space.wudi.learnio.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.LockSupport;

public class ServerMain {
    public static void main(String[] args) {
        try {
            new ServerSelectorGroup(12306, 3, (socketChannel -> MyClientHandler.class));
            LockSupport.park();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
