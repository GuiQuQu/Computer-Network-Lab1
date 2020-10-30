import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Author: Wang keLong
 * @DateTime: 22:37 2020/10/28
 */
public class main {
    public static void main(String[] args) {
        int port = 7890;
        try {
            ServerSocket server = new ServerSocket(port);
            while (true) {
                Socket socket = server.accept();
                new Thread(new ThreadHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
