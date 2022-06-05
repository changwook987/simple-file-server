package kr.hs.dgsw.network.test01.n2118.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;

public class Server {
    public static void main(String[] args) {
        Server.FILES.mkdirs();

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    InetAddress address = socket.getInetAddress();

                    ClientThread clientThread = clients.get(address);

                    if (clientThread != null) {
                        clientThread.interrupt();
                    }

                    clientThread = new ClientThread(socket);
                    clientThread.setDaemon(true);

                    clientThread.start();

                    clients.put(address, clientThread);

                    System.out.printf("%s 접속됨 %s\n", address, new Date());
                } catch (IOException e) {
                    System.err.println("연결 실패");
                }
            }
        } catch (IOException e) {
            System.out.println("서버를 열 수 없음");
        }
    }

    private Server() {
    }

    public static final HashMap<InetAddress, ClientThread> clients = new HashMap<>();

    public static final String ID = "admin";
    public static final String PW = "1234";

    public static final File FILES = new File("./Files");
}
