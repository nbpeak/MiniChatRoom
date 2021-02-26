package org.dongnaoedu.net.bio;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import static org.dongnaoedu.net.Util.*;

/**
 * 聊天室客户端
 */
public class ChatRoomClient {
    private final static String END_STRING = "<<<"; // 多行输入的结束符号

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        log("正在连接聊天室服务器......");
        try {
            Socket socket = new Socket("120.25.203.154", 9081);
            // 注册jvm退出时的钩子线程，用于在jvm退出时，关闭网络连接
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }));
            log("服务器连接成功！");
            String name = "";
            do {
                System.out.print("请输入你的姓名：");
                if (scanner.hasNextLine()) {
                    name = scanner.nextLine();
                }
                int retCode = registName(name, socket);
                if (retCode == 2) {
                    log("聊天室已经有" + name + "了，你换个名字吧");
                    continue;
                }
                break;
            } while (true);

            log(name + "你好！");

            // 在线程中读消息
            readStart(socket.getInputStream());

            // 开始输入消息发送
            writeStart(name, socket.getOutputStream());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册姓名
     * @param name 姓名
     * @param socket 客户端连接
     * @return 注册结果：1-正常，2-姓名已存在
     * @throws IOException
     */
    private static int registName(String name, Socket socket) throws IOException {
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.write(10);
        writeString(dos, name);

        DataInputStream dis = new DataInputStream(socket.getInputStream());
        return dis.readInt();
    }

    /**
     * 开始输入消息
     *
     * @param name         消息发送人
     * @param outputStream 消息输出的流对象
     * @throws IOException
     */
    private static void writeStart(String name, OutputStream outputStream) throws IOException {
        Scanner scanner = new Scanner(System.in);
        DataOutputStream os = new DataOutputStream(outputStream);
        System.out.println("你可以在这里输入要发送的内容(" + END_STRING + "表示输入结束)：");
        do {// 多行输入，遇到最末尾是结束符就结束输入，然后发送消息
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                String str = scanner.nextLine();
                if ("exit".equals(str.trim())) {// 输入的是exit就退出
                    return;
                } else if (str.endsWith(END_STRING)) {
                    content.append(str, 0, str.length() - END_STRING.length());
                    break;
                }
                content.append(str);
                content.append(System.getProperty("line.separator"));// 在每一行后面添加换行符
            }
            String msg = content.toString().trim();
            sendMsg(name, os, msg); // 发送消息
        } while (true);
    }

    /**
     * 发送消息
     *
     * @param name 发送人名称
     * @param os   输出流对象
     * @param msg  消息内容
     * @throws IOException
     */
    private static void sendMsg(String name, DataOutputStream os, String msg) throws IOException {
        if (msg.isEmpty())
            return;

        os.writeByte(11);
        writeString(os, name);
        writeString(os, msg);
    }

    /**
     * 开始读服务端传过来的消息，在一个守护线程中不停的读
     *
     * @param inputStream
     */
    private static void readStart(InputStream inputStream) {
        Thread thread = new Thread(() -> {
            try {
                DataInputStream is = new DataInputStream(inputStream);
                while (true) {
                    readForMsg(is);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 从流中读消息了打印
     *
     * @param is
     * @throws IOException
     */
    private static void readForMsg(DataInputStream is) throws IOException {
        String name = readString(is);
        String msg = readString(is);
        printMsg(name, msg);
    }
}
