package cross.order;


public abstract class Order {
    private final int orderId;
    private final String type; // ask or bid
    private final String orderType; // market, limit or stop
    private final int price;
    private final int size;
    private final long timestamp;
    private int notCoveredSize;


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

    public int getNotCoveredSize() {
        return notCoveredSize;

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

    public void setNotCoveredSize() {
        this.notCoveredSize = size;
    }

    public void setNotCoveredSize(int notCoveredSize) {
        this.notCoveredSize = notCoveredSize;
    }

    public boolean isFullyCovered() {
        return this.getNotCoveredSize() == 0;
    }

    public boolean isExecutable(int comparePrice) {
        if(this.isAsk()){
            return this.getPrice() <= comparePrice;
        } else {
            return this.getPrice() >= comparePrice;
        }
    }
    
}
