package space.wudi.learnio.io;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class TestBioBuffer {

    private static volatile boolean flag = true;
    private static final int delay = 5000;

    public static void main(String[] args) throws IOException {
        test(1);
        test(10);
        test(100);
        test(8*1024);
        test(16*1024);
        test(24*1024);
    }

    private static void test(int bytesEachTime) throws IOException {
        System.out.println("------------test "+bytesEachTime+" bytes each time");
        byte[] data = getData(bytesEachTime);
        Timer timer1 = startTimer();
        testWithoutBuffer("/tmp/WithoutBuffer.txt."+bytesEachTime, data);
        timer1.cancel();
        Timer timer2 = startTimer();
        testWithBuffer("/tmp/WithBuffer.txt."+bytesEachTime, data);
        timer2.cancel();
    }

    private static Timer startTimer(){
        flag = true;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() { flag = false; }
        }, delay);
        return timer;
    }

    private static byte[] getData(int len){
        byte[] bytes = new byte[len];
        for(int i=0;i<len;i++){
            bytes[i]='a';
        }
        return bytes;
    }

    private static void testWithoutBuffer(String filename, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename)){
            while (flag) {
                fos.write(data);
            }
            fos.flush();
        }
        System.out.println("output without buffer done. size = " + new File(filename).length());
    }

    private static void testWithBuffer(String filename, byte[] data) throws IOException {
        try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filename)/*, 16*1024*/)) {
            while (flag) {
                bos.write(data);
            }
            bos.flush();
        }
        System.out.println("output with buffer done.    size = " + new File(filename).length());
    }
}
