package cross.server.Orderbook;

import cross.server.Order.StopOrder;
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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;



public class SellStopOrderExecutor implements Runnable{

    private final Orderbook orderbook;// Riferimento all'Orderbook
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<StopOrder>> sellOrders; // Mappa degli ordini di vendita
    private final static String FILE_PATH = "files/sellStopOrders.json"; // Percorso del file JSON contenente gli ordini di vendita
    private final Object lock = new Object(); // Semaforo per vedere se la mappa è vuota
    private final static Gson gson = new GsonBuilder().disableHtmlEscaping().create(); // Gson per la serializzazione/deserializzazione degli oggetti
    private AtomicInteger price; // Prezzo corrente per attivare gli ordini
    private volatile boolean running = true; // Flag per terminare il thread

    public SellStopOrderExecutor(Orderbook orderbook, AtomicInteger price) {
        this.orderbook = orderbook;
        this.price = price;
        this.sellOrders = new ConcurrentSkipListMap<>();
        caricaSellStopOrders();
    }

    // Metodo per caricare l'orderbook di stop dal file
    private void caricaSellStopOrders() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                if (jsonObject.has("sellOrders")) {
                    Type mapType = new TypeToken<ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<StopOrder>>>(){}.getType();
                    sellOrders = gson.fromJson(jsonObject.get("sellOrders"), mapType);
                }
                if (sellOrders == null) sellOrders = new ConcurrentSkipListMap<>();
                System.out.println("SellStopOrders caricati da file.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    // Metodo per salvare l'orderbook di stop nel file
    private void salvaSellStopOrders() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write("{\n\"sellOrders\": {\n");

            // Scriviamo gli ordini di acquisto (buyOrders)
            Iterator<Map.Entry<Integer, ConcurrentLinkedQueue<StopOrder>>> sellIterator = sellOrders.entrySet().iterator();
            while (sellIterator.hasNext()) {
                Map.Entry<Integer, ConcurrentLinkedQueue<StopOrder>> entry = sellIterator.next();
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

                if (sellIterator.hasNext()) {
                    writer.write(",\n");
                }
            }

            writer.write("\n}}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Metodo per prendere l'ultimo id degli ordini di vendita
    public int getSellStopOrderId() {
        return sellOrders.values().stream()
            .flatMap(queue -> queue.stream().map(StopOrder::getId))
            .max(Integer::compare)
            .orElse(0);
    }



    // Metodo per aggiungere un ordine di vendita
    public int addSellStopOrder(StopOrder order) {

        synchronized (lock) {
            sellOrders.computeIfAbsent(order.getPrice(), k -> new ConcurrentLinkedQueue<>()).add(order);
            lock.notifyAll();
        }
        return order.getId();
    }

    // Metodo per rimuovere un ordine di vendita
    public boolean removeSellStopOrder(int id, String username) {
        return sellOrders.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(order -> order.getId() == id && order.getUsername().equals(username));
            return entry.getValue().isEmpty(); // Se la coda è vuota, rimuoviamo la chiave
        });
    }
    

    @Override
    public void run() {
        while (running) {
            synchronized (lock) {
                while (sellOrders.isEmpty() && running) { 
                    try {
                        lock.wait(); // Aspetta finché non ci sono ordini
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            

            // Esegui gli ordini di vendita
            while (!sellOrders.isEmpty() && sellOrders.firstKey() <= price.get()) {
                ConcurrentLinkedQueue<StopOrder> orders;
                synchronized (lock) {
                    orders = sellOrders.pollFirstEntry().getValue();
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
        salvaSellStopOrders();
        sellOrders.clear();
    }


    
}
