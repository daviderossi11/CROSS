package cross.server.Orderbook;

import cross.server.Order.*;
import cross.server.handler.StoricoOrdiniHandler;
import cross.server.utils.NotificationSender;
import cross.server.utils.Trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.*;

public class Orderbook {
    private final String FILE_PATH = "files/orderbook.json"; // Percorso del file JSON contenente l'Orderbook
    private final static NotificationSender sender = new NotificationSender(); // Sender per inviare le notifiche UDP
    private final static Gson gson = new GsonBuilder().disableHtmlEscaping().create(); // Gson per la serializzazione/deserializzazione degli oggetti

    private final StoricoOrdiniHandler storicoOrdiniHandler; // Riferimento al gestore dello storico degli ordini

    private AtomicInteger price; // Prezzo corrente 

    private static ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>> buyOrders; // Mappa degli ordini di acquisto
    private static ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>> sellOrders; // Mappa degli ordini di vendita

    public Orderbook(AtomicInteger price, StoricoOrdiniHandler storicoOrdiniHandler) {
        this.storicoOrdiniHandler = storicoOrdiniHandler;
        this.price = price;
        caricaOrderbook();
    }


    // Metodo per ottenere il prezzo corrente
    public int getCurrentPrice() {
        return price.get();
    }


    // Metodo per caricare l'Orderbook dal file
    private void caricaOrderbook() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type mapType = new TypeToken<ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>>>(){}.getType();
                Map<String, Object> orderbookData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
    
                if (orderbookData != null) {
                    buyOrders = gson.fromJson(gson.toJson(orderbookData.get("buyOrders")), mapType);
                    sellOrders = gson.fromJson(gson.toJson(orderbookData.get("sellOrders")), mapType);
                }
    
