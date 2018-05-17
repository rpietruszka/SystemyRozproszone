package pl.edu.agh.sr;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class ClientThread implements Runnable {

    private Socket clientSocket;

    private String clientNick;
    private BufferedReader in;
    private PrintWriter out;

    private int udpPort = -1;
    private Chat chat;

    public ClientThread(String clientNick, Socket clientSocket, Chat chat) {
        this.chat = chat;
        this.clientNick = clientNick;
        this.clientSocket = clientSocket;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String msg = "CLIENT_NAME:"+clientNick;
            sendMsg(msg);
            while (true) {
                msg = in.readLine();
                if (msg != null) {
                    //Informacja o porcie UDP usera jest przesyłana z przedrostkiem "UDP:"
                    if(msg.contains("UDP:")) {
                        udpPort = Integer.valueOf(msg.split(":")[2]);
                    } else {
                        System.out.println(msg);
                        chat.pushMsg(msg, clientSocket);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        out.println(msg);
    }

    public DatagramPacket formatDatagram(byte[] messageBuffer) {
        return new DatagramPacket(messageBuffer, messageBuffer.length,
                                clientSocket.getInetAddress(), udpPort);
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getClientNick() {
        return clientNick;
    }
}
