package cross.order;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

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
        while (!Thread.currentThread().isInterrupted()) {
            processOrders(buyOrders, true);
            processOrders(sellOrders, false);
        }
    }

    private void processOrders(ConcurrentSkipListMap<Integer, PriorityQueue<Order>> orders, boolean isBuy) {
        while (!orders.isEmpty()) {
            Map.Entry<Integer, PriorityQueue<Order>> entry = isBuy ? orders.firstEntry() : orders.lastEntry();
            if (entry == null) break;

            int price = entry.getKey();
            boolean shouldProcess = isBuy ? price >= currentPrice.get() : price <= currentPrice.get();

            if (shouldProcess) {
                PriorityQueue<Order> orderQueue = orders.pollFirstEntry().getValue();
                while (!orderQueue.isEmpty()) {
                    queue.add(orderQueue.poll());
                }
            } else {
                break;
            }
        }
    }

    public void addOrder(Order order) {
        ConcurrentSkipListMap<Integer, PriorityQueue<Order>> targetMap = order.isAsk() ? sellOrders : buyOrders;
        targetMap.computeIfAbsent(order.getPrice(), k -> new PriorityQueue<>(Comparator.comparing(Order::getTimestamp)))
                 .add(order);
    }
}
