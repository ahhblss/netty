package io.netty.example.ahhblss.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: Sean
 * @Date: 2019/2/25 15:16
 * @Description:
 */
public class NioClient {

    private SocketChannel client;

    private Selector selector;

    private List<String> responseQueue = new ArrayList<String>();

    private CountDownLatch connected = new CountDownLatch(1);

    public NioClient() throws IOException, InterruptedException {
        client = SocketChannel.open();
        client.configureBlocking(false);

        selector = Selector.open();

        client.register(selector, SelectionKey.OP_CONNECT);

        client.connect(new InetSocketAddress("127.0.0.1", 1024));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleKeys();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if (connected.getCount() != 0) {
            connected.await();
        }

        System.out.println("client start up");

    }

    private void handleKeys() throws IOException {

        while (true) {
            int selectNum = selector.select(30 * 1000L);
            System.out.println("select num:" + selectNum);
            if (selectNum == 0) {
                continue;
            }

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            if (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();

                if (!selectionKey.isValid()) {
                    continue;
                }

                handleKey(selectionKey);
            }
        }

    }

    private void handleKey(SelectionKey selectionKey) throws IOException {

        if (selectionKey.isConnectable()) {
            handleConnectableKey(selectionKey);
        }

        if (selectionKey.isReadable()) {
            handleReadableKey(selectionKey);
        }

        if (selectionKey.isWritable()) {
            handleWritableKey(selectionKey);
        }
    }

    private void handleConnectableKey(SelectionKey selectionKey) throws IOException {
        if (!client.isConnectionPending()) {
            return;
        }

        client.finishConnect();

        System.out.println("connect success -----------------------");

        client.register(selector, SelectionKey.OP_READ, responseQueue);

        connected.countDown();
    }

    private void handleReadableKey(SelectionKey selectionKey) {
        SocketChannel clientSocket = (SocketChannel) selectionKey.channel();

        ByteBuffer readBuffer = CodecUtil.read(clientSocket);

        if (readBuffer.position() > 0) {
            String content = CodecUtil.newString(readBuffer);

            System.out.println("read msg:" + content);
        }
    }

    private void handleWritableKey(SelectionKey selectionKey) throws IOException {
        SocketChannel clientSocket = (SocketChannel) selectionKey.channel();

        List<String> responseQueue = (ArrayList<String>) selectionKey.attachment();

        for (String content : responseQueue) {

            CodecUtil.write(clientSocket, content);

            System.out.println("write msg:" + content);

        }

        responseQueue.clear();
        clientSocket.register(selector, SelectionKey.OP_READ, responseQueue);

    }

    public synchronized void send(String content) throws ClosedChannelException {
        responseQueue.add(content);
        System.out.println("queue add:" + content);

        client.register(selector, SelectionKey.OP_WRITE, responseQueue);
        selector.wakeup();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        NioClient nioClient = new NioClient();
        for (int i = 0; i < 5; i++) {
            nioClient.send("nihao " + i);
            Thread.sleep(1000);
        }
    }
}
