package cross.order;

public abstract class Order {
    private final int orderId;
    private final String type; // ask or bid
    private final int size;
    private final long timestamp;

    public Order(int orderId, String type, int size) {
        this.orderId = orderId;
        this.type = type;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
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

    public abstract boolean processOrder();
    
}
