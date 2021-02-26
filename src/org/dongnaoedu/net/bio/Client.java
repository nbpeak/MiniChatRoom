package org.dongnaoedu.net.bio;

import java.net.Socket;

/**
 * 客户端对象，用于记录客户端姓名和连接的关系
 */
public class Client {
    private String name;

    private Socket socket;

    public Client(String name, Socket socket) {
        this.name = name;
        this.socket = socket;
    }

    public String getName() {
        return name;
    }

    public Socket getSocket() {
        return socket;
    }
}
