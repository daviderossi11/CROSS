package cross.util;

import cross.order.Order;
import com.google.gson.*;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class StoricoOrdini {
    private final String FILE_PATH = "../../files/storicoOrdini.json";
    private final List<Order> trades;
    private final Gson gson;

    public StoricoOrdini() {
        this.trades = Collections.synchronizedList(new ArrayList<>());
        this.gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        caricaStorico();
    }

    private synchronized  void caricaStorico() {
        try(Reader reader = new FileReader(FILE_PATH)) {
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

    public JsonObject getPriceHistory(String MeseAnno) {
        /// MeseAnno deve essere nel formato "MMYYYY"
        int mese = Integer.parseInt(MeseAnno.substring(0, 2)) - 1; // Calendar.MONTH è zero-based
        int anno = Integer.parseInt(MeseAnno.substring(2, 6));
    
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);
    
        JsonObject result = new JsonObject();
    
        // Verifica se il mese e l'anno sono validi
        if (anno > currentYear || (anno == currentYear && mese > currentMonth)) {
            result.addProperty("minPrice", -1);
            result.addProperty("maxPrice", -1);
            result.addProperty("openPrice", -1);
            result.addProperty("closePrice", -1);
            result.add("trades", new JsonArray());
            return result;
        }
    
        synchronized (trades) {
            List<Order> filteredTrades = trades.stream().filter(trade -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(trade.getTimestamp());
                return cal.get(Calendar.MONTH) == mese && cal.get(Calendar.YEAR) == anno;
            }).collect(Collectors.toList());
    
            if (filteredTrades.isEmpty()) {
                result.addProperty("minPrice", -1);
                result.addProperty("maxPrice", -1);
                result.addProperty("openPrice", -1);
                result.addProperty("closePrice", -1);
                result.add("trades", new JsonArray());
                return result.toString();
            }
    
            int minPrice = filteredTrades.stream().mapToInt(Order::getPrice).min().orElse(-1);
            int maxPrice = filteredTrades.stream().mapToInt(Order::getPrice).max().orElse(-1);
            int openPrice = filteredTrades.get(0).getPrice();
            int closePrice = filteredTrades.get(filteredTrades.size() - 1).getPrice();
    
            result.addProperty("minPrice", minPrice);
            result.addProperty("maxPrice", maxPrice);
            result.addProperty("openPrice", openPrice);
            result.addProperty("closePrice", closePrice);
    
            JsonArray tradesArray = new JsonArray();
            for (Order trade : filteredTrades) {
                JsonObject tradeJson = new JsonObject();
                tradeJson.addProperty("orderId", trade.getOrderId());
                tradeJson.addProperty("type", trade.getType());
                tradeJson.addProperty("price", trade.getPrice());
                tradeJson.addProperty("size", trade.getSize());
                tradeJson.addProperty("timestamp", trade.getTimestamp());
                tradesArray.add(tradeJson);
            }
    
            result.add("trades", tradesArray);
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