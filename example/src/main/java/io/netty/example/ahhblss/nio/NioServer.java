package io.netty.example.ahhblss.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: Sean
 * @Date: 2019/2/25 13:55
 * @Description:
 */
public class NioServer {

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public NioServer() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        serverSocketChannel.socket().bind(new InetSocketAddress("127.0.0.1",1024));
        System.out.println("nio started");

        handleKeys();
    }

    private void handleKeys() throws IOException {

        while (true) {
            int selectNums = selector.select(30 * 1000L);
            System.out.println("select num:" + selectNums);

            if (selectNums == 0) {
                continue;
            }

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
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

        if (selectionKey.isAcceptable()) {
            handleAccept(selectionKey);
        }

        if (selectionKey.isReadable()) {
            handleRead(selectionKey);
        }

        if (selectionKey.isWritable()) {
            handleWrite(selectionKey);
        }

    }

    private void handleAccept(SelectionKey selectionKey) throws IOException {
        SocketChannel clientSocket = ((ServerSocketChannel) selectionKey.channel()).accept();
        clientSocket.configureBlocking(false);

        System.out.println("accept a new socket");

        clientSocket.register(selector, SelectionKey.OP_READ, new ArrayList<String>());
    }

    private void handleRead(SelectionKey selectionKey) throws IOException {
        SocketChannel clientSocketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer readBuffer = CodecUtil.read(clientSocketChannel);
        if (readBuffer == null) {
            System.out.println("cancel channel");
            clientSocketChannel.register(selector, 0);
            return;
        }

        if (readBuffer.position() > 0) {
            String content = CodecUtil.newString(readBuffer);
            System.out.println("read msg:" + content);
            List<String> responseQueue = (ArrayList<String>) selectionKey.attachment();
            responseQueue.add(content);

            clientSocketChannel.register(selector, SelectionKey.OP_WRITE, selectionKey.attachment());
        }
    }

    private void handleWrite(SelectionKey selectionKey) throws IOException {
        SocketChannel clientSocketChannel = (SocketChannel) selectionKey.channel();
        List<String> responseQueue = (ArrayList<String>) selectionKey.attachment();

        for (String content : responseQueue) {
            System.out.println("write msg:" + content);
            CodecUtil.write(clientSocketChannel, content);
        }

        responseQueue.clear();

        clientSocketChannel.register(selector, SelectionKey.OP_READ,responseQueue);
    }

    public static void main(String[] args) throws IOException{
        NioServer nioServer = new NioServer();
    }


}
