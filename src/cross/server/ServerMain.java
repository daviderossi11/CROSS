package cross.server;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cross.server.Orderbook.*;
import cross.server.User.UserManager;
import cross.server.handler.ConnectionHandler;
import cross.server.handler.StoricoOrdiniHandler;

import java.io.*;

public class ServerMain {


    // PER IL CARICMENTO DEI FILE
    private static final Object lock = new Object();
    private static final AtomicBoolean loading = new AtomicBoolean(true);

    private static AtomicBoolean running = new AtomicBoolean(true);
    private static ExecutorService workerPool;
    private static ExecutorService threadPool;
    private static ExecutorService schedluedPool;
    private static ServerSocket serverSocket;

    private static Orderbook orderbook;
    private static StoricoOrdiniHandler storicoOrdiniHandler;
    private static UserManager userManager;
    private static BuyStopOrderExecutor buyStopOrderExecutor;
    private static SellStopOrderExecutor sellStopOrderExecutor;

    public static void main(String[] args) {

        // Carica configurazione
        Properties config = ReadConfig("config/server.properties");

        // Inizializza risorse
        String HOST = config.getProperty("HOST", "localhost");
        int PORT = Integer.parseInt(config.getProperty("PORT", "8080"));
        int TIMEOUT = Integer.parseInt(config.getProperty("TIMEOUT", "60"));
        int MAX_CLIENTS = Integer.parseInt(config.getProperty("MAX_CLIENTS", "0"));


        // Carico gli ordini dal file
        storicoOrdiniHandler = new StoricoOrdiniHandler();

        // Prendo l'ultimo prezzo registrato
        AtomicInteger price = new AtomicInteger(storicoOrdiniHandler.getUltimoPrezzo());

        // carico l'orderbook
        orderbook = new Orderbook(price, storicoOrdiniHandler);

        // Carico gli utenti dal file
        userManager = new UserManager();

        // Inizializzo i thread per la gestioe dell'orderbook di stoporders
        buyStopOrderExecutor = new BuyStopOrderExecutor(orderbook, price);
        sellStopOrderExecutor = new SellStopOrderExecutor(orderbook, price);


        // Prendo l'ultimo id registrato
        int orderId = Math.max(
            Math.max(buyStopOrderExecutor.getBuyStopOrderId(), sellStopOrderExecutor.getSellStopOrderId()),
            Math.max(orderbook.getOrderid(), storicoOrdiniHandler.getOrderid())
        );
        AtomicInteger orderid = new AtomicInteger(orderId);


        // Avvio i thread degli stoporders
        workerPool = Executors.newFixedThreadPool(2);
        workerPool.execute(buyStopOrderExecutor);
        workerPool.execute(sellStopOrderExecutor);


        // Avvio thread per il salvataggio periodico dei dati
        schedluedPool = Executors.newScheduledThreadPool(2);
        ((ScheduledExecutorService) schedluedPool).scheduleWithFixedDelay(() -> {
            storicoOrdiniHandler.salvaStoricoOrdini();
        }, 0, 2, TimeUnit.MINUTES);

        ((ScheduledExecutorService) schedluedPool).scheduleWithFixedDelay(() -> {
            userManager.saveUsers();
        }, 0, 2, TimeUnit.MINUTES);


        // Creo il thread pool per i client
        threadPool = Executors.newCachedThreadPool();


        // Disattivo la lock sui segnali
        synchronized (lock) {
            loading.set(false); 
            lock.notifyAll();  
        }

        System.out.println("Server inizializzato.");

        try {


            // Avvio il scoket del server
            serverSocket = new ServerSocket(PORT, MAX_CLIENTS, InetAddress.getByName(HOST)); 
            System.out.println("Server started on " + HOST + ":" + PORT);

            while (running.get()) {
                try {

                    // Accetto la connessione
                    Socket clientSocket = serverSocket.accept();

                    // Imposto il timeout
                    clientSocket.setSoTimeout(TIMEOUT*1000);

                    // Creo il thread per la connessione
                    ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, running, userManager, orderbook, buyStopOrderExecutor, sellStopOrderExecutor, orderid, storicoOrdiniHandler);
                    threadPool.execute(connectionHandler);
                } catch (SocketException e) {
                    if (running.get()) {
                        System.err.println("Errore durante l'accettazione della connessione.");
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    // shutdown Thread
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                synchronized (lock) {
                    if (loading.get()) {
                        System.out.println("Server in avvio, attendere il completamento del caricamento...");
                        try {
                            lock.wait();
                        } catch (InterruptedException ignored) {}
                    }
                }
                shutdown();
            }
        });
    }

    // shutdown method
    public static void shutdown() {
        running.set(false);

        System.out.println("Arresto dei servizi in corso...");

        // Chiudi il ServerSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("ServerSocket chiuso.");
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura del ServerSocket.");
                e.printStackTrace();
            }
        }

        // Chiudi i thread pool
        stopExecutor(threadPool, "ThreadPool");
        stopExecutor(schedluedPool, "ScheduledPool");

        // Arresta i worker thread
        buyStopOrderExecutor.stop();
        sellStopOrderExecutor.stop();

        stopExecutor(workerPool, "WorkerPool");

        // Salva i dati finali
        storicoOrdiniHandler.close();
        userManager.close();
        orderbook.close();

        System.out.println("Tutte le operazioni completate. Server spento correttamente.");
    }

    // Metodo per chiudere i thread pool in modo sicuro
    private static void stopExecutor(ExecutorService executor, String name) {
        if (executor == null) return;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                System.out.println(name + " non terminato, forzando lo shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }


    // Metodo per leggere il file di configurazione
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
