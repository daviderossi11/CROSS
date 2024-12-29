package cross.order;

public class StopOrder extends Order {

    public StopOrder(int orderId, String type, int price, int size,long timestamp, int userId) {
        super(orderId, type, "stop", price, size, timestamp, userId);
    }

}
