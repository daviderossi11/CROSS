package cross.server.Orderbook;
import cross.server.Order.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;




/*
 * Classe per eseguire gli ordini di acquisto di tipo Stop
 */


public class BuyStopOrderExecutor implements Runnable {

   private final Orderbook orderbook; // Riferimento all'Orderbook
   private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<StopOrder>> buyOrders; // Mappa degli ordini di acquisto
   private final static String FILE_PATH = "files/buyStopOrders.json"; // Percorso del file JSON contenente gli ordini di acquisto
   private final static Gson gson = new GsonBuilder().disableHtmlEscaping().create(); // Gson per la serializzazione/deserializzazione degli oggetti
   private final Object lock = new Object();  // Semaforo per vedere se la mapppa è vuota
   private AtomicInteger price; // Prezzo corrente per attivare gli ordini
   private volatile boolean running = true; // Flag per terminare il thread

    public BuyStopOrderExecutor(Orderbook orderbook, AtomicInteger price) {
        this.orderbook = orderbook;
        this.price = price;
        this.buyOrders = new ConcurrentSkipListMap<>();
        caricaBuyStopOrders();
    }


    // Metodo per caricaree l'orderbook di stop dal file
    private void caricaBuyStopOrders() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                if (jsonObject.has("buyOrders")) {
                    Type mapType = new TypeToken<ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<StopOrder>>>(){}.getType();
                    buyOrders = gson.fromJson(jsonObject.get("buyOrders"), mapType);
                }
                if (buyOrders == null) buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder());
                System.out.println("BuyStopOrders caricati da file.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    // Metodo per prendere l'ultimo id degli ordini di acquisto
    public int getBuyStopOrderId() {
        return buyOrders.values().stream()
            .flatMap(queue -> queue.stream().map(StopOrder::getId))
            .max(Integer::compare)
            .orElse(0);
    }
    


    // Metodo per salvare l'orderbook di stop nel file
    private void salvaBuyStopOrders() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write("{\n\"buyOrders\": {\n");

            // Scriviamo gli ordini di acquisto (buyOrders)
            Iterator<Map.Entry<Integer, ConcurrentLinkedQueue<StopOrder>>> buyIterator = buyOrders.entrySet().iterator();
            while (buyIterator.hasNext()) {
                Map.Entry<Integer, ConcurrentLinkedQueue<StopOrder>> entry = buyIterator.next();
                int price = entry.getKey();
                ConcurrentLinkedQueue<StopOrder> orders = entry.getValue();

                writer.write("\"" + price + "\": [\n");

                Iterator<StopOrder> orderIterator = orders.iterator();
                if (orderIterator.hasNext()) {
                    writer.write(gson.toJson(orderIterator.next()));
                }

                while (orderIterator.hasNext()) {
                    writer.write(",\n" + gson.toJson(orderIterator.next()));
                }

                writer.write("\n]");

                if (buyIterator.hasNext()) {
                    writer.write(",\n");
                }
            }

            writer.write("\n}}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo per aggiungere un ordine di acquisto di tipo Stop
    public int addBuyStopOrder(StopOrder order) {
        synchronized (lock) {
            buyOrders.computeIfAbsent(order.getPrice(), k -> new ConcurrentLinkedQueue<>()).add(order);
            lock.notifyAll();
        }
        return order.getId();
    }


    // Metodo per rimuovere un ordine di acquisto di tipo Stop
    public boolean removeBuyStopOrder(int id, String username) {
        return buyOrders.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(order -> order.getId() == id && order.getUsername().equals(username));
            return entry.getValue().isEmpty(); // Se la coda è vuota, rimuoviamo la chiave
        });
    }
    

    @Override
    public void run() {
        while (running) {
            synchronized (lock) {
                while (buyOrders.isEmpty() && running) { // Attende se non ci sono ordini
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            // Esegue gli ordini di acquisto
            while (!buyOrders.isEmpty() && buyOrders.firstKey() <= price.get()) {
                ConcurrentLinkedQueue<StopOrder> orders;
                synchronized (lock) {
                    orders = buyOrders.pollFirstEntry().getValue();
                }
                for (StopOrder order : orders) {
                    orderbook.executeStopOrder(order);
                }
            }
        }
    }



    // Metodo per terminare il thread
    public void stop() {
        synchronized (lock) {
            running = false;
            lock.notifyAll();
        }
        salvaBuyStopOrders();
        buyOrders.clear();
    }
    
}
