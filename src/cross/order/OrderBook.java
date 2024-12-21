package cross.order;

import java.util.*;
import java.time.*;
import java.io.*;
import com.google.gson.*;
import com.google.gson.GsonBuilder;


public class OrderBook {
    private final List<LimitOrder> bidOrders;
    private final List<LimitOrder> askOrders;
    private final List<StopOrder> stopOrders;

    public OrderBook() {
        this.bidOrders = Collections.synchronizedList(new ArrayList<>());
        this.askOrders = Collections.synchronizedList(new ArrayList<>());
        this.stopOrders = Collections.synchronizedList(new ArrayList<>());
    }

    // Aggiunge un ordine all'order book
    public synchronized void addOrder(Order order) {
        order.setStatus(OrderStatus.PENDING);
        if (order instanceof LimitOrder) {
            switch (order) {
                case LimitOrder limitOrder -> addLimitOrder(limitOrder);
                case StopOrder stopOrder -> stopOrders.add(stopOrder);
                case MarketOrder marketOrder -> processMarketOrder(marketOrder);
                default -> throw new IllegalArgumentException("Unknown order type");
            }
        }
    }

    // Aggiunge un LimitOrder con ordinamento
    private synchronized void addLimitOrder(LimitOrder order) {
        if (order.getType().equals("bid")) {
            bidOrders.add(order);
            bidOrders.sort(Comparator.comparing(LimitOrder::getLimitPrice).reversed()
                                     .thenComparing(LimitOrder::getTimestamp));
        } else {
            askOrders.add(order);
            askOrders.sort(Comparator.comparing(LimitOrder::getLimitPrice)
                                     .thenComparing(LimitOrder::getTimestamp));
        }
    }

    // Cancella un ordine specifico
    public synchronized void cancelOrder(int orderId) {
        bidOrders.removeIf(order -> {
            boolean match = order.getOrderId() == orderId;
            if (match) order.setStatus(OrderStatus.CANCELLED);
            return match;
        });
        askOrders.removeIf(order -> {
            boolean match = order.getOrderId() == orderId;
            if (match) order.setStatus(OrderStatus.CANCELLED);
            return match;
        });
        stopOrders.removeIf(order -> {
            boolean match = order.getOrderId() == orderId;
            if (match) order.setStatus(OrderStatus.CANCELLED);
            return match;
        });
    }

    // Elabora un MarketOrder
    public synchronized void processMarketOrder(MarketOrder order) {
        if (order.getType().equals("bid")) {
            MatchingAlgorithm.matchOrder(order, askOrders);
        } else {
            MatchingAlgorithm.matchOrder(order, bidOrders);
        }
    }

    // Elabora gli StopOrder
    public synchronized void processStopOrders(int marketPrice) {
        Iterator<StopOrder> iterator = stopOrders.iterator();
        while (iterator.hasNext()) {
            StopOrder stopOrder = iterator.next();
            if ((stopOrder.getType().equals("bid") && marketPrice >= stopOrder.getStopPrice()) ||
                (stopOrder.getType().equals("ask") && marketPrice <= stopOrder.getStopPrice())) {
                processMarketOrder(new MarketOrder(stopOrder.getOrderId(), stopOrder.getType(), stopOrder.getSize()));
                stopOrder.setStatus(OrderStatus.FILLED);
                iterator.remove();
            }
        }
    }

    // Stampa lo stato attuale dell'order book
    public synchronized void printOrderBook() {
        System.out.println("BID Orders:");
        bidOrders.forEach(o -> System.out.println(o.getLimitPrice() + " - " + o.getSize() + " - " + o.getStatus()));

        System.out.println("ASK Orders:");
        askOrders.forEach(o -> System.out.println(o.getLimitPrice() + " - " + o.getSize() + " - " + o.getStatus()));
    }

    // Genera un file JSON con lo storico degli ordini di un determinato mese
    public synchronized void exportOrderHistoryByMonth(int year, int month, String filePath) {
        List<Order> filteredOrders = new ArrayList<>();

        bidOrders.stream()
                .filter(o -> isOrderInMonth(o.getTimestamp(), year, month))
                .forEach(filteredOrders::add);

        askOrders.stream()
                .filter(o -> isOrderInMonth(o.getTimestamp(), year, month))
                .forEach(filteredOrders::add);

        stopOrders.stream()
                .filter(o -> isOrderInMonth(o.getTimestamp(), year, month))
                .forEach(filteredOrders::add);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(filteredOrders, writer);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    private boolean isOrderInMonth(long timestamp, int year, int month) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.getYear() == year && dateTime.getMonthValue() == month;
    }
}

