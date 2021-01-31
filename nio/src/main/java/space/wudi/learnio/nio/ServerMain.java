package space.wudi.learnio.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ServerMain {
    public static void main(String[] args) {
        try {
            ServerSelectorGroup group = new ServerSelectorGroup(12306, 3);
            group.park();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteBuffer bb = ByteBuffer.allocate(10);
    }
}
