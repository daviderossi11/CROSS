package cross.order;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class OrderBook {

    // Liste threadsafe per bid e ask
    private final PriorityBlockingQueue<LimitOrder> bidOrders;
    private final PriorityBlockingQueue<LimitOrder> askOrders;

    // Prezzo attuale threadsafe
    private final AtomicInteger currentPrice;

    // Costruttore
    public OrderBook() {
        this.bidOrders = new PriorityBlockingQueue<>(
            100, Comparator.comparing(LimitOrder::getLimitPrice).reversed()
                           .thenComparing(LimitOrder::getTimestamp)
        );
        this.askOrders = new PriorityBlockingQueue<>(
            100, Comparator.comparing(LimitOrder::getLimitPrice)
                           .thenComparing(LimitOrder::getTimestamp)
        );
        this.currentPrice = new AtomicInteger(0);
    }


    // Aggiunta ordini
    public synchronized void addOrder(Order order) {
        switch (order) {
            case LimitOrder limitOrder -> {
                if (limitOrder.getType().equals("bid")) {
                    bidOrders.add(limitOrder);
                } else {
                    askOrders.add(limitOrder);
                }
            }
            case MarketOrder marketOrder -> processMarketOrder(marketOrder);
            default -> throw new IllegalArgumentException("Unknown order type");
        }
    }

    // Processa MarketOrder
    private synchronized void processMarketOrder(MarketOrder order) {
        if (order.getType().equals("bid")) {
            matchOrders(order, askOrders);
        } else {
            matchOrders(order, bidOrders);
        }
    }

    // Matching Orders
    private synchronized void matchOrders(Order order, PriorityBlockingQueue<LimitOrder> orders) {
        int remainingSize = order.getSize();
        List<LimitOrder> temp = new ArrayList<>();

        while (!orders.isEmpty() && remainingSize > 0) {

            LimitOrder limitOrder = orders.peek();

            boolean priceMatch = (order.getType().equals("bid") && limitOrder.getLimitPrice() <= currentPrice.get()) ||
                                 (order.getType().equals("ask") && limitOrder.getLimitPrice() >= currentPrice.get());

            if (priceMatch) {
                if (remainingSize >= limitOrder.getSize()) {
                    remainingSize -= limitOrder.getSize();
                    temp.add(orders.poll());
                    
                } else {
                    int newSize = limitOrder.getSize() - remainingSize;
                    temp.add(orders.poll());
                    orders.add(new LimitOrder(limitOrder.getType(), newSize, limitOrder.getLimitPrice()));
                    remainingSize = 0;
                }
            } else {
                System.out.println("No match found for " + order.getOrderId());
                break;
            }
        }

        if (!temp.isEmpty()) {
            int price = temp.stream().mapToInt(LimitOrder::getLimitPrice).max().orElse(currentPrice.get());
            updateCurrentPrice(price);
            System.out.println("Matched " + order.getOrderId() + " with");
            temp.forEach(o -> System.out.println(o.getOrderId()));
        }
    }

    // Metodo per aggiornare il prezzo corrente
    public synchronized void updateCurrentPrice(int price) {
        currentPrice.set(price);
    }

    // Stampa lo stato dell'OrderBook
    public synchronized void printOrderBook() {
        System.out.println("BID Orders:");
        bidOrders.forEach(o -> System.out.println(o.getOrderId() + " - "+ o.getLimitPrice() + " - " + o.getSize()));
        
        System.out.println("ASK Orders:");
        askOrders.forEach(o -> System.out.println(o.getOrderId() + " - "+ o.getLimitPrice() + " - " + o.getSize()));

        System.out.println("Market Price: " + currentPrice.get());
    }
}
