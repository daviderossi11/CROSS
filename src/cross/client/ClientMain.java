package cross.client;

import java.net.Socket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import cross.client.utils.*;

import java.io.*;

public class ClientMain {
    private static ServerListener serverListener;
    private static ExecutorService threadPool;

    public static void main(String[] args) {
        new ClientMain().startClient();
    }

    public void startClient() {

        // Carica configurazione
        Properties config = ReadConfig("config/client.properties");
        String SERVER = config.getProperty("SERVER", "localhost");
        int UDP_PORT = Integer.parseInt(config.getProperty("UDP_PORT", "8081"));
        int SERVER_PORT = Integer.parseInt(config.getProperty("SERVER_PORT", "8080"));

        threadPool = Executors.newFixedThreadPool(2);

        try {

            // Crea la connessione TCP
            Socket tcpSocket = new Socket(SERVER, SERVER_PORT);

            // Crea la connessione UDP
            DatagramSocket udpSocket = new DatagramSocket(UDP_PORT);
            UDP_PORT = udpSocket.getLocalPort();

            // Avvia la SocketTCP
            SocketTCP socketTCP = new SocketTCP(tcpSocket, UDP_PORT);
            threadPool.execute(socketTCP);

            // Avvia il ServerListener che ora gestisce sia TCP che UDP
            serverListener = new ServerListener(socketTCP, udpSocket);
            threadPool.execute(serverListener);

            SyncConsole.print("> Connessione Avviata con il server " + InetAddress.getByName(SERVER).getHostAddress() + " sulla porta " + SERVER_PORT);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // shutdown Thread
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }));
    }

    // shutdown method
    public static void shutdown() {
        if (serverListener != null) serverListener.shutdown();
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(800, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }


    // Legge il file di configurazione
    public static Properties ReadConfig(String filename) {
        Properties config = new Properties();
        try {
            config.load(new FileReader(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }
}
