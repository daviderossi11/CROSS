package cross.server.Order;

import java.net.InetAddress;


/*
 * StopOrder class una classe che rappresenta un ordine di tipo Stop sottoclasse di Order
 * @param price: prezzo di stop dell'ordine
 */
public class StopOrder extends Order {
    private int price;

    public StopOrder(int id, String type, int size, int price, String username, InetAddress IP, int port) {
        super(id, type, "StopOrder", size, username, IP, port);
        this.price = price;
    }
    
    public int getPrice() {
        return price;
    }
}
