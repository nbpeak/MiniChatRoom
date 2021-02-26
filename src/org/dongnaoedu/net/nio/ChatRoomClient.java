package org.dongnaoedu.net.nio;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import static org.dongnaoedu.net.Util.*;

public class ChatRoomClient {
    public static void main(String[] args) throws IOException {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            Selector selector = Selector.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 8080));
            log("开始连接服务器！");
            while (selector.isOpen()) {
                int readyKeys = selector.select();
                if (readyKeys == 0) {
                    continue;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();

                    if (selectionKey.isConnectable()) {
                        finishConnect(selectionKey);
                    } else if (selectionKey.isReadable()) {
                        readData(((SocketChannel) selectionKey.channel()), ((ByteBuffer) selectionKey.attachment()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            log("与服务器连接断开，按任意键退出程序！");
            System.in.read();
        }
    }

    private static void readData(SocketChannel channel, ByteBuffer buffer) throws IOException {
        buffer.clear();
        while (true) {
            int len = channel.read(buffer);
            if (len == 0) {
                break;
            } else if (len == -1) {
                throw new EOFException();
            }

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            printMsg("陌生人", new String(data, StandardCharsets.UTF_8));
        }
    }

    private static void finishConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (channel.finishConnect()) {
            log("服务器连接成功！");
            channel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
            startWrite(channel, selectionKey.selector());
        }
    }

    private static void startWrite(SocketChannel channel, Selector selector) {
        new Thread(() -> {
            try {
                Scanner scanner = new Scanner(System.in);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                do {
                    System.out.print("你要说什么？");
                    String msg = "";
                    if (scanner.hasNextLine()) {
                        msg = scanner.nextLine();
                    }
                    if ("exit".equals(msg)) {
                        break;
                    }

                    buffer.put(msg.getBytes(StandardCharsets.UTF_8));
                    buffer.flip();
                    while (buffer.hasRemaining())
                        channel.write(buffer);
                    buffer.compact();
                } while (true);
            } catch (IOException e) {
//                e.printStackTrace();
            } finally {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
