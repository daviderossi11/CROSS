package cross.client.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;


/*
 * SocketTCP class per la gestione della connessione TCP e invio delle richieste al server
 */
public class SocketTCP implements Runnable {
    private Socket sock; // Socket TCP
    private PrintWriter out; // Output stream per inviare al server
    private static Gson gson = new Gson(); // Gson per la conversione da e verso JSON
    private static volatile boolean running = true; // Flag per terminare il thread
    private static boolean isLogged = false; // Flag per vedere se l'utente è loggato
    private boolean pendingLogin=false; // Flag per vedere se il login è in attesa
    private boolean pendingLogout=false; // Flag per vedere se il logout è in attesa
    private static int UDP_PORT; // Porta UDP
    private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); // Input stream per leggere da tastiera

    public SocketTCP(Socket sock, int PORT) {
        this.sock = sock;
        UDP_PORT = PORT;
        try {
            this.out = new PrintWriter(sock.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Metodo per inviare una richiesta al server
    public void sendRequest(JsonObject request) {
        if (request == null) return;
        if(request.get("operation").getAsString().equals("login")){
            pendingLogin=true;
        }
        if(request.get("operation").getAsString().equals("logout")){
            pendingLogout=true;
        }

        SyncConsole.print("> Invio richiesta: " + request.toString());
        out.println(gson.toJson(request));
    }

    // Metodo per ottenere la socket
    public Socket getSocket() {
        return sock;
    }

    // Metodo per vedere se l'utente è loggato
    public boolean isLogged() {
        return isLogged;
    }

    // Metodo per settare se l'utente è loggato
    public void setLogged(boolean logged) {
        isLogged = logged;
    }


    // Metodo per vedere se il login è in attesa
    public boolean isPendingLogin() {
        return pendingLogin;
    }   

    // Metodo per vedere se il logout è in attesa
    public boolean isPendingLogout() {
        return pendingLogout;
    }

    // Metodo per pulire i flag di login e logout
    public void clearPending() {
        pendingLogin = false;
        pendingLogout = false;
    }

    // Metodo per la gestione in caso l'utente non sia loggato
    private static JsonObject notLoggedError() {
        SyncConsole.print("> Errore: Devi essere loggato per eseguire questa operazione.");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("operation", "error");
        return jsonObject;
    }

    private static JsonObject printError() {
        SyncConsole.print("> Errore: Operazione non valida.");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("operation", "error");
        return jsonObject;
    }

    // Metodo pe rla gestione delle richieste di login
    private static JsonObject getLoginRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci username: ");
        String username = in.readLine();
        SyncConsole.print("> Inserisci password: ");
        String password = in.readLine();
        jsonObject.addProperty("operation", "login");
        JsonObject values = new JsonObject();
        values.addProperty("username", username);
        values.addProperty("password", password);
        jsonObject.add("values", values);
        return jsonObject;
    }

    // Metodo per la gestione delle richieste di registrazione
    private static JsonObject getRegisterRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci username: ");
        String username = in.readLine();
        SyncConsole.print("> Inserisci password: ");
        String password = in.readLine();
        jsonObject.addProperty("operation", "register");
        JsonObject values = new JsonObject();
        values.addProperty("username", username);
        values.addProperty("password", password);
        jsonObject.add("values", values);
        return jsonObject;
    }

    // Metodo per la gestione delle richieste di logout
    private static JsonObject getLogoutRequest() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("operation", "logout");
        jsonObject.add("values", new JsonObject());
        return jsonObject;
    }


    // Metodo per la gestione delle richieste di aggiornamento delle credenziali
    private static JsonObject getUpdateCredentialsRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci vecchia password: ");
        String oldPassword = in.readLine();
        SyncConsole.print("> Inserisci nuova password: ");
        String newPassword = in.readLine();
        
        jsonObject.addProperty("operation", "updateCredentials");
        JsonObject values = new JsonObject();
        values.addProperty("oldPassword", oldPassword);
        values.addProperty("newPassword", newPassword);
        jsonObject.add("values", values);
        return jsonObject;
    }


    // Metodo per la validazione degli ordini
    private static boolean validateOrder(double size, double price, String type) {
        return size > 0 && size <= Integer.MAX_VALUE && price > 0 && price <= Integer.MAX_VALUE && (type.equals("ask") || type.equals("bid"));
    }

    // Metodo per la gestione delle richieste di inserimento di ordini a prezzo limitato
    private static JsonObject getInsertLimitOrderRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci tipo (ask/bid): ");
        String type = in.readLine();
        SyncConsole.print("> Inserisci quantità: ");
        double dsize = Double.parseDouble(in.readLine())*1000;
        SyncConsole.print("> Inserisci prezzo: ");
        double dprice = Double.parseDouble(in.readLine())*1000;
        
        if (!validateOrder(dsize, dprice, type)) {
            return printError();
        }

        int size = (int) dsize;
        int price = (int) dprice;
        
        jsonObject.addProperty("operation", "insertLimitOrder");
        JsonObject values = new JsonObject();
        values.addProperty("type", type);
        values.addProperty("size", size);
        values.addProperty("price", price);
        jsonObject.add("values", values);
        return jsonObject;
    }

    // Metodo per la gestione delle richieste di inserimento di ordini a mercato
    private static JsonObject getInsertMarketOrderRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci tipo (ask/bid): ");
        String type = in.readLine();
        SyncConsole.print("> Inserisci quantità: ");
        double dsize = Double.parseDouble(in.readLine())*1000;
        
        if (!validateOrder(dsize, 1, type)) {
            return printError();
        }

        int size = (int) dsize;
        
        jsonObject.addProperty("operation", "insertMarketOrder");
        JsonObject values = new JsonObject();
        values.addProperty("type", type);
        values.addProperty("size", size);
        jsonObject.add("values", values);
        return jsonObject;
    }

    // Metodo per la gestione delle richieste di inserimento di ordini a prezzo stop
    private static JsonObject getInsertStopOrderRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci tipo (ask/bid): ");
        String type = in.readLine();
        SyncConsole.print("> Inserisci quantità: ");
        double dsize = Double.parseDouble(in.readLine())*1000;
        SyncConsole.print("> Inserisci prezzo: ");
        double dprice = Double.parseDouble(in.readLine())*1000;

        if (!validateOrder(dsize, dprice, type)) {
            return printError();
        }

        int size = (int) dsize;
        int price = (int) dprice;
        
        jsonObject.addProperty("operation", "insertStopOrder");
        JsonObject values = new JsonObject();
        values.addProperty("type", type);
        values.addProperty("size", size);
        values.addProperty("price", price);
        jsonObject.add("values", values);
        return jsonObject;
    }


    // Metodo per la gestione delle richieste di cancellazione di ordini
    private static JsonObject getCancelOrderRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci ID ordine: ");
        int orderId = Integer.parseInt(in.readLine());
        
        if (orderId <= 0) {
            SyncConsole.print("> Errore: ID ordine non valido.");
            return notLoggedError();
        }
        
        jsonObject.addProperty("operation", "cancelOrder");
        JsonObject values = new JsonObject();
        values.addProperty("orderid", orderId);
        jsonObject.add("values", values);
        return jsonObject;
    }


    // Metodo per la gestione delle richieste di ottenimento dello storico dei prezzi
    private static JsonObject getPriceHistoryRequest() throws IOException {
        JsonObject jsonObject = new JsonObject();
        SyncConsole.print("> Inserisci mese e anno (MMYYYY): ");
        String MMYYYY = in.readLine();
        
        jsonObject.addProperty("operation", "getPriceHistory");
        JsonObject values = new JsonObject();
        values.addProperty("month", MMYYYY);
        jsonObject.add("values", values);
        return jsonObject;
    }

    // Metodo per la gestione delle richieste di ottenimento del prezzo corrente
    private static JsonObject getCurrentPriceRequest() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("operation", "getCurrentPrice");
        jsonObject.add("values", new JsonObject());
        return jsonObject;
    }



    // Metodo per la stampa delle operazioni disponibili
    private static void printHelp(){
        SyncConsole.print("> Operazioni disponibili:");
        SyncConsole.print("> login");
        SyncConsole.print("> register");
        SyncConsole.print("> updateCredentials");
        SyncConsole.print("> getCurrentPrice");
        SyncConsole.print("> exit");
        if (isLogged) {
            SyncConsole.print("> logout");
            SyncConsole.print("> insertLimitOrder");
            SyncConsole.print("> insertMarketOrder");
            SyncConsole.print("> insertStopOrder");
            SyncConsole.print("> cancelOrder");
            SyncConsole.print("> getPriceHistory");
        }
    }

    // Metodo per la gestione delle richieste
    private static JsonObject getRequest() throws IOException {
        SyncConsole.print("> Inserisci operazione (help per vedere le operazioni disponibili): ");
        String operation = in.readLine();


        switch (operation) {
            case "login":
                return getLoginRequest();
            case "register":
                return getRegisterRequest();
            case "logout":
                return isLogged ? getLogoutRequest() : notLoggedError();
            case "updateCredentials":
                return getUpdateCredentialsRequest();
            case "getCurrentPrice":
                return getCurrentPriceRequest();
            case "insertLimitOrder":
                return isLogged ? getInsertLimitOrderRequest() : notLoggedError();
            case "insertMarketOrder":
                return isLogged ? getInsertMarketOrderRequest() : notLoggedError();
            case "insertStopOrder":
                return isLogged ? getInsertStopOrderRequest() : notLoggedError();
            case "cancelOrder":
                return isLogged ? getCancelOrderRequest() : notLoggedError();
            case "getPriceHistory":
                return isLogged ? getPriceHistoryRequest() : notLoggedError();
            case "exit":
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("operation", "exit");
                return jsonObject;
            case "help":
                printHelp();
                return getRequest();
            default:
                jsonObject = new JsonObject();
                jsonObject.addProperty("operation", "error");
                return jsonObject;
        }
    }


    // Metodo per chiudere la connessione
    public void shutdown() {
        running = false;
        try {
            if (sock != null && !sock.isClosed()) {
                sock.close();
                SyncConsole.print("> Connessione TCP chiusa.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    @Override
    public void run() {
        out.println(UDP_PORT); //invio la porta UDP al server
        while (running && !sock.isClosed()) {
            try {
                JsonObject request = getRequest();
                if (request.get("operation").getAsString().equals("error") || request == null) {
                    if(request == null){
                        SyncConsole.print("> Errore: Operazione non valida.");
                        continue;
                    }
                    continue;
                }
                if (request.get("operation").getAsString().equals("exit")) {
                    shutdown();
                    System.exit(0);
                    break;
                }
                sendRequest(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
