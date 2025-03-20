package cross.server.utils;

import java.time.Instant;


/*
    * Trade class per la gestione dei trade effettuati
    * @param orderId: id dell'ordine
    * @param type: tipo di ordine(ask/bid)
    * @param orderType: tipo di ordine (Limit/Market/Stop)
    * @param size: quantità dell'ordine
    * @param price: prezzo dell'ordine
    * @param timestamp: timestamp dell'ordine
    */

public class Trade {
    int orderId;
    String type;
    String orderType;
    int size;
    int price;
    long timestamp;

    public Trade(int orderId, String type, String orderType, int size, int price) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = Instant.now().getEpochSecond();
    }


    public int getOrderId() {
        return orderId;
    }
    
    public String getType() {
        return type;
    }

    public String getOrderType() {
        return orderType;
    }

    public int getSize() {
        return size;
    }


    public int getPrice() {
        return price;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
