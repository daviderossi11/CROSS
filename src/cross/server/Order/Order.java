package cross.server.Order;


import java.net.InetAddress;


/*
 * Order class una classe astratta che rappresenta un ordine
 * @param id: id dell'ordine
 * @param type: tipo di ordine(ask/bid)
 * @param orderType: tipo di ordine (Limit/Market/Stop)
 * @param size: quantità dell'ordine
 * @param IP: indirizzo IP dell'utente
 * @param port: porta dell'utente
 * @param username: username dell'utente
 * 
 * 
 */
public abstract class Order {
    private int id;
    private String type;
    private String orderType;
    private int size;
    private InetAddress IP;
    private int port;
    private String username;

    public Order(int id, String type, String orderType, int size,String username, InetAddress IP, int port) {
        this.id = id;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.username = username;
        this.IP = IP;
        this.port = port;
    }


    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getOrderType() {
        return orderType;
    }

    public InetAddress getIP() {
        return IP;
    }

    public int getSize() {
        return size;
    }

    public void reduceSize(int size) {
        this.size -= size;
    }

    public String getUsername() {
        return username;
    }

    public int getPort() {
        return port;
    }

}
