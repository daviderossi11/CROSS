package cross.server.utils;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.net.*;

public class NotificationSender {
    private static final Gson gson = new Gson();


    private static String createNotification(List<Trade> Trades) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("notification", "closedTrades");
        JsonArray jsonArray = new JsonArray();
        Trades.forEach(trade -> {
            jsonArray.add(gson.toJsonTree(trade));
        });
        jsonObject.add("trades", jsonArray);
        return jsonObject.toString();
    }
    public synchronized void sendNotification(List<Trade> Trades, InetAddress IP, int port) {
        String Notification = createNotification(Trades);
        byte[] buffer = Notification.getBytes();
        try (DatagramSocket Socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IP, port);
            Socket.send(packet);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
