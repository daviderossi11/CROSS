package cross.orderbook;

import cross.order.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.*;

public class CheckStopOrder implements Runnable {
    private final PriorityBlockingQueue<Order> queue;
    private final AtomicInteger currentPrice;

    private final ConcurrentSkipListMap<Integer, PriorityQueue<Order>> buyOrders;
    private final ConcurrentSkipListMap<Integer, PriorityQueue<Order>> sellOrders;

    public CheckStopOrder(PriorityBlockingQueue<Order> queue, AtomicInteger currentPrice) {
        this.queue = queue;
        this.currentPrice = currentPrice;
        this.buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        this.sellOrders = new ConcurrentSkipListMap<>();
    }

    @Override
    public void run() {
        caricaBook();
        while (!Thread.currentThread().isInterrupted()) {
            processOrders(buyOrders, true);
            processOrders(sellOrders, false);
        }
        salvaBook();
    }

    private void processOrders(ConcurrentSkipListMap<Integer, PriorityQueue<Order>> orders, boolean isBuy) {
        while (true) {
            Map.Entry<Integer, PriorityQueue<Order>> entry;

            // 1. Recupera la prima o ultima entry in modo thread-safe
            synchronized (orders) {
                if (orders.isEmpty()) break; // Controllo thread-safe
                entry = isBuy ? orders.firstEntry() : orders.lastEntry(); // Prima o ultima entry
                if (entry == null) break; // Caso in cui non ci siano elementi
            }

            int price = entry.getKey();

            // 2. Controlla se l'ordine deve essere processato in base al prezzo corrente
            boolean shouldProcess = isBuy ? price >= currentPrice.get() : price <= currentPrice.get();
            if (!shouldProcess) break; // Interrompe se il prezzo non soddisfa il criterio

            // 3. Estrae la coda associata al prezzo in modo thread-safe
            PriorityQueue<Order> orderQueue;
            synchronized (orders) {
                orderQueue = orders.remove(price); // Rimuove la coda associata al prezzo
            }

            if (orderQueue != null) {
                // 4. Processa la coda in modo thread-safe
                synchronized (orderQueue) { // Protegge l'accesso alla coda
                    while (!orderQueue.isEmpty()) {
                        queue.add(orderQueue.poll()); // Sposta gli ordini nella coda globale
                    }
                }
            }
        }
    }

    public synchronized void addOrder(Order order) {
        ConcurrentSkipListMap<Integer, PriorityQueue<Order>> targetMap = order.isAsk() ? sellOrders : buyOrders;
        synchronized (targetMap) {
            targetMap.computeIfAbsent(order.getPrice(), k -> new PriorityQueue<>(Comparator.comparing(Order::getTimestamp)))
                     .add(order);
        }
    }

    public synchronized boolean cancelOrder(int orderId, int userId) {
        return cancelFromSkipListMap(buyOrders, orderId, userId) || cancelFromSkipListMap(sellOrders, orderId, userId);
    }

    // Metodo per cancellare un ordine dalla ConcurrentSkipListMap
    private boolean cancelFromSkipListMap(ConcurrentSkipListMap<Integer, PriorityQueue<Order>> orders, int orderId, int userId) {
        for (Map.Entry<Integer, PriorityQueue<Order>> entry : orders.entrySet()) {
            PriorityQueue<Order> orderQueue = entry.getValue();

            synchronized (orderQueue) { // Blocca solo la coda specifica
                for (Order order : orderQueue) {
                    // Controllo diretto dell'ordine e dell'utente
                    if (order.getOrderId() == orderId) {
                        if (order.getUserId() != userId) return false; // UserID non corrispondente
                        orderQueue.remove(order); // Rimuove l'ordine
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void salvaBook() {
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();

        synchronized (buyOrders) {
            JsonArray buyOrdersArray = new JsonArray();
            buyOrders.forEach((price, queue) -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("price", price);
                JsonArray orders = new JsonArray();
                queue.forEach(order -> orders.add(gson.toJsonTree(order)));
                entry.add("orders", orders);
                buyOrdersArray.add(entry);
            });
            jsonObject.add("buyOrders", buyOrdersArray);
        }

        synchronized (sellOrders) {
            JsonArray sellOrdersArray = new JsonArray();
            sellOrders.forEach((price, queue) -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("price", price);
                JsonArray orders = new JsonArray();
                queue.forEach(order -> orders.add(gson.toJsonTree(order)));
                entry.add("orders", orders);
                sellOrdersArray.add(entry);
            });
            jsonObject.add("sellOrders", sellOrdersArray);
        }

        try (Writer writer = new FileWriter("checkstoporder.json")) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void caricaBook() {
        File file = new File("checkstoporder.json");
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            synchronized (buyOrders) {
                buyOrders.clear();
                JsonArray buyOrdersArray = jsonObject.getAsJsonArray("buyOrders");
                buyOrdersArray.forEach(element -> {
                    JsonObject entry = element.getAsJsonObject();
                    int price = entry.get("price").getAsInt();
                    JsonArray ordersArray = entry.getAsJsonArray("orders");
                    PriorityQueue<Order> queue = new PriorityQueue<>(Comparator.comparing(Order::getTimestamp));
                    ordersArray.forEach(orderElement -> queue.add(gson.fromJson(orderElement, Order.class)));
                    buyOrders.put(price, queue);
                });
            }

            synchronized (sellOrders) {
                sellOrders.clear();
                JsonArray sellOrdersArray = jsonObject.getAsJsonArray("sellOrders");
                sellOrdersArray.forEach(element -> {
                    JsonObject entry = element.getAsJsonObject();
                    int price = entry.get("price").getAsInt();
                    JsonArray ordersArray = entry.getAsJsonArray("orders");
                    PriorityQueue<Order> queue = new PriorityQueue<>(Comparator.comparing(Order::getTimestamp));
                    ordersArray.forEach(orderElement -> queue.add(gson.fromJson(orderElement, Order.class)));
                    sellOrders.put(price, queue);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
