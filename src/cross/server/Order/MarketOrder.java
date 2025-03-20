package cross.server.Order;

import java.net.InetAddress;


/*
 * MarketOrder class una classe che rappresenta un ordine di tipo Market sottoclasse di Order
 */
public class MarketOrder extends Order {

    public MarketOrder(int id, String type, int size, String username, InetAddress IP, int port) {
        super(id, type, "MarketOrder", size, username, IP, port);
    }


}
