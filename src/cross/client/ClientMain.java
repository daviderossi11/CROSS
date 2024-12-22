package cross.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import com.google.gson.*;

public class ClientMain {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Gson gson = new Gson();

        try(Socket socket = new Socket("localhost", 1024)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                System.out.println("Choose action (register/login/logout) or type 'exit' to quit: ");
                String action = scanner.nextLine();

                if (action.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting client...");
                    break;
                }

                JsonObject request = new JsonObject();

                switch (action) {
                    case "register":
                        System.out.println("Enter username: ");
                        String regUsername = scanner.nextLine();
                        System.out.println("Enter password: ");
                        String regPassword = scanner.nextLine();
                        request.addProperty("action", "register");
                        request.addProperty("username", regUsername);
                        request.addProperty("password", regPassword);
                        break;

                    case "login":
                        System.out.println("Enter username: ");
                        String logUsername = scanner.nextLine();
                        System.out.println("Enter password: ");
                        String logPassword = scanner.nextLine();
                        request.addProperty("action", "login");
                        request.addProperty("username", logUsername);
                        request.addProperty("password", logPassword);
                        break;

                    case "logout":
                        System.out.println("Enter user ID: ");
                        int userId = scanner.nextInt();
                        scanner.nextLine(); // consume newline
                        request.addProperty("action", "logout");
                        request.addProperty("userId", userId);
                        break;

                    default:
                        System.out.println("Invalid action");
                        continue;
                }

                out.println(gson.toJson(request));

                String response = in.readLine();
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                System.out.println("Status: " + jsonResponse.get("status").getAsString() + ", Message: " + jsonResponse.get("message").getAsString());   
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
