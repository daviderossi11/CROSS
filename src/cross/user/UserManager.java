package cross.user;

import java.io.*;
import java.util.concurrent.*;

import com.google.gson.*;

import cross.util.Session;

public class UserManager {
    private final CopyOnWriteArrayList<User> users;
    private final ConcurrentHashMap<Integer, Session> activeUsers;
    private final String FILE_PATH = "files/Users.json";
    private final Gson gson;

    public UserManager() {
        this.activeUsers = new ConcurrentHashMap<>();
        this.users = new CopyOnWriteArrayList<>();
        this.gson = new Gson();
    }

    public void caricoUtenti() {
        File FILE = new File(FILE_PATH);
        if (!FILE.exists()) {
            return;
        }
        try (Reader reader = new FileReader(FILE)) {
            JsonElement jsonElement = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray jsonArray = jsonObject.getAsJsonArray("users");
            jsonArray.forEach(jsonElement1 -> {
                User user = gson.fromJson(jsonElement1, User.class);
                users.add(user);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void salvaUtenti() {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        users.forEach(user -> {
            jsonArray.add(gson.toJsonTree(user));
        });
        jsonObject.add("users", jsonArray);
        
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUserId(String username) {
        User user = users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
        return user == null ? -1 : user.getUserId();
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void addActiveUser(int id, Session session) {
        activeUsers.put(id, session);
    }

    public void removeActiveUser(int userId) {
        activeUsers.remove(userId);
    }

    public synchronized Session getSession(int userId) {
        return activeUsers.get(userId);
    }

    public synchronized int register(String username, String password) {
        try {
            if (users.stream().anyMatch(user -> user.getUsername().equals(username))) {
                return 101; // Username already exists
            }
            if (password.length() < 8) {
                return 102; // Password too short
            }
            if (username.length() < 4) {
                return 103; // Username too short
            }
            User user = new User(users.size(), username, password);
            users.add(user);
            return 100; // Success
        } catch (Exception e) {
            return 104; // General error
        }
    }

    public synchronized int login(String username, String password) {
        try {
            User user = users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
            if (user == null || !user.checkPassword(password)) {
                return 101; // Invalid username or password
            }
            if (activeUsers.containsKey(user.getUserId())) {
                return 102; // User already logged in
            }
            return 100; // Success
        } catch (Exception e) {
            return 103; // General error
        }
    }

    public synchronized int logout(int userId) {
        try {
            if (!activeUsers.containsKey(userId)) {
                return 101; // User not logged in
            }
            removeActiveUser(userId);
            return 100; // Success
        } catch (Exception e) {
            return 102; // General error
        }
    }

    public synchronized int updateCredentials(String username, String oldPassword, String newPassword) {
        try {
            User user = users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
            if (user == null || !user.checkPassword(oldPassword)) {
                return 102; // Invalid username or password
            }
            if (activeUsers.containsKey(user.getUserId())) {
                return 104; // User is logged in
            }
            if (newPassword.length() < 8) {
                return 101; // New password too short
            }
            if (user.checkPassword(newPassword)) {
                return 103; // New password cannot be the same as the old password
            }
            user.changePassword(newPassword);
            return 100; // Success
        } catch (Exception e) {
            return 105; // General error
        }
    }

    public synchronized void clear() {
        users.clear();
        activeUsers.clear();
    }

}
