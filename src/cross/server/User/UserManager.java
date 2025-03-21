package cross.server.User;

import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;


/*
 * Classe per gestire gli user e le loro operazioni
 */
public class UserManager {
    private final ConcurrentHashMap<String, User> users; // Mappa di tutti gli utenti associati al proprio username 
    private final ConcurrentHashMap<String, Boolean> onlineUsers; // Mappa di tutti gli utenti online associati al proprio username
    private final String path = "files/users.json"; // Percorso del file JSON contenente gli utenti
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Gson per la serializzazione/deserializzazione degli oggetti

    public UserManager() {
        users = new ConcurrentHashMap<>();
        this.onlineUsers = new ConcurrentHashMap<>();
        caricaUsers();
    }

    // Carica gli utenti dal file JSON
    public void caricaUsers() {
        File FILE = new File(path);
        if (!FILE.exists()) {
            return;
        }
        try (Reader reader = new FileReader(FILE)) {
            JsonElement jsonElement = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray jsonArray = jsonObject.getAsJsonArray("users");
            jsonArray.forEach(jsonElement1 -> {
                User user = gson.fromJson(jsonElement1, User.class);
                users.put(user.getUsername(), user);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Salva gli utenti nel file JSON
    public void saveUsers() {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        users.values().forEach(user -> {
            jsonArray.add(gson.toJsonTree(user));
        });
        jsonObject.add("users", jsonArray);        
        
        try (Writer writer = new FileWriter(path)) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Registra un nuovo utente
    public int register(String username, String password) {
        if (users.putIfAbsent(username, new User(username, password)) != null) {
            return 101; // Username già esistente
        }
        return password.length() < 8 ? 102 : 100;
    }
    
    // Effettua il login
    public int login(String username, String password) {
        User user = users.get(username);
        if (user == null || !user.checkPassword(password)) {
            return 101;
        }
        return onlineUsers.putIfAbsent(username, true) == null ? 100 : 102;
    }
    
    // Effettua il logout
    public int logout(String username) {
        return onlineUsers.remove(username) != null ? 100 : 101;
    }
    
    // Aggiorna le credenziali di un utente
    public int updateCredentials(String username, String oldPassword, String newPassword) {
        if (onlineUsers.containsKey(username)) {
            return 104;
        }
        if (newPassword.length() < 8) {
            return 101;
        }
        if (oldPassword.equals(newPassword)) {
            return 102;
        }
    
        return users.computeIfPresent(username, (key, user) -> {
            if (user.checkPassword(oldPassword)) {
                user.setPassword(newPassword);
                return user;
            }
            return null; // Rimuove l'utente se la password è errata
        }) != null ? 100 : 103;
    }
    

    // Gunzione di chiusura
    public void close() {
        saveUsers();
        users.clear();  
    }
}
