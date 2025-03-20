package cross.server.handler;


import cross.server.utils.Trade;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class StoricoOrdiniHandler {
    private static final String FILE_PATH = "files/storicoOrdini.json";
    private final ConcurrentLinkedDeque<Trade> trades = new ConcurrentLinkedDeque<>();
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public StoricoOrdiniHandler() {
        caricaStoricoOrdini();
    }

    /**
     * Carica i trade dallo storico JSON nella collezione thread-safe.
     */
    public void caricaStoricoOrdini() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            if (jsonObject != null && jsonObject.has("trades")) {
                List<Trade> tradeList = gson.fromJson(jsonObject.get("trades"), new TypeToken<List<Trade>>() {}.getType());
                trades.addAll(tradeList);
            }
        } catch (IOException e) {
            System.out.println("File storico non trovato, inizializzo un nuovo storico.");
        }
    }

    /**
     * Salva i trade attuali sul file JSON.
     */
    public synchronized void salvaStoricoOrdini() {
        try (FileWriter writer = new FileWriter("files/storicoOrdini.json")) {
            writer.write("{\n\"trades\": [\n");
    
            Iterator<Trade> iterator = trades.iterator(); // Otteniamo un iteratore sulla Deque
    
            if (iterator.hasNext()) {
                writer.write(gson.toJson(iterator.next())); // Scriviamo il primo elemento senza virgola
    
                while (iterator.hasNext()) { // Se ci sono altri elementi, scriviamo con la virgola
                    writer.write(",\n" + gson.toJson(iterator.next()));
                }
            }
    
            writer.write("\n]}\n"); // Chiusura del JSON
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Aggiunge nuovi trade allo storico in modo thread-safe.
     */
    public void aggiungiTrade(List<Trade> nuoviTrade) {
        trades.addAll(nuoviTrade);
    }

    /**
     * Restituisce l'ultimo prezzo registrato.
     */
    public int getUltimoPrezzo() {
        return trades.isEmpty() ? -1 : trades.getLast().getPrice();
    }

    /**
     * Restituisce l'ID dell'ultimo trade registrato.
     */
    public int getOrderid() {
        return trades.isEmpty() ? -1 : trades.getLast().getOrderId();
    }

    /**
     * Restituisce la cronologia dei prezzi giornaliera per un mese e anno specifici in formato JSON.
     */
    public JsonObject getPriceHistory(int mese, int anno) {
        Map<String, List<Integer>> dailyPrices = new HashMap<>();
    
        // trasformo i trade in un formato più comodo per l'elaborazione
        trades.stream()
            .filter(t -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(t.getTimestamp() * 1000L); // Conversione da secondi a millisecondi
                return cal.get(Calendar.MONTH) + 1 == mese && cal.get(Calendar.YEAR) == anno;
            })
            .forEach(t -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(t.getTimestamp() * 1000L);
                String date = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    
                dailyPrices.computeIfAbsent(date, k -> new ArrayList<>()).add(t.getPrice());
            });
    
        JsonArray dailyData = new JsonArray();
    
        for (Map.Entry<String, List<Integer>> entry : dailyPrices.entrySet()) {
            List<Integer> prices = entry.getValue();
    
            // Se la lista è vuota, non aggiungere questo giorno
            if (prices.isEmpty()) continue;
    
            // Calcola i prezzi minimi e massimi in modo più efficiente
            int minPrice = Collections.min(prices);
            int maxPrice = Collections.max(prices);
            int openPrice = prices.get(0); // Primo prezzo registrato nel giorno
            int closePrice = prices.get(prices.size() - 1); // Ultimo prezzo registrato nel giorno
    
            JsonObject daily = new JsonObject();
            daily.addProperty("date", entry.getKey());
            daily.addProperty("minPrice", minPrice);
            daily.addProperty("maxPrice", maxPrice);
            daily.addProperty("openPrice", openPrice);
            daily.addProperty("closePrice", closePrice);
            dailyData.add(daily);
        }
    
        // Se non ci sono dati per il mese, restituire un JSON con valori a -1
        if (dailyData.size() == 0) {
            JsonObject noData = new JsonObject();
            noData.addProperty("date", "N/A");
            noData.addProperty("minPrice", -1);
            noData.addProperty("maxPrice", -1);
            noData.addProperty("openPrice", -1);
            noData.addProperty("closePrice", -1);
            dailyData.add(noData);
        }
    
        JsonObject result = new JsonObject();
        result.addProperty("month", mese);
        result.addProperty("year", anno);
        result.add("dailyData", dailyData);
    
        return result;
    }
    

    public void close() {
        salvaStoricoOrdini();
        trades.clear();
    }
}
