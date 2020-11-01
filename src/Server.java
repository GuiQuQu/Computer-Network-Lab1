

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Author: Wang keLong
 * @DateTime: 22:37 2020/10/28
 */
public class Server {
    ServerSocket serverSocket;

    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public void listen() throws IOException {
        while (true) {
            Socket socket = serverSocket.accept();  //监听得到与客户端的接口
            //new Thread(new xxx.HandleThread(socket)).start();
            new Thread(new ThreadHandler(socket)).start();
        }

    }
}
