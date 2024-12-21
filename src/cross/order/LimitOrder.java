package cross.order;

public class LimitOrder extends Order{
    private final int limitPrice;

    public LimitOrder(String type, int size, int limitPrice) {
        super(type, size);
        this.limitPrice = limitPrice;
    }

    public int getLimitPrice() {
        return limitPrice;
    }

    
}
