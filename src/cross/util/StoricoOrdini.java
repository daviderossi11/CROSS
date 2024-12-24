package cross.util;

import cross.order.Order;
import com.google.gson.*;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class StoricoOrdini {
    private final String FILE_PATH = "src/cross/db/storicoOrdini.json";
    private final List<Order> trades;
    private final Gson gson;

    public StoricoOrdini() {
        this.trades = Collections.synchronizedList(new ArrayList<>());
        this.gson = new Gson();
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

    public void addTrade(List<Order> trade) {
        this.trades.addAll(trade);
    }


    public List<Order> getPriceHistory(String MeseAnno) {
        /// MeseAnno deve essere nel formato "MMYYYY"
        int mese = Integer.parseInt(MeseAnno.substring(0, 2));
        int anno = Integer.parseInt(MeseAnno.substring(2, 6));
        synchronized (trades){
            return trades.stream().filter(trade -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(trade.getTimestamp());
                return cal.get(Calendar.MONTH) == mese && cal.get(Calendar.YEAR) == anno;
            }).collect(Collectors.toList());
        }
        
    }


    public synchronized void SalvaOrdini() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = gson.toJsonTree(trades).getAsJsonArray();
            jsonObject.add("trades", jsonArray);
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
