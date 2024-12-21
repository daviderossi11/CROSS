package cross.order;
public class MarketOrder extends Order {
    public MarketOrder(int orderId, String type, int size) {
        super(orderId, type, size);
    }
    
    @Override
    public boolean processOrder() {
        // process market order
        return true;
    }
    
}
