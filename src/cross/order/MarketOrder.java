package cross.order;


public class MarketOrder extends Order {
    public MarketOrder(int orderId, String type, int price, int size) {
        super(orderId, type, "market", price, size);
    }
    
    
}
