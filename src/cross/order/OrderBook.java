package cross.order;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class OrderBook implements Runnable {
    private final  LinkedBlockingDeque<Order> MarketOrderQueue;
    private final  PriorityBlockingQueue<Order> LimitOrderQueue;

    private volatile Integer price;
    
    public OrderBook(LinkedBlockingDeque<Order> MarketOrderQueue, PriorityBlockingQueue<Order> LimitOrderQueue) {
        this.MarketOrderQueue = MarketOrderQueue;
        this.LimitOrderQueue = LimitOrderQueue;
    }

    public void run() {
        while (true) {
            try {
                Order order = MarketOrderQueue.take();
                Integer tempPrice = null;
                if (order.getType().equals("bid")) {
                    Iterator<Order> iterator = LimitOrderQueue.iterator();
                    while (iterator.hasNext()) {
                        Order limitOrder = iterator.next();
                        if(limitOrder.getType().equals("bid")){
                            continue;
                        }

                
                    }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
