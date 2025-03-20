package cross.server.Order;
import java.net.InetAddress;


/*
 * LimitOrder class una classe che rappresenta un ordine di tipo Limit sottoclasse di Order
 * @param price: prezzo limite dell'ordine
 */

public class LimitOrder extends Order{
    private int price;

    public LimitOrder(int id, String type, int size, int price, String username, InetAddress IP, int port) {
        super(id, type, "LimitOrder", size, username, IP, port);
        this.price = price;
    }
    
    public int getPrice() {
        return price;
    }
    
}