                // Se il file esiste ma contiene dati nulli, inizializza le mappe vuote
                if (buyOrders == null) buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder());
                if (sellOrders == null) sellOrders = new ConcurrentSkipListMap<>();
    
                System.out.println("Orderbook caricato da file.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Se il file non esiste, inizializza le mappe vuote
            buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder());
            sellOrders = new ConcurrentSkipListMap<>();
            System.out.println("File non trovato. Creando un nuovo Orderbook.");
        }
    }


    // Metodo per salvare l'Orderbook nel file
    public void saveOrderbook() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write("{\n\"buyOrders\": {\n");

            // Scriviamo gli ordini di acquisto (buyOrders)
            Iterator<Map.Entry<Integer, ConcurrentLinkedQueue<LimitOrder>>> buyIterator = buyOrders.entrySet().iterator();
            while (buyIterator.hasNext()) {
                Map.Entry<Integer, ConcurrentLinkedQueue<LimitOrder>> entry = buyIterator.next();
                int price = entry.getKey();
                ConcurrentLinkedQueue<LimitOrder> orders = entry.getValue();

                writer.write("\"" + price + "\": [\n");

                Iterator<LimitOrder> orderIterator = orders.iterator();
                if (orderIterator.hasNext()) {
                    writer.write(gson.toJson(orderIterator.next()));
                }

                while (orderIterator.hasNext()) {
                    writer.write(",\n" + gson.toJson(orderIterator.next()));
                }

                writer.write("\n]");

                if (buyIterator.hasNext()) {
                    writer.write(",\n");
                }
            }

            writer.write("\n},\n\"sellOrders\": {\n");

            // Scriviamo gli ordini di vendita (sellOrders)
            Iterator<Map.Entry<Integer, ConcurrentLinkedQueue<LimitOrder>>> sellIterator = sellOrders.entrySet().iterator();
            while (sellIterator.hasNext()) {
                Map.Entry<Integer, ConcurrentLinkedQueue<LimitOrder>> entry = sellIterator.next();
                int price = entry.getKey();
                ConcurrentLinkedQueue<LimitOrder> orders = entry.getValue();

                writer.write("\"" + price + "\": [\n");

                Iterator<LimitOrder> orderIterator = orders.iterator();
                if (orderIterator.hasNext()) {
                    writer.write(gson.toJson(orderIterator.next()));
                }

                while (orderIterator.hasNext()) {
                    writer.write(",\n" + gson.toJson(orderIterator.next()));
                }

                writer.write("\n]");

                if (sellIterator.hasNext()) {
                    writer.write(",\n");
                }
            }

            writer.write("\n}\n}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // Metodo per aggiungere un ordine di tipo Limit
    public int addLimitOrder(LimitOrder order) {
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>> orders = order.getType().equals("bid") ? buyOrders : sellOrders;
        orders.computeIfAbsent(order.getPrice(), k -> new ConcurrentLinkedQueue<>()).add(order);
        return order.getId();
    }



    // Metodo per prendere l'ultimo id degli ordini
    public int getOrderid() {
        return Math.max(
            buyOrders.values().stream()
                .flatMap(queue -> queue.stream().map(LimitOrder::getId))
                .max(Integer::compare)
                .orElse(0),
            sellOrders.values().stream()
                .flatMap(queue -> queue.stream().map(LimitOrder::getId))
                .max(Integer::compare)
                .orElse(0)
        );
    }
    


    // Metodo per rimuovere un ordine di tipo Limit
    public boolean removeLimitOrder(int id, String username) {
        return removeOrderFromMap(buyOrders, id, username) || removeOrderFromMap(sellOrders, id, username);
    }
    
    // Metodo per rimuovere un ordine da una mappa di ordini
    private boolean removeOrderFromMap(ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>> orders, int id, String username) {
        return orders.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(order -> order.getId() == id && order.getUsername().equals(username));
            return entry.getValue().isEmpty(); // Se la coda è vuota, rimuoviamo la chiave
        });
    }
    
    // Metodo per controllare se un ordine  è processabile
    public boolean isProcessable(Order order, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>> orders) {
        int size = order.getSize();
        String username = order.getUsername();
        for (Map.Entry<Integer, ConcurrentLinkedQueue<LimitOrder>> entry : orders.entrySet()) {
            for (LimitOrder limitOrder : entry.getValue()) {
                if (size > 0) {
                    if(!limitOrder.getUsername().equals(username)) {
                        size -= limitOrder.getSize();
                    }
                } else {
                    return true;
                }
            }
        }
        return size <= 0;
    }

    // Metodo per processare un ordine di tipo Market
    public int processMarketOrder(MarketOrder order) {
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>> map = order.getType().equals("bid") ? sellOrders : buyOrders;
        synchronized (map) {
            // controllo se l'ordine è processabile
            if (!isProcessable(order, map)) {
                System.out.println("ORDINE " + order.getId() + " SCARTATO");
                return -1;
            }
            List<Trade> storico = new ArrayList<>();
            int size = order.getSize();
            String username = order.getUsername();
            for (Map.Entry<Integer, ConcurrentLinkedQueue<LimitOrder>> entry : map.entrySet()) {
                price.set(entry.getKey()); //modifico il prezzo corrente
                List<Trade> trades = new ArrayList<>();
                Iterator<LimitOrder> iterator = entry.getValue().iterator();
                while (iterator.hasNext()) {
                    LimitOrder limitOrder = iterator.next();
                    if (username.equals(limitOrder.getUsername())) {
                        continue; // se l'utente è lo stesso non posso eseguire l'ordine
                    }
                    if (limitOrder.getSize() < size) {
                        // se l'ordine è inferiore alla size dell'ordine di mercato e l'utente non è lo stesso posso eseguire l'ordine
                        size -= limitOrder.getSize();
                        Trade n1 = new Trade(limitOrder.getId(), limitOrder.getType(), limitOrder.getOrderType(), limitOrder.getSize(), limitOrder.getPrice());
                        Trade n2 = new Trade(order.getId(), order.getType(), order.getOrderType(), limitOrder.getSize(), limitOrder.getPrice());
                        trades.add(n1);
                        List<Trade> temp = new ArrayList<>();
                        temp.add(n2);

                        System.out.println("LIMIT ORDER: " + limitOrder.getId());
                        sender.sendNotification(temp, limitOrder.getIP(), limitOrder.getPort()); // invio la notifica del limit order eseguito al proprietario
                        iterator.remove(); // rimuovo l'ordine dalla lista
                    } else {
                        // se la size dell'ordine di mercato è inferiore all'ordine limite
                        Trade n1 = new Trade(limitOrder.getId(), limitOrder.getType(), limitOrder.getOrderType(), size, limitOrder.getPrice());
                        Trade n2 = new Trade(order.getId(), order.getType(), order.getOrderType(), size, limitOrder.getPrice());
                        trades.add(n1);
                        List<Trade> temp = new ArrayList<>();
                        temp.add(n2);
                        System.out.println("LIMIT ORDER: " + limitOrder.getId());
                        sender.sendNotification(temp, limitOrder.getIP(), limitOrder.getPort()); // invio la notifica del limit order eseguito al proprietario
                        if (limitOrder.getSize() == size) {
                            // se la size dell'ordine di mercato è uguale all'ordine limite
                            iterator.remove(); // rimuovo l'ordine dalla lista
                            if (entry.getValue().isEmpty()) map.remove(entry.getKey()); // se la coda è vuota rimuovo la chiave
                        } else {
                            limitOrder.reduceSize(size); // riduco la size dell'ordine limite
                        }

                        System.out.println("MARKET ORDER: " + order.getId());
                        sender.sendNotification(trades, order.getIP(), order.getPort()); // invio la notifica dell'ordine di mercato eseguito al proprietario

                        // aggiungere trade a storico
                        storico.addAll(trades);
                        storico.add(n2);

                        storicoOrdiniHandler.aggiungiTrade(storico);
                        System.out.println("ORDINE " + order.getId() + " ESEGUITO");

                        return order.getId(); // restituisco l'id dell'ordine
                    }
                }

                // devo segnare tutti gli ordini allo stesso prezzo
                sender.sendNotification(trades, order.getIP(), order.getPort()); // invio una parte dell'ordine di mercato eseguito al proprietario
                Trade n2 = new Trade(order.getId(), order.getType(), order.getOrderType(), size, entry.getKey());
                storico.addAll(trades);
                storico.add(n2);
                if (entry.getValue().isEmpty()) map.remove(entry.getKey());
            }
            // aggiungere storico a file

            storicoOrdiniHandler.aggiungiTrade(storico);

            System.out.println("ORDINE " + order.getId() + " ESEGUITO");

            return order.getId();
        }
    }

    // Metodo per processare un ordine di tipo Stop
    public void executeStopOrder(StopOrder order) {
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<LimitOrder>> map = order.getType().equals("bid") ? sellOrders : buyOrders;
        synchronized (map) {
            if (!isProcessable(order, map)) {
                // se l'ordine non è processabile
                List<Trade> trades = new ArrayList<>();
                trades.add(new Trade(order.getId(), order.getType(), order.getOrderType(), -1, -1)); 
                sender.sendNotification(trades, order.getIP(), order.getPort()); // invio una notifica di ordine scartato al proprietario dello stop order
                System.err.println("ORDINE " + order.getId() + " SCARTATO");

                return;
            }
            if (order.getType().equals("bid") && map.firstKey() >= order.getPrice()) {
                // se il tipo è bid e il prezzo è maggiore o uguale al prezzo dello stop order
                List<Trade> trades = new ArrayList<>();
                trades.add(new Trade(order.getId(), order.getType(), order.getOrderType(), -1, -1));
                sender.sendNotification(trades, order.getIP(), order.getPort()); // invio una notifica di ordine scartato al proprietario dello stop order
                System.err.println("ORDINE " + order.getId() + " SCARTATO");
                return;
            }
            if (order.getType().equals("ask") && map.firstKey() <= order.getPrice()) {
                // se il tipo è ask e il prezzo è minore o uguale al prezzo dello stop order
                List<Trade> trades = new ArrayList<>();
                trades.add(new Trade(order.getId(), order.getType(), order.getOrderType(), -1, -1));
                sender.sendNotification(trades, order.getIP(), order.getPort()); // invio una notifica di ordine scartato al proprietario dello stop order
                System.err.println("ORDINE " + order.getId() + " SCARTATO");
                return;
            }
            List<Trade> storico = new ArrayList<>();
            int size = order.getSize();
            String username = order.getUsername();
            for (Map.Entry<Integer, ConcurrentLinkedQueue<LimitOrder>> entry : map.entrySet()) {
                price.set(entry.getKey()); // modifico il prezzo corrente
                List<Trade> trades = new ArrayList<>();
                Iterator<LimitOrder> iterator = entry.getValue().iterator();
                while (iterator.hasNext()) {
                    LimitOrder limitOrder = iterator.next();
                    if (username.equals(limitOrder.getUsername())) {
                        continue; // se l'utente è lo stesso non posso eseguire l'ordine
                    }
                    if (limitOrder.getSize() < size) {
                        // se l'ordine è inferiore alla size dello stop order e l'utente non è lo stesso posso eseguire l'ordine
                        size -= limitOrder.getSize();
                        Trade n1 = new Trade(limitOrder.getId(), limitOrder.getType(), limitOrder.getOrderType(), limitOrder.getSize(), limitOrder.getPrice());
                        Trade n2 = new Trade(order.getId(), order.getType(), order.getOrderType(), limitOrder.getSize(), limitOrder.getPrice());
                        trades.add(n1);
                        List<Trade> temp = new ArrayList<>();
                        temp.add(n2);

                        System.out.println("LIMIT ORDER: " + limitOrder.getId());
                        sender.sendNotification(temp, limitOrder.getIP(), limitOrder.getPort()); // invio la notifica del limit order eseguito al proprietario
                        iterator.remove(); // rimuovo l'ordine dalla coda
                    } else {

                        // se la size dello stop order è inferiore all'ordine limite
                        Trade n1 = new Trade(limitOrder.getId(), limitOrder.getType(), limitOrder.getOrderType(), size, limitOrder.getPrice());
                        Trade n2 = new Trade(order.getId(), order.getType(), order.getOrderType(), size, limitOrder.getPrice());
                        trades.add(n1);
                        List<Trade> temp = new ArrayList<>();
                        temp.add(n2);

                        System.out.println("LIMIT ORDER: " + limitOrder.getId());
                        sender.sendNotification(temp, limitOrder.getIP(), limitOrder.getPort()); // invio la notifica del limit order eseguito al proprietario
                        if (limitOrder.getSize() == size) {
                            iterator.remove(); // rimuovo l'ordine dalla coda
                            if (entry.getValue().isEmpty()) map.remove(entry.getKey()); // se la coda è vuota rimuovo la chiave
                        } else {
                            limitOrder.reduceSize(size); // riduco la size dell'ordine limite
                        }

                        System.out.println("STOP ORDER: " + order.getId());
                        sender.sendNotification(trades, order.getIP(), order.getPort()); // invio la notifica dello stop order eseguito al proprietario

                        // aggiungere trade a storico
                        storico.addAll(trades);
                        storico.add(n2);

                        storicoOrdiniHandler.aggiungiTrade(storico);

                        System.out.println("ORDINE " + order.getId() + " ESEGUITO");
                        return;
                    }
                }

                System.out.println("STOP ORDER: " + order.getId());
                sender.sendNotification(trades, order.getIP(), order.getPort()); // invio una parte dello stop order eseguito al proprietario
                Trade n2 = new Trade(order.getId(), order.getType(), order.getOrderType(), size, entry.getKey());
                storico.addAll(trades);
                storico.add(n2);
                if (entry.getValue().isEmpty()) map.remove(entry.getKey());
            }
            // aggiungere storico a file

            storicoOrdiniHandler.aggiungiTrade(storico);

            System.out.println("ORDINE " + order.getId() + " ESEGUITO");
            return;
        }
    }


    // Metodo per chiudere l'Orderbook
    public void close() {
        saveOrderbook();
        buyOrders.clear();
        sellOrders.clear();
    }
}
