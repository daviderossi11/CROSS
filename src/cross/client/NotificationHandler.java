package cross.client;

import java.io.*;
import java.net.*;

public class NotificationHandler implements Runnable {

    private DatagramSocket socket;
    private final byte[] buffer;
    private final PrintStream out;


    public NotificationHandler(PrintStream out) {
        try {
            socket = new DatagramSocket(1024);
        } catch (SocketException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            System.exit(1);
        }
        buffer = new byte[1024];
        this.out = out;
    }

    @Override
    public void run() {
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String notification = new String(packet.getData(), 0, packet.getLength());
                printNotification(notification);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e);
        } finally {
            socket.close();
        }
    }

    private synchronized void printNotification(String notification) {
        out.println();
        out.println("Notification received: " + notification);
        out.print("> "); // Prompt symbol to indicate user can continue typing
    }
}
