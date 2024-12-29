package cross.orderbook;

import cross.order.*;
import cross.user.*;
import cross.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.google.gson.*;

public class OrderBook implements Runnable {
    private final PriorityBlockingQueue<Order> queue;
    private final ConcurrentSkipListMap<Integer, PriorityQueue<Order>> buyOrders;
    private final ConcurrentSkipListMap<Integer, PriorityQueue<Order>> sellOrders;
    private final AtomicInteger currentPrice;
    private final PriorityBlockingQueue<Session> notificationQueue;
    private final StoricoOrdini storicoOrdini;
    private final UserManager userManager;

    public OrderBook(PriorityBlockingQueue<Order> queue, AtomicInteger currentPrice, PriorityBlockingQueue<Session> notificationQueue, StoricoOrdini storicoOrdini, UserManager userManager) {
        this.queue = queue;
        this.currentPrice = currentPrice;
        this.buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        this.sellOrders = new ConcurrentSkipListMap<>();
        this.notificationQueue = notificationQueue;
        this.storicoOrdini = storicoOrdini;
        this.userManager = userManager;
    }

    @Override
    public void run() {
        caricaBook();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Blocca il thread finché non c'è un elemento disponibile
                Order order = queue.take(); // Blocca finché la coda non ha un elemento

                // Processa l'ordine
                if (order.isAsk()) {
                    processOrder(order, buyOrders, false);
                } else {
                    processOrder(order, sellOrders, true);
                }
            }
        } catch (InterruptedException e) {
            // Interruzione pulita del thread
            Thread.currentThread().interrupt();
        } finally {
            // Salvataggio finale prima della chiusura
            salvaBook();
        }
    }

    public synchronized void addOrder(Order order) {
        ConcurrentSkipListMap<Integer, PriorityQueue<Order>> targetMap = order.isAsk() ? sellOrders : buyOrders;
        
        targetMap.computeIfAbsent(order.getPrice(), k -> new PriorityQueue<>(Comparator.comparing(Order::getTimestamp)))
                    .add(order);
    }

    private void processOrder(Order order, ConcurrentSkipListMap<Integer, PriorityQueue<Order>> oppositeOrders, boolean isBuy) {
        List<Order> executedOrders = new ArrayList<>();
        int tempSize = 0;
        Iterator<Map.Entry<Integer, PriorityQueue<Order>>> iterator = oppositeOrders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PriorityQueue<Order>> entry = iterator.next();
            int price = entry.getKey();

            // Verifica se l'ordine può essere eseguito
            if (isBuy ? price > order.getPrice() : price < order.getPrice()) {
                break;
            }

            PriorityQueue<Order> orders = entry.getValue();
            synchronized (orders) {
                Iterator<Order> orderIterator = orders.iterator();

                while (orderIterator.hasNext()) {
                    Order matchingOrder = orderIterator.next();

                    // verifico che gli ordini abbiano utenti diversi
                    if (matchingOrder.getUserId() == order.getUserId()) {
                        continue;
                    }

                    int executedSize = Math.min(order.getSize(), matchingOrder.getSize());

                    // Aggiornamento della dimensione residua
                    tempSize = tempSize + executedSize;
                    order.reduceSize(executedSize);
                    matchingOrder.reduceSize(executedSize);

                    // Notifica l'utente
                    switch (order.getType()) {
                        case "market" -> notifyUsers(List.of(new MarketOrder(order.getOrderId(), order.getType(), order.getPrice(), executedSize, System.currentTimeMillis(), order.getUserId())), order.getUserId());
                        case "limit" -> notifyUsers(List.of(new LimitOrder(order.getOrderId(), order.getType(), order.getPrice(), executedSize, System.currentTimeMillis(), order.getUserId())), order.getUserId());
                        case "stop" -> notifyUsers(List.of(new StopOrder(order.getOrderId(), order.getType(), order.getPrice(), executedSize, System.currentTimeMillis(), order.getUserId())), order.getUserId());
                    }

                    // Aggiorna il prezzo corrente
                    synchronized (currentPrice) {
                        currentPrice.set(price);
                    }

                    // Aggiungi al registro delle transazioni
                    Order newOrder = aggiungiTrade(matchingOrder, executedSize);
                    executedOrders.add(newOrder);

                    // Rimuovi l'ordine eseguito completamente
                    if (matchingOrder.getSize() == 0) {
                        orderIterator.remove();
                    }

                    // Esci se l'ordine è completamente eseguito
                    if (order.getSize() == 0) {
                        aggiungiTrade(order, tempSize);
                        notifyUsers(executedOrders, order.getUserId());
                        return;
                    }
                }
            }

            // Rimuovi le code vuote
            if (orders.isEmpty()) {
                iterator.remove();
            }
        }

        // Se l'ordine non è stato completato, aggiungilo all'elenco appropriato
        aggiungiTrade(order, tempSize);
        notifyUsers(executedOrders, tempSize);
        addOrder(order);
    }

    public synchronized boolean cancelOrder(int orderId, int userId) {
        return cancelFromSkipListMap(buyOrders, orderId, userId) || cancelFromSkipListMap(sellOrders, orderId, userId) || cancelFromQueue(queue, orderId, userId);
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

    // Metodo per cancellare un ordine dalla PriorityBlockingQueue (già thread-safe)
    private boolean cancelFromQueue(PriorityBlockingQueue<Order> queue, int orderId, int userId) {
        for (Order order : queue) {
            // Controllo diretto dell'ordine e dell'utente
            if (order.getOrderId() == orderId) {
                if (order.getUserId() != userId) return false; // UserID non corrispondente
                queue.remove(order); // Rimuove l'ordine direttamente
                return true;
            }
        }
        return false;
    }

    public Order aggiungiTrade(Order order, int size) {
        switch (order.getOrderType()) {
            case "market" -> {
                MarketOrder newOrder = new MarketOrder(order.getOrderId(), order.getType(), order.getPrice(), size, System.currentTimeMillis(), order.getUserId());
                storicoOrdini.addTrade(newOrder);
                return newOrder;
            }
            case "limit" -> {
                LimitOrder newOrder = new LimitOrder(order.getOrderId(), order.getType(), order.getPrice(), size, System.currentTimeMillis(), order.getUserId());
                storicoOrdini.addTrade(newOrder);
                return newOrder;
            }
            case "stop" -> {
                StopOrder newOrder = new StopOrder(order.getOrderId(), order.getType(), order.getPrice(), size, System.currentTimeMillis(), order.getUserId());
                storicoOrdini.addTrade(newOrder);
                return newOrder;
            }
            default -> throw new IllegalArgumentException("Unknown order type: " + order.getOrderType());
        }
    }

    private void notifyUsers(List<Order> orders, int userid) {
        JsonObject notification = new JsonObject();
        JsonArray trades = new JsonArray();
        for (Order order : orders) {
            JsonObject trade = new JsonObject();
            trade.addProperty("orderId", order.getOrderId());
            trade.addProperty("type", order.getType());
            trade.addProperty("price", order.getPrice());
            trade.addProperty("size", order.getSize());
            trade.addProperty("timestamp", order.getTimestamp());
            trades.add(trade);
        }
        notification.add("trades", trades);

        Session session = userManager.getSession(userid);

        if (session != null) {
            synchronized (session) {
                session.setNotification(notification.toString());
                notificationQueue.add(session);
            }
        }
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

        try (Writer writer = new FileWriter("files/orderbook.json")) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void caricaBook() {
        File file = new File("files/orderbook.json");
        if (!file.exists()) {
            return;
        }

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
