package cross.order;

import java.util.*;
import java.util.concurrent.*;

public class OrderBook {

    // Liste threadsafe per bid e ask
    private final PriorityBlockingQueue<LimitOrder> bidOrders;
    private final PriorityBlockingQueue<LimitOrder> askOrders;

    // Lista per MarketOrder
    private final BlockingQueue<MarketOrder> marketOrders;

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
        this.marketOrders = new LinkedBlockingQueue<>();
        this.currentPrice = new AtomicInteger(0);
    }

    // Aggiunta ordini
    public synchronized void addOrder(Order order) {
        if (order instanceof LimitOrder limitOrder) {
            if (limitOrder.getType().equals("bid")) {
                bidOrders.add(limitOrder);
            } else {
                askOrders.add(limitOrder);
            }
        } else if (order instanceof MarketOrder marketOrder) {
            processMarketOrder(marketOrder);
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

        while (!orders.isEmpty() && remainingSize > 0) {
            LimitOrder limitOrder = orders.peek();

            boolean priceMatch = (order.getType().equals("bid") && limitOrder.getLimitPrice() <= currentPrice.get()) ||
                                 (order.getType().equals("ask") && limitOrder.getLimitPrice() >= currentPrice.get());

            if (priceMatch) {
                if (remainingSize >= limitOrder.getSize()) {
                    remainingSize -= limitOrder.getSize();
                    orders.poll();
                } else {
                    int newSize = limitOrder.getSize() - remainingSize;
                    orders.poll();
                    orders.add(new LimitOrder(limitOrder.getType(), newSize, limitOrder.getLimitPrice()));
                    remainingSize = 0;
                }
            } else {
                break;
            }
        }
    }

    // Metodo per aggiornare il prezzo corrente
    public synchronized void updateCurrentPrice(int price) {
        currentPrice.set(price);
    }

    // Stampa lo stato dell'OrderBook
    public synchronized void printOrderBook() {
        System.out.println("BID Orders:");
        bidOrders.forEach(o -> System.out.println(o.getLimitPrice() + " - " + o.getSize()));

        System.out.println("ASK Orders:");
        askOrders.forEach(o -> System.out.println(o.getLimitPrice() + " - " + o.getSize()));

        System.out.println("Market Price: " + currentPrice.get());
    }
}
