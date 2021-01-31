package space.wudi.learnio.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.IntSupplier;

@SuppressWarnings({"unused", "all"})
public class ClientHandler {
    private final int BUFFER_SIZE = 4 * 1024;
    private final int START_STATE = 2;
    private final int MAX_RETRY = 3;
    private final int ERROR_HANDLER = 1;
    private final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    private final String QUIT = "quit";



    private SelectionKey selectionKey;
    private SocketChannel client;
    private ByteBuffer buffer;
    private int current;
    private String lastError;
    private int retryIndex;
    private int retryCount;

    private String reply = "";
    private final IntSupplier[] suppliers = new IntSupplier[]{
    /* 0 */ () -> {
                // finish state
                System.out.println("handler finished");
                try {
                    client.write(DEFAULT_CHARSET.encode("Bye~\n"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return -1;
            },
    /* 1 */ () -> {
                // error handle
                if(retryCount > 0){
                    try{
                        client.write(DEFAULT_CHARSET.encode(lastError+" Retry:\n"));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return 0;
                    }
                    selectionKey.interestOps(SelectionKey.OP_READ);
                    return retryIndex;
                }else{
                    try{
                        client.write(DEFAULT_CHARSET.encode(lastError+" Reaches retry limit.\n"));
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                    return 0;
                }
            },
    /* 2 */ () -> {
                // write hello
                try {
                    client.write(DEFAULT_CHARSET.encode("欢迎来到智能对答器。请你首先发言。\n"));
                } catch (IOException e) {
                    e.printStackTrace();
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                    return 0;
                }
                selectionKey.interestOps(SelectionKey.OP_READ);
                return 3;
            },
    /* 3 */ () -> {
                // read sentence
                System.out.println("read sentence");
                Optional<String> stringOptional = Optional.empty();
                try {
                    stringOptional = readStringFromClient();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(!stringOptional.isPresent()){
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                    return 0;
                }
                String sentence = stringOptional.get().trim();
                System.out.println("comes: "+sentence);
                if(QUIT.equals(sentence)){
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                    return 0;
                }
                int len = sentence.length();

                if(sentence.endsWith("吗") || sentence.endsWith("么")){
                    reply = sentence.substring(0, len-1);
                } else if(sentence.endsWith("吗？")){
                    reply = sentence.substring(0, len-2);
                } else if(sentence.endsWith("。")){
                    reply = sentence.substring(0, len-1) + "！";
                } else {
                    reply = sentence + "！";
                }
                reply = reply.replace("你", "我");
                selectionKey.interestOps(SelectionKey.OP_WRITE);
                return 4;
            },
    /* 4 */ () -> {
                System.out.println("write reply");
                try{
                    client.write(DEFAULT_CHARSET.encode(reply+"\n"));
                }catch (IOException e){
                    e.printStackTrace();
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                    return 0;
                }
                selectionKey.interestOps(SelectionKey.OP_READ);
                return 3;
            }
    };

    public ClientHandler(SelectionKey selectionKey){
        this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.current = START_STATE;
        this.selectionKey = selectionKey;
        this.client = (SocketChannel) selectionKey.channel();
    }

    public boolean handle(){
        current = suppliers[current].getAsInt();
        return current >= 0;
    }

    private Optional<String> readStringFromClient() throws IOException {
        StringBuilder sb = new StringBuilder();
        while(true){
            buffer.clear();
            int length = client.read(buffer);
            if(length < 0 ){
                selectionKey.cancel();
                return Optional.empty();
            }else if(length == 0){
                break;
            }else{
                buffer.flip();
                sb.append(DEFAULT_CHARSET.decode(buffer).toString());
            }
        }
        return Optional.of(sb.toString());
    }

    private void clearLastError() {
        lastError = null;
        retryCount = MAX_RETRY;
        retryIndex = -2;
    }

    private int setErrot(String error, int currentIndex) {
        if(lastError == null || !lastError.equals(error)){
            lastError = error;
            retryCount = MAX_RETRY;
        }else{
            retryCount--;
        }
        retryIndex = currentIndex;
        selectionKey.interestOps(SelectionKey.OP_WRITE);
        return ERROR_HANDLER;
    }
}
