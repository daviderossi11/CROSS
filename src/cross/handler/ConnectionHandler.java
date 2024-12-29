package cross.handler;

import java.io.*;
import java.net.Socket;
import com.google.gson.*;
import cross.order.*;
import cross.orderbook.*;
import cross.user.*;
import cross.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionHandler implements Runnable {
    
    private final AtomicInteger orderId;
    private final AtomicInteger currentPrice;
    private int userId;
    private final PriorityBlockingQueue<Order> OrderBookqueue;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final UserManager userManager;
    private final OrderBook orderBook;
    private final CheckStopOrder checkStopOrder;
    private final StoricoOrdini storicoOrdini;

    public ConnectionHandler(Socket socket, UserManager userManager, OrderBook orderBook, CheckStopOrder checkStopOrder, StoricoOrdini storicoOrdini, AtomicInteger currentPrice, AtomicInteger orderId, PriorityBlockingQueue queue) {
        this.socket = socket;
        this.userManager = userManager;
        this.orderBook = orderBook;
        this.checkStopOrder = checkStopOrder;
        this.currentPrice = currentPrice;
        this.orderId = orderId;
        this.storicoOrdini = storicoOrdini;
        this.OrderBookqueue = queue;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(){
        Gson gson = new Gson();
        try {
            while(socket.isConnected() && !Thread.currentThread().isInterrupted()) {
                String request = in.readLine();
                if (request == null) break;

                JsonElement jsonElement = JsonParser.parseString(request);
                JsonObject jsonRequest = jsonElement.getAsJsonObject();
                String action = jsonRequest.get("action").getAsString();
                JsonObject response = new JsonObject();

                switch (action) {
                    case "login" -> {
                        String username = jsonRequest.get("username").getAsString();
                        String password = jsonRequest.get("password").getAsString();
                        int responseCode = userManager.login(username, password);
                        response.addProperty("code", responseCode);
                        switch (responseCode) {
                            case 100 -> {
                                userId = userManager.getUserId(username);
                                Session session = new Session(socket.getInetAddress());
                                userManager.addActiveUser(userId, session);
                                response.addProperty("message", "Login successful");
                            }
                            case 101 -> response.addProperty("message", "username/password mismatch or user does not exist");
                            case 102 -> response.addProperty("message", "User already logged in");
                            default -> response.addProperty("message", "other error cases");
                        }
                    }
                    case "logout" -> {
                        int logoutResponse = userManager.logout(userId);
                        response.addProperty("code", logoutResponse);
                        switch (logoutResponse) {
                            case 100 -> response.addProperty("message", "Logout successful");
                            default -> response.addProperty("message", "username/connection mismatch or not logged in");
                        }
                    }
                    case "register" -> {
                        String username = jsonRequest.get("username").getAsString();
                        String password = jsonRequest.get("password").getAsString();
                        int registerResponse = userManager.register(username, password);
                        response.addProperty("code", registerResponse);
                        switch (registerResponse) {
                            case 100 -> response.addProperty("message", "OK");
                            case 101 -> response.addProperty("message", "Username already exists");
                            case 102 -> response.addProperty("message", "invalid password");
                            default -> response.addProperty("message", "Other error cases");
                        }
                    }
                    case "updateCredentials" -> {
                        String username = jsonRequest.get("username").getAsString();
                        String oldPassword = jsonRequest.get("old_password").getAsString();
                        String newPassword = jsonRequest.get("new_password").getAsString();
                        int updateResponse = userManager.updateCredentials(username, oldPassword, newPassword);
                        response.addProperty("code", updateResponse);
                        switch (updateResponse) {
                            case 100 -> response.addProperty("message", "OK");
                            case 101 -> response.addProperty("message", "invalid new password");
                            case 102 -> response.addProperty("message", "usernmae/old_password mismatch");
                            case 103 -> response.addProperty("message", "new password same as old password");
                            case 104 -> response.addProperty("message", "user currently logged in");
                            default -> response.addProperty("message", "Other error cases");
                        }
                    }
                    case "InsertLimitOrder" -> {
                        int price = jsonRequest.get("price").getAsInt();
                        int size = jsonRequest.get("size").getAsInt();
                        String type = jsonRequest.get("type").getAsString();
                        long timestamp = System.currentTimeMillis();
                        Order order = new LimitOrder(orderId.incrementAndGet(), type, orderType, price, size, timestamp, userId);
                        try {
                            orderBook.addOrder(order);
                            response.addProperty("orderId", order.getOrderId());
                        } catch (Exception e) {
                            response.addProperty("orderId", -1);
                        }
                    }
                    case "InsertMarketOrder" -> {
                        int size = jsonRequest.get("size").getAsInt();
                        String type = jsonRequest.get("type").getAsString();
                        long timestamp = System.currentTimeMillis();
                        Order order = new MarketOrder(orderId.incrementAndGet(), type, orderType, currentPrice.get(), size, timestamp, userId);
                        try {
                            OrderBookqueue.add(order);
                            response.addProperty("orderId", order.getOrderId());
                        } catch (Exception e) {
                            response.addProperty("orderId", -1);
                        }
                    }
                    case "InsertStopOrder" -> {
                        int price = jsonRequest.get("price").getAsInt();
                        int size = jsonRequest.get("size").getAsInt();
                        String type = jsonRequest.get("type").getAsString();
                        long timestamp = System.currentTimeMillis();
                        Order order = new StopOrder(orderId.incrementAndGet(), type, orderType, price, size, timestamp, userId);
                        try {
                            checkStopOrder.addOrder(order);
                            response.addProperty("orderId", order.getOrderId());
                        } catch (Exception e) {
                            response.addProperty("orderId", -1);
                        }
                    }
                    case "cancelOrder" -> {
                        int order_id = jsonRequest.get("orderId").getAsInt();
                        boolean canceled = orderBook.cancelOrder(order_id, userId) || checkStopOrder.cancelOrder(order_id, userId);
                        response.addProperty("code", canceled ? 100 : 101);
                        response.addProperty("message", canceled ? "Order canceled" : "Order not found");
                    }
                    case "getPriceHistory" -> {
                        String MMMYYYY = jsonRequest.get("month").getAsString();
                        JsonObject history = storicoOrdini.getPriceHistory(MMMYYYY);

                        if (history.has("error")) { // Controlla se c'è un errore
                            response.addProperty("error", -1);
                            response.addProperty("message", "Dati non disponibili per il mese selezionato.");
                        } else {
                            response = history; // Invia i dati storici dettagliati
                        }
                    }
                    default -> {
                        response.addProperty("code", 400);
                        response.addProperty("message", "Unknown action");
                    }
                }
                out.println(gson.toJson(response));
            }
        } catch(IOException e){
            e.printStackTrace();
        }finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
