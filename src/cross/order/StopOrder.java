package cross.order;

public class StopOrder extends Order {

    public StopOrder(int orderId, String type, int price, int size) {
        super(orderId, type, "stop", price, size);
    }

}
