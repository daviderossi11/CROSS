package cross.server;

import java.net.ServerSocket;
import java.net.Socket;
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
        serverSocket = new ServerSocket(1024);
        HandlerExecutor = Executors.newCachedThreadPool();
    }

    public void start() {
        while (true) {
            Socket socket = serverSocket.accept();
            ConnectionHandler handler = new ConnectionHandler(socket, userManagement, orderBook);
            HandlerExecutor.execute(handler);
        }
    }

    
}
