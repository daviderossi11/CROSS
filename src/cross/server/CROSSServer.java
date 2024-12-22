package cross.server;

import cross.order.OrderBook;
import cross.user.UserManagement;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CROSSServer {
    private final UserManagement userManagement;
    private final OrderBook orderBook;
    private final ServerSocket serverSocket;
    private final ExecutorService HandlerExecutor;


    public CROSSServer() {
        userManagement = new UserManagement();
        orderBook = new OrderBook();
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
                ConnectionHandler handler = new ConnectionHandler(socket, userManagement, orderBook);
                HandlerExecutor.execute(handler);
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e);
            }
        }
    }

    
}
