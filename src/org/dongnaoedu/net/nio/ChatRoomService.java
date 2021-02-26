package org.dongnaoedu.net.nio;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.dongnaoedu.net.Util.*;

public class ChatRoomService {
    private static final Map<String, SocketChannel> clients = new ConcurrentHashMap();

    public static void main(String[] args) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            serverSocketChannel.bind(new InetSocketAddress(8080));

            log("服务端已启动，等待客户端连接。。。");
            while (true) {
                int readyKeys = selector.select();
                if (readyKeys == 0) {
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (selectionKey.isAcceptable()) {
                        acceptClient(selectionKey);
                    } else if (selectionKey.isReadable()) {
                        readData(((SocketChannel) selectionKey.channel()), ((ByteBuffer) selectionKey.attachment()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readData(SocketChannel channel, ByteBuffer buffer) {
        buffer.clear();
        try {
            while (true) {
                int len = channel.read(buffer);
                if (len == 0) {
                    break;
                } else if (len == -1) {
                    throw new EOFException();
                }

                transmitMsg(channel, buffer);
            }
        } catch (IOException e) {
//            e.printStackTrace();
            try {
                leave(channel);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private static void transmitMsg(SocketChannel channel, ByteBuffer buffer) throws IOException {
        buffer.flip();
        buffer.mark();
        String clientAddress = getClientAddress(channel);
        List<SocketChannel> channels = clients.keySet().stream()
                .filter(key -> !key.equals(clientAddress))
                .map(clients::get)
                .collect(Collectors.toList());
        for (SocketChannel clientChannel : channels) {
            try {
                while (buffer.hasRemaining()) {
                    clientChannel.write(buffer);
                }
                buffer.reset();
            } catch (IOException e) {
                leave(clientChannel);
            }
        }
    }

    private static void leave(SocketChannel channel) throws IOException {
        String clientAddress = getClientAddress(channel);
        clients.remove(clientAddress);
        channel.close();
        log(clientAddress + " 离开了！");
    }

    private static void acceptClient(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = channel.accept();
        socketChannel.configureBlocking(false);
        String clientAddress = getClientAddress(socketChannel);
        socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(8 * 1024));
        clients.putIfAbsent(clientAddress, socketChannel);
        log(clientAddress + " 加入聊天室！");
    }

    private static String getClientAddress(SocketChannel socketChannel) throws IOException {
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        return String.format("%s:%d", remoteAddress.getAddress().getHostAddress(), remoteAddress.getPort());
    }
}
