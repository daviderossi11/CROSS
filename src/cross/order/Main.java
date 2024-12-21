package cross.order;

import java.time.*;

public class Main {
    public static void main(String[] args) {
        // Creazione dell'OrderBook
        OrderBook orderBook = new OrderBook();

        // Aggiunta di ordini di esempio
        LimitOrder order1 = new LimitOrder(1, "bid", 10, 50000);
        LimitOrder order2 = new LimitOrder(2, "ask", 5, 51000);
        LimitOrder order3 = new LimitOrder(3, "bid", 15, 50500);
        StopOrder stopOrder1 = new StopOrder(4, "bid", 10, 49500);
        StopOrder stopOrder2 = new StopOrder(5, "ask", 8, 51500);

        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);
        orderBook.addOrder(stopOrder1);
        orderBook.addOrder(stopOrder2);

        // Simulazione di un market order
        MarketOrder marketOrder = new MarketOrder(6, "bid", 12);
        orderBook.processMarketOrder(marketOrder);

        // Stampa lo stato attuale dell'order book
        System.out.println("Stato attuale dell'OrderBook:");
        orderBook.printOrderBook();

        // Esporta lo storico degli ordini per il mese corrente
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        String filePath = "storicoOrdini.json";
        orderBook.exportOrderHistoryByMonth(year, month, filePath);
        System.out.println("Storico ordini esportato in: " + filePath);
    }
}
