package cross.order;

public class Main {
    public static void main(String[] args) {
        // Creazione dell'OrderBook
        OrderBook orderBook = new OrderBook();

        // Creazione di alcuni ordini
        LimitOrder limitOrder1 = new LimitOrder("bid", 100, 100000);
        LimitOrder limitOrder2 = new LimitOrder("ask", 100, 200000);
        MarketOrder marketOrder1 = new MarketOrder("bid", 50);
        MarketOrder marketOrder2 = new MarketOrder("ask", 50);
        LimitOrder limitOrder3 = new LimitOrder("bid", 100, 150000);
        LimitOrder limitOrder4 = new LimitOrder("ask", 100, 250000);
        MarketOrder marketOrder3 = new MarketOrder("bid", 50);
        MarketOrder marketOrder4 = new MarketOrder("ask", 50);

        // Aggiunta degli ordini all'OrderBook
        orderBook.addOrder(limitOrder1);
        orderBook.addOrder(limitOrder2);
        orderBook.addOrder(marketOrder1);
        orderBook.addOrder(marketOrder2);
        orderBook.addOrder(limitOrder3);
        orderBook.addOrder(limitOrder4);
        orderBook.addOrder(marketOrder3);
        orderBook.addOrder(marketOrder4);

        // Stampa dell'OrderBook
        orderBook.printOrderBook();

        }
}
