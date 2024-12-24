package cross.order;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


public class OrderBook implements Runnable {
    private final  PriorityBlockingQueue<Order> Queue;
    private final  TreeMap<Integer, PriorityQueue<Order>> buyOrders;
    private final  TreeMap<Integer, PriorityQueue<Order>> sellOrders;
    private final AtomicInteger currentPrice;

    public OrderBook(PrioriryBlockingQueue<Order> Queue, AtomicInteger currentPrice) {
        this.Queue = Queue;
        this.currentPrice = currentPrice;
        this.buyOrders = new TreeMap<>(Collections.reverseOrder());
        this.sellOrders = new TreeMap<>();

    }

    public void run() {
        while (true) {
            try {
                Order order = Queue.take();
                if (order.isAsk()) {
                    processAsk(order);
                } else {
                    processBid(order);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void addOrder(Order order) {
        if (order.isAsk()) {
            if(!sellOrders.containsKey(order.getPrice())) {
                sellOrders.put(order.getPrice(), new PriorityQueue<>(Comparator.comparing(Order::getTimestamp)));
            }else {
                sellOrders.get(order.getPrice()).add(order);
            }
        } else {
            if(!buyOrders.containsKey(order.getPrice())) {
                buyOrders.put(order.getPrice(), new PriorityQueue<>(Comparator.comparing(Order::getTimestamp)));
            }else {
                buyOrders.get(order.getPrice()).add(order);
            }
        }

        public void processAsk(Order order) {
            while (!buyOrders.isEmpty() && buyOrders.firstKey() >= order.getPrice()) {
                PriorityQueue<Order> orders = buyOrders.firstEntry().getValue();
                while (!orders.isEmpty() && order.getSize() > 0) {
                    Order buyOrder = orders.poll();
                    if (buyOrder.getSize() > order.getSize()) {
                        buyOrder.setSize(buyOrder.getSize() - order.getSize());
                    } else {
                        order.setSize(order.getSize() - buyOrder.getSize());
                    }
                }
                if (orders.isEmpty()) {
                    buyOrders.pollFirstEntry();
                }
            }
            if (order.getSize() > 0) {
                sellOrders.put(order.getPrice(), order);
            }
        }
    }
    











}
