package cross.order;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class Order {
    private static final AtomicInteger orderIdGenerator = new AtomicInteger(0);
    private final int orderId;
    private final String type; // ask or bid
    private final int size;
    private final long timestamp;
    private OrderStatus status;

    public Order(String type, int size) {
        this.orderId = orderIdGenerator.incrementAndGet();
        this.type = type;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
        this.status = OrderStatus.PENDING;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    
}
