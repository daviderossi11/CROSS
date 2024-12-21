package cross.order;

public class StopOrder extends Order {
    private final int stopPrice;

    public StopOrder(int orderId, String type, int size, int stopPrice) {
        super(orderId, type, size);
        this.stopPrice = stopPrice;
    }

    public int getStopPrice() {
        return stopPrice;
    }


}
