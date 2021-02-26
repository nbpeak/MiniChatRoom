package org.dongnaoedu.net.bio;

import org.dongnaoedu.net.Util;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

import static org.dongnaoedu.net.Util.*;

/**
 * 聊天室服务端
 */
public class ChatRoomService {
    static ExecutorService executors = Executors.newCachedThreadPool();
    static Map<String, Client> clients = new ConcurrentHashMap();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(9081);
            Util.log("服务端已启动，等待客户端连接....");
            while (true) {
                Socket socket = serverSocket.accept();// 等待客户端进行连接
                waitClientSendMsg(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 在线程中等待客户端发送消息
     * @param socket
     */
    public static void waitClientSendMsg(Socket socket) {
        Util.log(Util.getRemoteAddress(socket) + " - 已连接！");
        execute(() -> {
            try {
                L_WHILE:
                while (true) {
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    int opType = dis.read();// 操作类型
                    switch (opType) {
                        case 10: // 注册
                            register(socket);
                            break;
                        case 11: // 发送消息
                            transmitMsg(socket);
                            break;
                        default: // 下线
                            break L_WHILE;
                    }
                }
            } catch (IOException e) {
            } finally {
                clientLeave(socket);
            }
        });
    }

    /**
     * 转发消息
     * @param socket 客户端连接
     */
    private static void transmitMsg(Socket socket) {
        try {
            // 取出客户端发送的消息
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String name = readString(dis);
            int len = dis.readInt();
            byte[] msgData = new byte[len];
            dis.read(msgData, 0, len);
            byte[] nameData = name.getBytes(StandardCharsets.UTF_8);

            // 将客户端发送的消息转发给其它客户端
            clients.keySet().stream()
                    .filter(key -> !name.equals(key)) // 过滤掉客户端自己
                    .map(clients::get) // 获取要接收消息的客户端对象
                    .map(Client::getSocket) // 获取客户端的连接
                    .forEach(client -> {// 遍历每个客户端连接，开始转发消息
                        try {
                            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                            dos.writeInt(nameData.length);
                            dos.write(nameData);
                            dos.writeInt(msgData.length);
                            dos.write(msgData);
                        } catch (IOException e) {
                        }
                    });
        } catch (IOException e) {
        }
    }

    /**
     * 注册网络连接
     * @param socket 连接对象
     * @throws IOException
     */
    private static void register(Socket socket) throws IOException {
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        String name = readString(dis); // 读出注册者的姓名
        // 放入Map保存，姓名已存在就会返回已存在的客户端对象，
        // 不存在就会把当前客户端对象放入Map，并返回null
        Client client = clients.putIfAbsent(name, new Client(name, socket));
        if (client != null) { // 表示要注册的姓名已经存在
            writeInt(dos, 2);
        } else {
            writeInt(dos, 1);
        }
    }

    /**
     * 客户端离开
     * @param socket 客户端连接
     */
    public static void clientLeave(Socket socket) {
        try {
            // 找到连接对于的名称
            String clientName = clients.values().stream()
                    .filter(client -> client.getSocket().equals(socket))
                    .map(Client::getName)
                    .findFirst().orElse(null);
            if (clientName != null) {
                // 将名称从Map中移除
                clients.remove(clientName);
            }
        } finally {
            try {
                socket.close(); // 关闭客户端连接
            } catch (IOException e) {
            }
            System.out.println("客户端已退出：" + socket);
        }
    }

    public static void execute(Runnable runnable) {
        executors.execute(runnable);
    }
}
