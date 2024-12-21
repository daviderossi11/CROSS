package cross.order;

public class StopOrder extends Order {
    private final int stopPrice;

    public StopOrder(String type, int size, int stopPrice) {
        super(type, size);
        this.stopPrice = stopPrice;
    }

    public int getStopPrice() {
        return stopPrice;
    }


}
