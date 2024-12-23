package cross.util;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class Session {
    private DatagramSocket socket;
    private final int userId;
    private final ArrayList<Integer> activeOrders;
    private final InetAddress address;

    public Session(int userId, InetAddress address) {
        this.userId = userId;
        this.address = address;
        this.activeOrders = new ArrayList<>();

        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Error creating socket: " + e.getMessage());
        }
    }

    public int getUserId() {
        return userId;
    }

    public ArrayList<Integer> getActiveOrders() {
        return activeOrders;
    }

    public void addActiveOrder(int orderId) {
        activeOrders.add(orderId);
    }
    
    
    public void sendNotification(String notification) {
        byte[] buffer = notification.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 1024);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending notification: " + e.getMessage());
        }
    }

    
}
