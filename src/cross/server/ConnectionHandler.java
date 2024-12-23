package cross.server;

import java.io.*;
import java.net.Socket;
import com.google.gson.*;
import cross.order.OrderBook;
import cross.user.User;
import cross.user.UserManagement;
import cross.util.Session;

public class ConnectionHandler implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final UserManagement userManagement;
    private final OrderBook orderBook;
    private User user;
    private Session session;

    public ConnectionHandler(Socket socket, UserManagement userManagement, OrderBook orderBook) {
        this.socket = socket;
        this.userManagement = userManagement;
        this.orderBook = orderBook;
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
            while(socket.isConnected()){
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
                        int responseCode = userManagement.login(username, password);
                        response.addProperty("code", responseCode);
                        switch (responseCode) {
                            case 100 -> {
                                user = userManagement.getUser(username);
                                session = new Session(user.getUserId(), socket.getInetAddress());
                                response.addProperty("message", "Login successful");
                            }
                            case 101 -> response.addProperty("message", "username/password mismatch or user does not exist");
                            case 102 -> response.addProperty("message", "User already logged in");
                            default -> response.addProperty("message", "other error cases");
                        }
                    }
                    case "logout" -> {
                        int logoutResponse = userManagement.logout(user.getUserId());
                        response.addProperty("code", logoutResponse);
                        switch (logoutResponse) {
                            case 100 -> response.addProperty("message", "Logout successful");
                            default -> response.addProperty("message", "username/connection mismatch or not logged in");
                        }
                    }
                    case "register" -> {
                        String username = jsonRequest.get("username").getAsString();
                        String password = jsonRequest.get("password").getAsString();
                        int registerResponse = userManagement.register(username, password);
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
                        int updateResponse = userManagement.updateCredentials(username, oldPassword, newPassword);
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
                    default -> {
                        response.addProperty("code", 400);
                        response.addProperty("message", "Unknown action");
                    }
                }
                out.println(gson.toJson(response));
            }
        } catch(IOException e){
            System.err.println("Error: " + e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error: " + e);
            }
        }
    }
}
