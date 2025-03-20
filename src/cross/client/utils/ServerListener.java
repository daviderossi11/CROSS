package cross.client.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/*
 * ServerListener class per la gestione della ricezione dei messaggi dal server
 */

public class ServerListener implements Runnable {
    private SocketTCP socketTCP; // riferimetno alla Socket TCP
    private DatagramSocket udpSocket; // riferimento alla Socket UDP
    private BufferedReader in; // buffer per la lettura dei messaggi TCP dal server
    private static volatile boolean running = true; // flag per terminare il thread
    private static final Gson gson = new Gson(); // Gson per la serializzazione/deserializzazione degli oggetti
    private byte[] buffer = new byte[4096]; // buffer per la lettura dei messaggi UDP

    public ServerListener(SocketTCP socketTCP, DatagramSocket udpSocket) {
        this.socketTCP = socketTCP;
        this.udpSocket = udpSocket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socketTCP.getSocket().getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Metodo per terminare il thread
    public void shutdown() {
        running = false;
        socketTCP.shutdown();
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
            SyncConsole.print("> Connessione UDP chiusa.");
        }
    }

    // Metodo per gestire le risposte dal server
    private synchronized void HandleResponse(JsonObject jsonResponse) { // Gestione delle risposte utente del server
        if (jsonResponse.has("errorMessage")){
             SyncConsole.print("> [TCP] " + jsonResponse.get("errorMessage").getAsString());
            return;
        }
        if (jsonResponse.has("orderId")) { // Ho ricevuto l'ID dell'ordine
            int orderId = jsonResponse.get("orderId").getAsInt();
            if(orderId != -1) SyncConsole.print("> [TCP] Ordine inserito con successo. ID: " + orderId);
            else SyncConsole.print("> [TCP] Errore nell'inserimento dell'ordine.");
            return;
        }
        if (jsonResponse.has("price")){  // Ho ricevuto il prezzo attuale
            SyncConsole.print("> [TCP] Prezzo attuale: " + jsonResponse.get("price").getAsInt());
            return;
        }
        if (jsonResponse.has("dailyData")) { // Ho ricevuto dati giornalieri
            JsonArray dailyData = jsonResponse.get("dailyData").getAsJsonArray();
            JsonElement first = dailyData.get(0);
            String data = first.getAsJsonObject().get("date").getAsString();
        
            if (data.equals("N/A")) {
                SyncConsole.print("> Il mese scelto non è presente nello storico.");
            } else {
                SyncConsole.print("> [TCP] Dati giornalieri ricevuti:");
                for (JsonElement element : dailyData) {
                    JsonObject day = element.getAsJsonObject();
                    SyncConsole.print("  - Data: " + day.get("date").getAsString() +
                        " | Min: " + day.get("minPrice").getAsInt() +
                        " | Max: " + day.get("maxPrice").getAsInt() +
                        " | Open: " + day.get("openPrice").getAsInt() +
                        " | Close: " + day.get("closePrice").getAsInt());
                }
            }
            return;
        }
        if (jsonResponse.has("notification")) { // Ho ricevuto una notifica
            JsonArray closedTrades = jsonResponse.get("trades").getAsJsonArray();
            if (closedTrades.size() == 0) {
                SyncConsole.print("> Nessun trade chiuso.");
            } else {
                SyncConsole.print("> [UDP] Trade effettuato con:");
                for (JsonElement element : closedTrades) {
                    JsonObject trade = element.getAsJsonObject();
                    SyncConsole.print("  - ID: " + trade.get("id").getAsInt() +
                        " | Tipo: " + trade.get("type").getAsString() +
                        " | Tipo ordine: " + trade.get("orderType").getAsString() +
                        " | Prezzo: " + trade.get("price").getAsInt() +
                        " | Quantità: " + trade.get("size").getAsInt() +
                        " | Timestamp: " + trade.get("timestamp").getAsLong());
                }
            }
            return;
        }
        
    }
    

    @Override
    public void run() {
        new Thread(this::listenTCP).start();
        new Thread(this::listenUDP).start();
    }


    // Thread per la ricezione dei messaggi TCP
    private void listenTCP() {
        try {
            while (running) {
                String response = in.readLine();
                if (response == null) {
                    SyncConsole.print("> Connessione TCP chiusa dal server.");
                    shutdown();
                    System.exit(0);
                    break;
                }
    
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
    
                if (jsonResponse.has("response")) {
                    int responseCode = jsonResponse.get("response").getAsInt();
    
                    if (responseCode == 999) {
                        SyncConsole.print("> Il server ha chiuso la connessione. Chiusura del client...");
                        shutdown();
                        System.exit(0);
                        break;
                    } 
                   if(socketTCP.isPendingLogin()){
                        if(responseCode == 100){
                            socketTCP.setLogged(true);
                            socketTCP.clearPending();
                        } else {
                            socketTCP.clearPending();
                        }
                     }  
                    if(socketTCP.isPendingLogout()){
                        if(responseCode == 100){
                            socketTCP.setLogged(false);
                            socketTCP.clearPending();
                        } else {
                            socketTCP.clearPending();
                        }
                   }
                }
                
                HandleResponse(jsonResponse);
            }
        } catch (IOException e) {
            SyncConsole.print("> Errore di connessione TCP.");
        }
    }
    
    // Thread per la ricezione dei messaggi UDP
    private void listenUDP() {
        try {
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                JsonObject jsonResponse = gson.fromJson(message, JsonObject.class);
                HandleResponse(jsonResponse);
            }
        } catch (IOException e) {
            if (running) {
                SyncConsole.print("> Errore di connessione UDP.");
                e.printStackTrace();
            }
        }
    }
}
