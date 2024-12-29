package cross.order;

import com.google.gson.annotations.Expose;

public abstract class Order {
    @Expose private final int orderId;
    @Expose private final String type; // ask or bid
    @Expose private final String orderType; // market, limit or stop
    @Expose private final int price;
    @Expose private int size;
    @Expose private final long timestamp;
    private final int userId;



    public Order(int orderId, String type, String orderType, int price, int size, long timestamp, int userId) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.price = price;
        this.size = size;
        this.timestamp = timestamp;
        this.userId = userId;
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


    public long getTimestamp() {
        return timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public int getSize() {
        return size;
    }

    public boolean isAsk() {
        return type.equals("ask");
    }

    public boolean isBid() {
        return type.equals("bid");
    }

    public boolean isExecutable(int comparePrice) {
        if(this.isAsk()){
            return this.getPrice() <= comparePrice;
        } else {
            return this.getPrice() >= comparePrice;
        }
    }

    public void reduceSize(int size) {
        this.size -= size;
    }
    
}
