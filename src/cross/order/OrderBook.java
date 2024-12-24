package cross.order;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class OrderBook implements Runnable {
    private final PriorityBlockingQueue<Order> queue;
    private final ConcurrentSkipListMap<Integer, PriorityQueue<Order>> buyOrders;
    private final ConcurrentSkipListMap<Integer, PriorityQueue<Order>> sellOrders;
    private final AtomicInteger currentPrice;

    public OrderBook(PriorityBlockingQueue<Order> queue, AtomicInteger currentPrice) {
        this.queue = queue;
        this.currentPrice = currentPrice;
        this.buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        this.sellOrders = new ConcurrentSkipListMap<>();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Order order = queue.take();
                if (order.isAsk()) {
                    processOrder(order, buyOrders, false);
                } else {
                    processOrder(order, sellOrders, true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void addOrder(Order order) {
        ConcurrentSkipListMap<Integer, PriorityQueue<Order>> targetMap = order.isAsk() ? sellOrders : buyOrders;
        targetMap.computeIfAbsent(order.getPrice(), k -> new PriorityQueue<>(Comparator.comparing(Order::getTimestamp)))
                 .add(order);
    }

    private void processOrder(Order order, ConcurrentSkipListMap<Integer, PriorityQueue<Order>> oppositeOrders, boolean isBuy) {
        Iterator<Map.Entry<Integer, PriorityQueue<Order>>> iterator = oppositeOrders.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, PriorityQueue<Order>> entry = iterator.next();
            if (order.isExecutable(entry.getKey())) {
                PriorityQueue<Order> orderQueue = entry.getValue();
                while (!orderQueue.isEmpty()) {
                    Order matchedOrder = orderQueue.peek();
                    if (matchedOrder.isExecutable(order.getPrice())) {
                        int tradePrice = matchedOrder.getPrice();
                        int tradeSize = Math.min(matchedOrder.getNotCoveredSize(), order.getNotCoveredSize());

                        matchedOrder.setNotCoveredSize(matchedOrder.getNotCoveredSize() - tradeSize);
                        order.setNotCoveredSize(order.getNotCoveredSize() - tradeSize);

                        if (matchedOrder.getNotCoveredSize() == 0) {
                            orderQueue.poll();
                            // TODO: Implement trade notification for matchedOrder
                        }

                        currentPrice.set(tradePrice);

                        if (order.getNotCoveredSize() == 0) {
                            // TODO: Implement trade notification for order
                            return;
                        }
                    } else {
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }
}
