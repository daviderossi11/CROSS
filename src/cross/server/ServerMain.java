package cross.server;

import cross.handler.*;
import cross.order.*;
import cross.orderbook.*;
import cross.user.*;
import cross.util.*;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ServerMain {

    private static int PORT;
    private static int THREAD_POOL_SIZE;
    private static int inactivityTimeout;
    private static int saveTime;

    private static volatile boolean running = true;

    public static void main(String[] args) {

        // Load Config
        try {
            Properties config = ReadConfig("server.properties");
            PORT = Integer.parseInt(config.getProperty("port", "8080"));
            THREAD_POOL_SIZE = Integer.parseInt(config.getProperty("thread_pool_size", "10"));
            inactivityTimeout = Integer.parseInt(config.getProperty("inactivity_timeout", "60"));
            saveTime = Integer.parseInt(config.getProperty("save_time", "60"));
            System.out.println("Server configuration loaded.");
        } catch (IOException e) {
            System.out.println("Error loading server configuration.");
            e.printStackTrace();
            System.exit(-1);
        }

        // Thread Pool and Queues
        ScheduledExecutorService savingService = Executors.newScheduledThreadPool(1);
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        ExecutorService HandlerThreadPool = Executors.newFixedThreadPool(3);
        PriorityBlockingQueue<Order> orderQueue = new PriorityBlockingQueue<>();
        PriorityBlockingQueue<Session> notificationQueue = new PriorityBlockingQueue<>();

        // Order Book and Handlers
        StoricoOrdini storicoOrdini = new StoricoOrdini();
        AtomicInteger currentPrice = new AtomicInteger(storicoOrdini.getLastPrice());
        AtomicInteger orderId = new AtomicInteger(storicoOrdini.getOrderid());


        // User Management
        UserManager userManager = new UserManager();
        userManager.caricoUtenti();


        OrderBook orderBook = new OrderBook(orderQueue, currentPrice, notificationQueue, storicoOrdini, userManager);
        CheckStopOrder checkStopOrder = new CheckStopOrder(orderQueue, currentPrice);

        // Save Data

        savingService.scheduleAtFixedRate(() -> {
            System.out.println("Saving data...");
            storicoOrdini.SalvaOrdini();
            userManager.salvaUtenti();
        }, saveTime, saveTime, TimeUnit.SECONDS);


        // Notification Handler
        NotificationHandler notificationHandler = new NotificationHandler(notificationQueue);

        // Start Handlers
        HandlerThreadPool.execute(notificationHandler);
        HandlerThreadPool.execute(orderBook);
        HandlerThreadPool.execute(checkStopOrder);

        // Start Server Socket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            // Main Server Loop
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection accepted.");

                clientSocket.setSoTimeout(inactivityTimeout * 1000);

                // Create and execute a connection handler
                ConnectionHandler handler = new ConnectionHandler(
                        clientSocket,
                        userManager,
                        orderBook,
                        checkStopOrder,
                        storicoOrdini,
                        currentPrice,
                        orderId,
                        orderQueue
                );
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdownServer(threadPool, HandlerThreadPool, savingService, orderQueue, notificationQueue, storicoOrdini, userManager);
        }
    }

    private static void shutdownServer(ExecutorService pool, ExecutorService HandlerPool, ScheduledExecutorService savingService, PriorityBlockingQueue<Order> orderQueue, PriorityBlockingQueue<Session> notificationQueue, StoricoOrdini storicoOrdini, UserManager userManager) {
        System.out.println("Shutting down server...");
        running = false;

        // Shutdown Connection Handlers

        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }
        savingService.shutdown();
        try {
            if (!savingService.awaitTermination(10, TimeUnit.SECONDS)) {
                savingService.shutdownNow();
            }
        } catch (InterruptedException e) {
            savingService.shutdownNow();
        }
        HandlerPool.shutdown();
        try {
            if (!HandlerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                HandlerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            HandlerPool.shutdownNow();
        }


        // Save Data
        System.out.println("Saving data...");
        storicoOrdini.SalvaOrdini();
        userManager.salvaUtenti();

        // Shutdown Order Book
        orderQueue.clear();
        notificationQueue.clear();
        storicoOrdini.clear();
        userManager.clear();

        System.out.println("Server shutdown complete.");

    
    }
    // Graceful Termination Handler
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered.");
            running = false;
        }));
    }

    
    public static Properties ReadConfig(String fileName) throws IOException {
    Properties properties = new Properties();
    try (FileReader reader = new FileReader(fileName)) {
        properties.load(reader);
        }
    return properties;
    }
}
