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

    // Configurazioni e risorse condivise
    private static int PORT;
    private static int THREAD_POOL_SIZE;
    private static int inactivityTimeout;
    private static int saveTime;

    private static volatile boolean running = true;

    private static ScheduledExecutorService savingService;
    private static ExecutorService threadPool;
    private static ExecutorService HandlerThreadPool;
    private static PriorityBlockingQueue<Order> orderQueue;
    private static PriorityBlockingQueue<Session> notificationQueue;
    private static StoricoOrdini storicoOrdini;
    private static UserManager userManager;
    private static AtomicInteger currentPrice;
    private static AtomicInteger orderId;

    public static void main(String[] args) {

        // Carica configurazione
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

        // Inizializza risorse
        savingService = Executors.newScheduledThreadPool(1);
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        HandlerThreadPool = Executors.newFixedThreadPool(3);
        orderQueue = new PriorityBlockingQueue<>();
        notificationQueue = new PriorityBlockingQueue<>();

        storicoOrdini = new StoricoOrdini();
        currentPrice = new AtomicInteger(storicoOrdini.getLastPrice());
        orderId = new AtomicInteger(storicoOrdini.getOrderid());

        userManager = new UserManager();
        userManager.caricoUtenti();

        OrderBook orderBook = new OrderBook(orderQueue, currentPrice, notificationQueue, storicoOrdini, userManager);
        CheckStopOrder checkStopOrder = new CheckStopOrder(orderQueue, currentPrice);

        // Salvataggio periodico
        savingService.scheduleAtFixedRate(() -> {
            System.out.println("Saving data...");
            storicoOrdini.SalvaOrdini();
            userManager.salvaUtenti();
        }, saveTime, saveTime, TimeUnit.SECONDS);

        // Handler delle notifiche
        NotificationHandler notificationHandler = new NotificationHandler(notificationQueue);

        // Avvio thread di gestione
        HandlerThreadPool.execute(notificationHandler);
        HandlerThreadPool.execute(orderBook);
        HandlerThreadPool.execute(checkStopOrder);

        // Avvio del server
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New connection accepted.");

                    clientSocket.setSoTimeout(inactivityTimeout * 1000);

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
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Shutdown Hook
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered.");
            running = false;
            shutdownServer(threadPool, HandlerThreadPool, savingService, orderQueue, notificationQueue, storicoOrdini, userManager);
            System.out.println("Server shutdown complete.");
        }));
    }

    // Metodo di arresto sicuro
    private static void shutdownServer(
            ExecutorService pool,
            ExecutorService handlerPool,
            ScheduledExecutorService savingService,
            PriorityBlockingQueue<Order> orderQueue,
            PriorityBlockingQueue<Session> notificationQueue,
            StoricoOrdini storicoOrdini,
            UserManager userManager
    ) {
        System.out.println("Shutting down server...");

        // Arresto dei thread pool
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }

        handlerPool.shutdown();
        try {
            if (!handlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                handlerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            handlerPool.shutdownNow();
        }

        savingService.shutdown();
        try {
            if (!savingService.awaitTermination(5, TimeUnit.SECONDS)) {
                savingService.shutdownNow();
            }
        } catch (InterruptedException e) {
            savingService.shutdownNow();
        }

        // Salvataggio finale dei dati
        System.out.println("Saving data...");
        storicoOrdini.SalvaOrdini();
        userManager.salvaUtenti();

        // Pulizia delle code e delle risorse
        orderQueue.clear();
        notificationQueue.clear();
        storicoOrdini.clear();
        userManager.clear();

        System.out.println("Shutdown process completed.");
    }

    // Metodo per leggere i file di configurazione
    public static Properties ReadConfig(String fileName) throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(fileName)) {
            properties.load(reader);
        }
        return properties;
    }
}
