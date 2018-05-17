package pl.edu.agh.sr;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Client {
    public Executor executor;
    private String nick;
    private int serverPort = -1;

    private final String multicastAddress = "224.222.11.96";
    private final int multicastPort = 7777;
    private Socket socket;
    private DatagramSocket datagramSocket;
    private MulticastSocket multicastSocket;

    private PrintWriter out;
    private BufferedReader in;



    public Client(int port) throws IOException {
        executor = Executors.newFixedThreadPool(4);
        serverPort = port;

        socket = new Socket(InetAddress.getLocalHost(), serverPort);
        datagramSocket = new DatagramSocket();

        multicastSocket = new MulticastSocket(multicastPort);
        multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));

        initIO();

        initTcpListener();
        initDatagramListener();
        initMulticastListener();
    }

    private void initIO() throws IOException {
        setOut(new PrintWriter(getSocket().getOutputStream(), true));
        setIn(new BufferedReader(new InputStreamReader(getSocket().getInputStream())));
    }

    public void initTcpListener() {
        executor.execute(() -> {
            String msg;
            while (true) {
                try {
                    msg = in.readLine();
                    if (msg.startsWith("CLIENT_NAME:")) {
                        nick = (msg.split(":")[1]);
                        System.out.println("Connected.");
                    } else {
                        System.out.println("[TCP]: " + msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void initDatagramListener() {
        executor.execute(()-> {
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (true) {
                try {
                    datagramSocket.receive(packet);

                    System.out.println("[UDP]: " + new String(packet.getData(), 0, packet.getLength()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void initMulticastListener() {
        byte[] buf = new byte[1024];
        executor.execute(()->{
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while(true) {
                try {
                    multicastSocket.receive(packet);
                    System.out.println("[MLT]: " +new String(packet.getData(),0,packet.getLength()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendMsg(String msg) {
        out.println(nick + ':' + msg);
    }

    public void sendDatagramMsg(String msg) {
        byte[] bufer = (nick + ':' + msg).getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(bufer, bufer.length,
                    socket.getRemoteSocketAddress());
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMulticastMsg(String msg) {
        byte[] bufer = (nick + ':' + msg).getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(bufer, bufer.length,
                    InetAddress.getByName(multicastAddress), multicastPort);
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Client c = new Client(9009);
            c.tellServerUdpPort();
            String msg;
            while (true) {
                msg = new BufferedReader(new InputStreamReader(System.in)).readLine();
                if(msg.length() > 0) {
                    switch(msg.charAt(0)) {
                        case 'U':
                            c.sendDatagramMsg(msg.substring(1));
                            break;
                        case 'M':
                            c.sendMulticastMsg(msg.substring(1));
                            break;
                        default:
                            c.sendMsg(msg);
                            break;
                    }
                }
                msg = "";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void tellServerUdpPort() {
        sendMsg("UDP:" + datagramSocket.getLocalPort());
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public Socket getSocket() {
        return socket;
    }

}
