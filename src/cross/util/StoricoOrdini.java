package cross.util;

import cross.order.Order;
import com.google.gson.*;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class StoricoOrdini {
    private final String FILE_PATH = "files/storicoOrdini.json";
    private final List<Order> trades;
    private final Gson gson;

    public StoricoOrdini() {
        this.trades = Collections.synchronizedList(new ArrayList<>());
        this.gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        caricaStorico();
    }

    private synchronized  void caricaStorico() {

        File FILE = new File(FILE_PATH);

        if (!FILE.exists()) {
            return;
        }

        try(Reader reader = new FileReader(FILE)) {
            JsonElement jsonElement = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray jsonArray = jsonObject.getAsJsonArray("trades");
            jsonArray.forEach(jsonElement1 -> {
                Order trade = gson.fromJson(jsonElement1, Order.class);
                trades.add(trade);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLastPrice() {
        synchronized (trades){
            if (trades.isEmpty()) return 0;
            return trades.get(trades.size() - 1).getPrice();
        }
    }

    public int getOrderid() {
        synchronized (trades){
            if (trades.isEmpty()) return 1;
            return trades.get(trades.size() - 1).getOrderId();
        }
    }

    public synchronized void addTrade(Order trade) {
        trades.add(trade);
    }

    public synchronized JsonObject getPriceHistory(String MeseAnno) {
        int mese = Integer.parseInt(MeseAnno.substring(0, 2)) - 1; // Calendar.MONTH è zero-based
        int anno = Integer.parseInt(MeseAnno.substring(2, 6));
    
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);
    
        JsonObject result = new JsonObject();
    
        // Verifica se il mese e l'anno sono validi
        if (anno > currentYear || (anno == currentYear && mese > currentMonth)) {
            result.addProperty("error", -1);
            return result;
        }
    
        synchronized (trades) {
            // Raggruppa gli ordini per giorno
            Map<String, List<Order>> groupedTrades = trades.stream().filter(trade -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(trade.getTimestamp());
                return cal.get(Calendar.MONTH) == mese && cal.get(Calendar.YEAR) == anno;
            }).collect(Collectors.groupingBy(order -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(order.getTimestamp());
                return String.format("%02d-%02d-%04d", cal.get(Calendar.DAY_OF_MONTH), mese + 1, anno);
            }));
    
            if (groupedTrades.isEmpty()) {
                result.addProperty("error", -1);
                return result;
            }
    
            JsonArray dailyData = new JsonArray();
    
            for (Map.Entry<String, List<Order>> entry : groupedTrades.entrySet()) {
                String date = entry.getKey();
                List<Order> dayTrades = entry.getValue();
    
                int minPrice = dayTrades.stream().mapToInt(Order::getPrice).min().orElse(-1);
                int maxPrice = dayTrades.stream().mapToInt(Order::getPrice).max().orElse(-1);
                int openPrice = dayTrades.get(0).getPrice();
                int closePrice = dayTrades.get(dayTrades.size() - 1).getPrice();
    
                JsonObject daily = new JsonObject();
                daily.addProperty("date", date);
                daily.addProperty("minPrice", minPrice);
                daily.addProperty("maxPrice", maxPrice);
                daily.addProperty("openPrice", openPrice);
                daily.addProperty("closePrice", closePrice);
                dailyData.add(daily);
            }
    
            result.add("data", dailyData);
            return result;
        }
    }
    
    
    


    public synchronized void SalvaOrdini() {

        trades.sort(Comparator.comparing(Order::getOrderId));
        try (Writer writer = new FileWriter(FILE_PATH)) {
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = gson.toJsonTree(trades).getAsJsonArray();
            jsonObject.add("trades", jsonArray);
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public synchronized void clear() {
        trades.clear();
    }
}