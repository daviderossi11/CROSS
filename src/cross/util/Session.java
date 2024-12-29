package cross.util;

import java.io.IOException;
import java.net.*;

public class Session {
    private final InetAddress ip;
    private final int port = 5000;
    private DatagramSocket socket;
    private String notification;


    public Session(InetAddress ip) {
        this.ip = ip;
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
           e.printStackTrace(); 
        }
    }

    public void close() {
        this.socket.close();
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public void sendNotification() {
        try {
            byte[] buffer = notification.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
