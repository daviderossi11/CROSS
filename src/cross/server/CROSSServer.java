package cross.server;

import cross.user.UserManagement;
import cross.util.StoricoOrdini;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


public class CROSSServer {
    private final UserManagement userManagement;
    private final ServerSocket serverSocket;
    private final ExecutorService HandlerExecutor;
    private final StoricoOrdini storicoOrdini;


    public CROSSServer() {
        userManagement = new UserManagement();
        storicoOrdini = new StoricoOrdini();
        int tempPrice = storicoOrdini.getLastPrice();
        int tempOrderId = storicoOrdini.getOrderid();

        AtomicInteger orderId = new AtomicInteger(tempOrderId);
        AtomicInteger price = new AtomicInteger(tempPrice);

        try {
            serverSocket = new ServerSocket(5000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HandlerExecutor = Executors.newCachedThreadPool();
    }

    public void start() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(socket, userManagement, price, orderId);
                HandlerExecutor.execute(handler);
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e);
            }
        }
    }

    
}
