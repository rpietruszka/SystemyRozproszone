package pl.edu.agh.sr;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Chat {

    int clientID = 0;
    private ConcurrentMap<String, ClientThread> clientsRegister;

    private ServerSocket serverTcpSocket;
    private DatagramSocket serverDatagramSocket;

    private Executor serverExecutor;
    private final int poolSize = 10;
    private final int bufferSize = 1024;

    public Chat(int port) throws IOException{
        clientsRegister = new ConcurrentHashMap<>();
        serverTcpSocket = new ServerSocket(port);
        serverDatagramSocket = new DatagramSocket(port);

        initExecutor();

        System.out.println(String.format("[Running server] Address: %s:%d",
                serverTcpSocket.getInetAddress().toString(), serverTcpSocket.getLocalPort()));
    }

    private void initDatagramCommunication() {
        serverExecutor.execute(() -> {
                    while(true) {
                        byte[] buffer = new byte[bufferSize];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        try {
                            serverDatagramSocket.receive(packet);
                            pushDatagramMsg(new String(packet.getData(), 0, packet.getLength()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    public synchronized void pushDatagramMsg(String msg) {
        String sender = msg.split(":")[0];
        byte[] bufer = msg.getBytes();
        clientsRegister.values()
                .forEach(client -> {
                    if(!client.getClientNick().equals(sender)){
                        try {
                            serverDatagramSocket.send(client.formatDatagram(bufer));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
    }

    private void acceptIncomingTcpConnection() {
        serverExecutor.execute(() -> {
            while (true) {
                String clientNick = "USER"+clientID++;
                try {
                    Socket clientSocket = serverTcpSocket.accept();
                    ClientThread clientThread = new ClientThread(clientNick, clientSocket, this);
                    clientsRegister.put(clientNick, clientThread);
                    serverExecutor.execute(clientThread);
                    System.out.println("New client accepted");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public synchronized void pushMsg(String msg, Socket senderSocket) {
        clientsRegister.values().forEach(client -> {
            if(!client.getClientSocket().equals(senderSocket))
                client.sendMsg(msg);
        });
    }

    private void initExecutor() {
        serverExecutor = Executors.newFixedThreadPool(poolSize);
    }

    public static void main(String[] args) {
        try {
            Chat chat = new Chat(9009);
            chat.acceptIncomingTcpConnection();
            chat.initDatagramCommunication();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
