// UdpHandler.java
package cross.client;

import java.io.*;
import java.net.*;

public class UdpHandler implements Runnable {
    private DatagramSocket socket;
    private final byte[] buffer;
    private String lastConsoleOutput = ""; // Per memorizzare l'ultimo output scritto
    private volatile boolean running = true; // Flag per terminazione

    public UdpHandler() {
        try {
            socket = new DatagramSocket(1024);
        } catch (SocketException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            System.exit(1);
        }
        buffer = new byte[1024];
    }

    @Override
    public void run() {
        try {
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String notification = new String(packet.getData(), 0, packet.getLength());
                synchronized (this) {
                    // Riscrive ciò che era sulla console prima
                    System.out.print("\033[H\033[2J"); // Pulisce lo schermo
                    System.out.flush();
                    System.out.println("Notification received: " + notification);
                    System.out.print(lastConsoleOutput);
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error: " + e.getMessage());
            }
        } finally {
            socket.close();
        }
    }

    public synchronized void saveConsoleOutput(String output) {
        lastConsoleOutput = output;
    }

    public void stop() {
        running = false;
        socket.close();
    }
}