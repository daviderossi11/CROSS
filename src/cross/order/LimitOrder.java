package cross.order;

public class LimitOrder extends Order{
    

    public LimitOrder(int orderId, String type, int price, int size) {
        super(orderId, type, "limit", price, size);
    }

}
