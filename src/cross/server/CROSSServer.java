package cross.server;


import cross.order.*;
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

    private final AtomicInteger orderId;
    private final AtomicInteger price;


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
        
        PriorityBlockingQueue<Order> queue = new PriorityBlockingQueue<>();
        OrderBook orderBook = new OrderBook(queue, price);
        Thread orderBookThread = new Thread(orderBook);
        orderBookThread.start();

        CheckStopOrder checkStopOrder = new CheckStopOrder(queue, price);
        Thread checkStopOrderThread = new Thread(checkStopOrder);
        checkStopOrderThread.start();

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(socket, userManagement, price, orderId, orderBook, checkStopOrder, storicoOrdini);
                HandlerExecutor.execute(handler);
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e);
            }
        }
    }

    
}
