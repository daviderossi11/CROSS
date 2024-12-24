package cross.order;


public class MarketOrder extends Order {
    public MarketOrder(int orderId, String type, int price, int size) {
        super(orderId, type, "market", price, size);
    }

    public boolean isExecutable(Order order) {
        return getPrice() == order.getPrice();
    }
    
}
