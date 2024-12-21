package cross.order;

public class LimitOrder extends Order{
    private final int limitPrice;

    public LimitOrder(int orderID, String type, int size, int limitPrice) {
        super(orderID, type, size);
        this.limitPrice = limitPrice;
    }

    public int getLimitPrice() {
        return limitPrice;
    }


    @Override
    public boolean processOrder() {
        // process limit order
        return true;
    }
    
}
