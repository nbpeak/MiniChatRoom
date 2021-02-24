package org.dongnaoedu.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Util {
    /**
     * 打印日志
     * @param msg
     */
    public static void log(String msg) {
        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"));
        String threadName = Thread.currentThread().getName();
        System.out.printf("%s - [%s] %s%n", formattedTime, threadName, msg);
    }

    /**
     * 打印消息
     * @param name 消息发送人
     * @param msg 消息内容
     */
    public static void printMsg(String name, String msg) {
        LocalTime now = LocalTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("%n%s %s%n%s%n%n", name, formattedTime, msg);
    }

    /**
     * 获取远程连接的IP和端口地址
     * @param socket 连接对象
     * @return
     */
    public static String getRemoteAddress(Socket socket) {
        InetSocketAddress remoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        return remoteSocketAddress.getAddress().getHostAddress() + ":" + remoteSocketAddress.getPort();
    }

    /**
     * 往流对象中写字符串
     * @param dos
     * @param msg
     * @throws IOException
     */
    public static void writeString(DataOutputStream dos, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);// 写字符串的长度
        dos.write(bytes); // 写字符串的内容
    }

    /**
     * 从流中读字符串
     * @param dis
     * @return
     * @throws IOException
     */
    public static String readString(DataInputStream dis) throws IOException {
        int len = dis.readInt(); // 读字符串的长度
        byte[] data = new byte[len];
        dis.read(data, 0, len);// 读字符串的内容
        return new String(data, StandardCharsets.UTF_8);
    }

    public static int readInt(DataInputStream dis) throws IOException {
        return dis.readInt();
    }

    public static void writeInt(DataOutputStream dos, int val) throws IOException {
        dos.writeInt(val);
    }

    public static long readLong(DataInputStream dis) throws IOException {
        return dis.readLong();
    }

    public static void writeInt(DataOutputStream dos, long val) throws IOException {
        dos.writeLong(val);
    }
}
