package cross.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CROSSClient {
    private final String host;
    private final int port;

    public CROSSClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start(){

        Thread notificationHandler = new Thread(new NotificationHandler(System.out));
        notificationHandler.start();
        
        try(Scanner scanner = new Scanner(System.in)){
            Gson gson = new Gson();
            try(Socket socket = new Socket(host, port)){
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                while(true){
                    System.out.println("Choose action or type 'exit' to quit: ");
                    String action = scanner.nextLine();

                    if(action.equalsIgnoreCase("exit")){
                        System.out.println("Exiting client...");
                        break;
                    }

                    JsonObject request = new JsonObject();

                    switch(action){
                        case "register" -> {
                            System.out.println("Enter username: ");
                            String regUsername = scanner.nextLine();
                            System.out.println("Enter password: ");
                            String regPassword = scanner.nextLine();
                            request.addProperty("action", "register");
                            request.addProperty("username", regUsername);
                            request.addProperty("password", regPassword);
                        }
                        case "login" -> {
                            System.out.println("Enter username: ");
                            String logUsername = scanner.nextLine();
                            System.out.println("Enter password: ");
                            String logPassword = scanner.nextLine();
                            request.addProperty("action", "login");
                            request.addProperty("username", logUsername);
                            request.addProperty("password", logPassword);
                        }
                        case "logout" -> {
                            scanner.nextLine(); // consume newline
                            request.addProperty("action", "logout");
                        }
                        case "updateCredentials" -> {
                            System.out.println("Enter new username: ");
                            String newUsername = scanner.nextLine();
                            System.out.println("Enter new password: ");
                            String newPassword = scanner.nextLine();
                            request.addProperty("action", "updateCredentials");
                            request.addProperty("username", newUsername);
                            request.addProperty("password", newPassword);
                        }
                        default -> {
                            System.out.println("Invalid action. Please try again.");
                            continue;
                        }
                    }

                    out.println(gson.toJson(request));
                    String response = in.readLine();
                    System.out.println("Server response: " + response);
                }
            } catch(IOException e){
                System.err.println("Error: " + e);
            }
        }
    }
    
}
