package cross.order;


public abstract class Order {
    private final int orderId;
    private final String type; // ask or bid
    private final String orderType; // market, limit or stop
    private final int price;
    private final int size;
    private final long timestamp;


    public Order(int orderId, String type, String orderType, int price, int size) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.price = price;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
    }

    public int getOrderId() {
        return orderId;
    }

    public String getType() {
        return type;
    }

    public String getOrderType() {
        return orderType;
    }

    public int getPrice() {
        return price;
    }

    public int getSize() {
        return size;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isAsk() {
        return type.equals("ask");
    }

    public boolean isBid() {
        return type.equals("bid");
    }

    
}
