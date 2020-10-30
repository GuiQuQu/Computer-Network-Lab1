import wall.Wall;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @Author: Wang keLong
 * @DateTime: 12:12 2020/10/29
 */
public class ThreadHandler implements Runnable {
    private Socket clientSocket;
    private Socket remoteSocket;

    public ThreadHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * 1.获取http请求
     * 2.分析http请求
     */
    @Override
    public void run() {
        int bufferLen = 512;
        InputStream ClientToProxy = null, ServerInProxy = null;
        OutputStream OutToClient = null, OutToServer = null;
        FileOutputStream fileOutput = null;
        FileInputStream fileInput = null;
        byte[] buffer = new byte[bufferLen];
        int len;
        try {
            ClientToProxy = new BufferedInputStream(clientSocket.getInputStream());
            OutToClient = new BufferedOutputStream(clientSocket.getOutputStream());
            BufferedReader ClientToProxyReader = new BufferedReader(new InputStreamReader(ClientToProxy));
            String line = "";
            StringBuffer request = new StringBuffer();
            String host = "";
            int port = 80;
            String url = "";
            String method = "";

            /**
             * GET请求没有请求体，但是POST请求有可能有请求体，但是在此忽略，只读取请求行和头部信息
             * */
            while ((line = ClientToProxyReader.readLine()) != null) {  //readLine()遇到\r和\n停止，无法读取换行符\n和回车符\r
                if (line.equalsIgnoreCase("")) {
                    break;
                }
                if (line.contains("Proxy-Connection")) {
                    String[] temp = line.split(" ");
                    StringBuffer lineB = new StringBuffer();
                    lineB.append("Connection: ");
                    for (int i = 1; i < temp.length; i++) {
                        lineB.append(temp[i] + " ");
                    }
                    request.append(lineB.toString().trim() + "\r\n");
                } else {
                    request.append(line + "\r\n");
                }
                String[] temp = line.split(" ");
                //请求行
                if (temp[temp.length - 1].contains("HTTP/")) {
                    if (temp.length >= 3) {
                        method = temp[0];
                        url = temp[1];
                    }
                }
                //Host行
                if (temp[0].contains("Host")) {
                    host = temp[1];
                    String[] temp1 = host.split(":");
                    if (temp1.length >= 2) {
                        host = temp1[0];
                        port = Integer.parseInt(temp1[1]);
                    }
                }
            } //不包含最后的\r\n
            if (method.equalsIgnoreCase("Connect")) {
                OutToClient.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
                OutToClient.flush();
                System.out.println("Jump Connect");
                return;
            }
            System.out.println("解析结果:\nmethod:" + method + "\nhost:" + host + "\nport:" + port + "\nurl:" + url);
            //网站过滤+用户过滤+网站引导
            Wall wall = new Wall();
            if (wall.isForbiddenHost(clientSocket.getLocalAddress().getHostAddress())) {
                OutToClient.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
                OutToClient.flush();
                return;
            }
            if (wall.isForbiddenUrl(url)) {
                OutToClient.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
                OutToClient.flush();
                return;
            }

            File cache = new File("Cache\\" + url.replace("\\", " ")
                    .replace("/", " ").replace(":", " ")
                    .replace("?", "_") + "0.cache");
            if (cache.exists() && cache.length() > 0) {
                //获取最后修改时间
                String lastModified = "";
                BufferedReader tempInput = new BufferedReader(new InputStreamReader(new FileInputStream(cache)));
                String fileLine = "";
                while ((fileLine = tempInput.readLine()) != null) {
                    if (fileLine == "") break;
                    if (fileLine.contains("Last-Modified")) {
                        String[] strings = fileLine.split(" ");
                        lastModified = strings[1];
                    }
                }
                if (!lastModified.equals("")) {
                    request.append("If-Modified-Since: " + lastModified + "\r\n");
                }
                //关闭文件流
                tempInput.close();
            } else {
                cache.createNewFile();
            }
            request.append("\r\n");
            System.out.println(request.toString());
            if (wall.isFishing(host, url) != null) {
                String[] temp = wall.isFishing(host, url);
                //替换报文中的host和url
                //url基本是第一个遇到的
                request.replace(request.indexOf(url), request.indexOf(url) + url.length(), temp[1]);
                int start = request.indexOf(host, request.indexOf("Host:"));
                int end = start + host.length();
                request.replace(start, end, temp[0]);
                host = temp[0];
                url = temp[1];

            }
            System.out.println("读取请求消息:\n" + request);
            //获得和服务器的连接
            remoteSocket = new Socket(host, port);
            ServerInProxy = new BufferedInputStream(remoteSocket.getInputStream());
            OutToServer = new BufferedOutputStream(remoteSocket.getOutputStream());
            //发送请求报文
            System.out.println("送向服务器请求报文:");
            System.out.println(request);
            OutToServer.write(request.toString().getBytes());
            OutToServer.flush();
            //转发数据
            fileInput = new FileInputStream(cache);
            fileOutput = new FileOutputStream(cache);
            len = ServerInProxy.read(buffer);
            String firBuffer = new String(buffer, 0, len);
            System.out.println("服务器返回数据:\n" + firBuffer);
            if (firBuffer.contains("304")) {  //可以使用缓存
                len = fileInput.read(buffer);
                while (len != -1) {
                    OutToClient.write(buffer);
                    len = fileInput.read(buffer);
                }
                OutToClient.flush();
            } else {  //不可以使用缓存
                while (len != -1) {
                    OutToClient.write(buffer);
                    fileOutput.write(buffer);
                    len = ServerInProxy.read(buffer);
                }
                OutToClient.flush();
                fileOutput.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭套接字和流
            try {
                if (OutToClient != null) {
                    OutToClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (OutToServer != null) {
                    OutToServer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (ServerInProxy != null) {
                    ServerInProxy.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (ClientToProxy != null) {
                    ClientToProxy.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fileInput != null) {
                    fileInput.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fileOutput != null) {
                    fileOutput.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (remoteSocket != null) {
                    remoteSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
