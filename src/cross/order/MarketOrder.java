package cross.order;


public class MarketOrder extends Order {

    public MarketOrder(int orderId, String type, int price, int size,long timestamp, int userId) {
        super(orderId, type, "market", price, size, timestamp, userId);
    }
    
    
}
